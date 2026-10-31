package com.example.chronomod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * 시간 되돌리기: 최근 10초 동안의 위치/체력/허기와 주변 몹의 상태를 기록해 두었다가
 * 발동하면 과거로 빠르게 감겨 돌아간다.
 */
public final class Rewind {
    public static final int RECORD_INTERVAL = 5;  // 5틱(0.25초)마다 기록
    public static final int MAX_SNAPS = 40;       // 40개 = 10초
    public static final int ANIM_TICKS = 28;      // 되감기 연출 길이

    public record MobSnap(UUID id, Vec3d pos, float yaw, float health) {}

    public record Snap(RegistryKey<World> dim, Vec3d pos, float yaw, float pitch,
                       float health, int food, List<MobSnap> mobs) {}

    private static final Map<UUID, ArrayDeque<Snap>> HISTORY = new HashMap<>();
    private static final List<Job> JOBS = new ArrayList<>();

    private static final class Job {
        final UUID player;
        final List<Snap> path;   // 최신 -> 과거 순서
        final double step;
        double progress = 0;
        int applied = -1;

        Job(UUID player, List<Snap> path) {
            this.player = player;
            this.path = path;
            this.step = Math.max(1.0, path.size() / (double) ANIM_TICKS);
        }
    }

    private Rewind() {}

    private static boolean isRewinding(UUID id) {
        for (Job j : JOBS) if (j.player.equals(id)) return true;
        return false;
    }

    public static void record(MinecraftServer server) {
        if (TimeManager.now % RECORD_INTERVAL != 0) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            ArrayDeque<Snap> dq = HISTORY.computeIfAbsent(p.getUuid(), k -> new ArrayDeque<>());
            if (!p.isAlive() || p.isSpectator()) {
                dq.clear();
                continue;
            }
            if (isRewinding(p.getUuid())) continue;

            ServerWorld w = (ServerWorld) p.getWorld();
            List<MobSnap> mobs = new ArrayList<>();
            for (Entity e : w.getOtherEntities(p, p.getBoundingBox().expand(16),
                    x -> x instanceof LivingEntity && x.isAlive())) {
                LivingEntity le = (LivingEntity) e;
                mobs.add(new MobSnap(le.getUuid(), le.getPos(), le.getYaw(), le.getHealth()));
            }
            dq.addLast(new Snap(w.getRegistryKey(), p.getPos(), p.getYaw(), p.getPitch(),
                    p.getHealth(), p.getHungerManager().getFoodLevel(), mobs));
            while (dq.size() > MAX_SNAPS) dq.removeFirst();
        }
    }

    public static boolean start(ServerPlayerEntity p) {
        if (isRewinding(p.getUuid())) return false;
        ArrayDeque<Snap> dq = HISTORY.get(p.getUuid());
        ServerWorld w = (ServerWorld) p.getWorld();
        if (dq == null || dq.isEmpty()) {
            p.sendMessage(Text.translatable("msg.chronomod.no_history"), true);
            return false;
        }

        List<Snap> all = new ArrayList<>(dq);
        Collections.reverse(all);
        List<Snap> path = new ArrayList<>();
        for (Snap s : all) {
            if (!s.dim().equals(w.getRegistryKey())) break;   // 차원이 바뀐 지점까지만
            path.add(s);
        }
        if (path.size() < 4) {
            p.sendMessage(Text.translatable("msg.chronomod.no_history"), true);
            return false;
        }
        dq.clear();
        JOBS.add(new Job(p.getUuid(), path));
        SummonedFx.rewind(w, p.getPos());
        AbilityClockFx.behind(w, p, ANIM_TICKS, true, false);

        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ANIM_TICKS + 20, 4, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 50, 0, false, false));

        Vec3d eye = p.getPos().add(0, 1.0, 0);
        Fx.sound(w, p.getPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 2.0f, 0.6f);
        Fx.sound(w, p.getPos(), SoundEvents.BLOCK_BELL_RESONATE, 2.0f, 1.8f);
        ModelFx.line(w, eye.add(-1.2, 0, 0), eye.add(1.2, 0, 0), 1.0, 8);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job job = it.next();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(job.player);
            if (p == null || !p.isAlive()) {
                it.remove();
                continue;
            }
            ServerWorld w = (ServerWorld) p.getWorld();
            if (!w.getRegistryKey().equals(job.path.get(0).dim())) {
                it.remove();
                continue;
            }

            job.progress += job.step;
            int target = Math.min((int) job.progress, job.path.size() - 1);
            for (int i = Math.max(0, job.applied + 1); i <= target; i++) {
                visit(w, job.path.get(i), i);
            }
            job.applied = target;

            Snap s = job.path.get(target);
            p.networkHandler.requestTeleport(s.pos().x, s.pos().y, s.pos().z, s.yaw(), s.pitch());
            p.setVelocity(Vec3d.ZERO);
            p.fallDistance = 0;

            // 거꾸로 도는 나선 + 점점 낮아지는 차임 소리 (되감기 느낌)
            if (TimeManager.now % 4 == 0) {
                ModelFx.helix(w, s.pos(), 0.65, 1.5, 1, -TimeManager.now * 0.45, 3, 5);
            }
            if (TimeManager.now % 2 == 0) {
                float frac = target / (float) Math.max(1, job.path.size() - 1);
                Fx.sound(w, s.pos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 2.0f - 1.5f * frac);
            }

            if (target >= job.path.size() - 1) {
                it.remove();
                finish(w, p, job);
            }
        }
    }

    private static void visit(ServerWorld w, Snap s, int index) {
        if (index % 2 == 0) Fx.ghost(w, s.pos());
        // Nearby players and mobs are rewound along with the caster, using the
        // same historical snapshots recorded for this caster.
        for (MobSnap ms : s.mobs()) {
            Entity e = w.getEntity(ms.id());
            if (e instanceof LivingEntity le && le.isAlive()) {
                le.refreshPositionAndAngles(ms.pos().x, ms.pos().y, ms.pos().z, ms.yaw(), le.getPitch());
                le.setVelocity(Vec3d.ZERO);
                le.fallDistance = 0;
            }
        }
    }

    private static void finish(ServerWorld w, ServerPlayerEntity p, Job job) {
        Snap last = job.path.get(job.path.size() - 1);

        // 체력/허기는 "더 좋은 쪽"을 유지 (되돌리다가 오히려 손해 보지 않도록)
        p.setHealth(Math.max(p.getHealth(), Math.min(last.health(), p.getMaxHealth())));
        p.getHungerManager().setFoodLevel(Math.max(p.getHungerManager().getFoodLevel(), last.food()));
        p.extinguish();
        p.fallDistance = 0;

        // 주변 플레이어와 몹도 과거 위치/체력으로 되돌린다
        for (MobSnap ms : last.mobs()) {
            Entity e = w.getEntity(ms.id());
            if (e instanceof LivingEntity le && le.isAlive()) {
                Vec3d now = le.getPos();
                Fx.line(w, ParticleTypes.REVERSE_PORTAL, now.add(0, 1, 0), ms.pos().add(0, 1, 0), 0.5);
                le.refreshPositionAndAngles(ms.pos().x, ms.pos().y, ms.pos().z, ms.yaw(), le.getPitch());
                le.setVelocity(Vec3d.ZERO);
                le.fallDistance = 0;
                le.setHealth(Math.min(ms.health(), le.getMaxHealth()));
                ModelFx.impact(w, ms.pos().add(0, 1, 0), 0.7);
            }
        }

        Vec3d eye = p.getPos().add(0, 1.0, 0);
        ModelFx.burst(w, eye, 3, 0.55, 7);
        Fx.sound(w, p.getPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
        Fx.sound(w, p.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.2f);
    }

    public static void onDisconnect(UUID id) {
        HISTORY.remove(id);
        JOBS.removeIf(j -> j.player.equals(id));
    }

    public static void clear() {
        HISTORY.clear();
        JOBS.clear();
    }
}
