package com.example.chronomod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * 시간 정지: 반경 안의 모든 엔티티(몹, 플레이어, 투사체, 아이템 ...)를 5초 동안 멈춘다.
 * 정지 중 받은 피해는 쌓아 두었다가 정지가 풀리는 순간 한꺼번에 터진다.
 */
public final class TimeStop {
    public static final int DURATION = 100;      // 5초
    public static final double RADIUS = 40.0;

    private static final List<Active> ACTIVE = new ArrayList<>();

    private static final class Frozen {
        final Vec3d pos;
        final float yaw, pitch;
        final boolean noAi, noGravity, silent;
        final int tntFuse;
        float pending;
        DamageSource source;

        Frozen(Entity e) {
            pos = e.getPos();
            yaw = e.getYaw();
            pitch = e.getPitch();
            noAi = e instanceof MobEntity m && m.isAiDisabled();
            noGravity = e.hasNoGravity();
            silent = e.isSilent();
            tntFuse = e instanceof TntEntity t ? t.getFuse() : 0;
        }
    }

    private static final class Active {
        final UUID caster;
        final RegistryKey<World> dim;
        final Vec3d center;
        final long endAt;
        final List<ItemDisplayEntity> clockParts;
        final Map<UUID, Frozen> frozen = new HashMap<>();

        Active(UUID caster, RegistryKey<World> dim, Vec3d center, long endAt, List<ItemDisplayEntity> clockParts) {
            this.caster = caster;
            this.dim = dim;
            this.center = center;
            this.endAt = endAt;
            this.clockParts = clockParts;
        }
    }

    private TimeStop() {}

    private static boolean isFrozen(UUID id) {
        for (Active a : ACTIVE) if (a.frozen.containsKey(id)) return true;
        return false;
    }

    public static boolean start(ServerPlayerEntity caster) {
        for (Active a : ACTIVE) if (a.caster.equals(caster.getUuid())) return false;

        ServerWorld world = (ServerWorld) caster.getWorld();
        List<ItemDisplayEntity> clockParts = spawnClock(world, caster);
        Active active = new Active(caster.getUuid(), world.getRegistryKey(), caster.getPos(), TimeManager.now + DURATION, clockParts);

        Box box = caster.getBoundingBox().expand(RADIUS);
        List<Entity> targets = world.getOtherEntities(caster, box, e ->
                e.isAlive()
                        && !e.isSpectator()
                        && !(e instanceof AbstractDecorationEntity)
                        && e != caster.getVehicle()
                        && e.squaredDistanceTo(caster) <= RADIUS * RADIUS
                        && !isFrozen(e.getUuid()));

        for (Entity e : targets) {
            active.frozen.put(e.getUuid(), new Frozen(e));
            if (e instanceof MobEntity m) m.setAiDisabled(true);
            e.setNoGravity(true);
            e.setSilent(true);
            e.setVelocity(Vec3d.ZERO);
        }
        ACTIVE.add(active);

        playStartEffects(world, caster, targets);
        SummonedFx.timeStop(world, caster.getPos());
        return true;
    }

    /** Builds a floating 3D clock from separate item displays so its hands and gears can rotate independently. */
    private static List<ItemDisplayEntity> spawnClock(ServerWorld world, ServerPlayerEntity caster) {
        List<ItemDisplayEntity> parts = new ArrayList<>();
        addClockPart(world, caster, ModItems.TIME_STOP_CLOCK, parts);
        addClockPart(world, caster, ModItems.TIME_STOP_HOUR_HAND, parts);
        addClockPart(world, caster, ModItems.TIME_STOP_MINUTE_HAND, parts);
        addClockPart(world, caster, ModItems.TIME_STOP_GEAR_A, parts);
        addClockPart(world, caster, ModItems.TIME_STOP_GEAR_B, parts);
        return parts;
    }

    private static void addClockPart(ServerWorld world, ServerPlayerEntity caster, net.minecraft.item.Item item,
                                     List<ItemDisplayEntity> parts) {
        ItemDisplayEntity display = EntityType.ITEM_DISPLAY.create(world, SpawnReason.TRIGGERED);
        if (display == null) return;
        display.getStackReference(0).set(new ItemStack(item));
        display.setPosition(caster.getX(), caster.getY() + 2.7, caster.getZ());
        display.setPitch(90.0f);
        display.setYaw(caster.getYaw());
        display.setNoGravity(true);
        display.setSilent(true);
        display.setInvulnerable(true);
        display.setCommandTag("chronomod_time_stop_clock");
        world.spawnEntity(display);
        parts.add(display);
    }

    private static void updateClock(Active active, ServerPlayerEntity caster) {
        float baseYaw = caster.getYaw();
        float t = TimeManager.now;
        float[] offsets = {0.0f, t * 0.18f, t * 3.6f, t * 4.5f, -t * 6.2f};
        double yaw = Math.toRadians(baseYaw);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double[][] localOffsets = {{0, 0}, {0, 0}, {0, 0}, {-2.15, 1.45}, {2.05, -1.55}};

        for (int i = 0; i < active.clockParts.size(); i++) {
            ItemDisplayEntity part = active.clockParts.get(i);
            if (part.isRemoved()) continue;

            double lx = localOffsets[Math.min(i, 4)][0];
            double lz = localOffsets[Math.min(i, 4)][1];
            double worldX = lx * cos + lz * sin;
            double worldZ = -lx * sin + lz * cos;

            part.setPosition(caster.getX() + worldX, caster.getY() + 2.7, caster.getZ() + worldZ);
            part.setPitch(90.0f);
            part.setYaw(baseYaw + offsets[Math.min(i, offsets.length - 1)]);
        }
    }

    private static void playStartEffects(ServerWorld world, ServerPlayerEntity caster, List<Entity> targets) {
        Vec3d c = caster.getPos();
        Vec3d eye = c.add(0, 1.0, 0);
        Vec3d ground = c.add(0, 0.1, 0);

        Fx.sound(world, c, SoundEvents.BLOCK_BELL_RESONATE, 3.0f, 0.5f);
        Fx.sound(world, c, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.7f);
        Fx.sound(world, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0f, 0.5f);

        world.spawnParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        Fx.sphere(world, ParticleTypes.END_ROD, eye, 160, 0.9);
        Fx.sphere(world, ParticleTypes.ELECTRIC_SPARK, eye, 100, 1.4);
        Fx.shockwave(world, ParticleTypes.END_ROD, ground, 24, 12, 1, 90);
        Fx.shockwave(world, ParticleTypes.SOUL_FIRE_FLAME, ground, 14, 8, 2, 60);

        Scheduler.later(6, () -> {
            world.spawnParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
            Fx.sphere(world, ParticleTypes.REVERSE_PORTAL, eye, 120, 0.7);
        });

        int n = 0;
        for (Entity e : targets) {
            if (n++ >= 40) break;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(),
                    12, 0.3, 0.5, 0.3, 0.15);
        }
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Iterator<Active> it = ACTIVE.iterator(); it.hasNext(); ) {
            Active a = it.next();
            ServerWorld world = server.getWorld(a.dim);
            if (world == null) {
                it.remove();
                continue;
            }
            ServerPlayerEntity caster = server.getPlayerManager().getPlayer(a.caster);
            long left = a.endAt - TimeManager.now;

            if (left <= 0 || caster == null || !caster.isAlive() || caster.getWorld() != world) {
                it.remove();            // 먼저 목록에서 빼야 end()의 피해가 다시 쌓이지 않는다
                end(world, a, caster);
                continue;
            }
            hold(world, a);
            updateClock(a, caster);
            visuals(world, a, caster, left);
        }
    }

    /** 매 틱마다 얼려 둔 엔티티를 제자리로 되돌린다 */
    private static void hold(ServerWorld world, Active a) {
        for (Iterator<Map.Entry<UUID, Frozen>> it = a.frozen.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Frozen> en = it.next();
            Entity e = world.getEntity(en.getKey());
            if (e == null || e.isRemoved() || !e.isAlive()) {
                it.remove();
                continue;
            }
            Frozen f = en.getValue();
            e.setVelocity(Vec3d.ZERO);
            e.fallDistance = 0;
            if (e.getPos().squaredDistanceTo(f.pos) > 1.0E-4) {
                if (e instanceof ServerPlayerEntity sp) {
                    sp.networkHandler.requestTeleport(f.pos.x, f.pos.y, f.pos.z, f.yaw, f.pitch);
                } else {
                    e.refreshPositionAndAngles(f.pos.x, f.pos.y, f.pos.z, f.yaw, f.pitch);
                }
            }
            if (e instanceof TntEntity t) t.setFuse(f.tntFuse);
        }
    }

    private static void visuals(ServerWorld world, Active a, ServerPlayerEntity caster, long left) {
        Vec3d c = caster.getPos();
        if (TimeManager.now % 2 == 0) {
            Fx.clockEmblem(world, c.add(0, 0.05, 0), 3.5, TimeManager.now);
            SummonedFx.clockFace(world, c, caster.getYaw(), 3.6, TimeManager.now);
        }
        world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y + 1.0, c.z, 3, 1.5, 1.0, 1.5, 0.02);

        if (TimeManager.now % 3 == 0) {
            int n = 0;
            for (UUID id : a.frozen.keySet()) {
                Entity e = world.getEntity(id);
                if (e == null) continue;
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(),
                        1, e.getWidth() / 2, e.getHeight() / 2, e.getWidth() / 2, 0.0);
                if (++n >= 40) break;
            }
        }

        if (left % 20 == 0) {
            caster.sendMessage(Text.translatable("msg.chronomod.stop_left", left / 20), true);
            if (left <= 60) {
                Fx.sound(world, c, SoundEvents.BLOCK_COMPARATOR_CLICK, 1.0f, 1.6f);
            }
        }
    }

    /** 정지 해제: 풀어 주고, 쌓인 피해를 한꺼번에 터뜨린다 */
    private static void end(ServerWorld world, Active a, ServerPlayerEntity caster) {
        for (ItemDisplayEntity part : a.clockParts) {
            if (!part.isRemoved()) part.discard();
        }
        Vec3d center = caster != null ? caster.getPos() : a.center;
        Vec3d eye = center.add(0, 1.0, 0);

        for (Map.Entry<UUID, Frozen> en : a.frozen.entrySet()) {
            Entity e = world.getEntity(en.getKey());
            if (e == null || e.isRemoved()) continue;
            Frozen f = en.getValue();

            if (e instanceof MobEntity m) m.setAiDisabled(f.noAi);
            e.setNoGravity(f.noGravity);
            e.setSilent(f.silent);

            if (f.pending > 0 && e instanceof LivingEntity le && le.isAlive()) {
                DamageSource src = f.source != null ? f.source : world.getDamageSources().generic();
                le.damage(world, src, f.pending);
                world.spawnParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(),
                        40, 0.4, 0.5, 0.4, 0.6);
                world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getHeight() / 2, e.getZ(),
                        30, 0.4, 0.5, 0.4, 0.5);
            }
        }

        Fx.sound(world, center, SoundEvents.BLOCK_BEACON_DEACTIVATE, 2.0f, 0.6f);
        Fx.sound(world, center, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.2f);
        world.spawnParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.SONIC_BOOM, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        Fx.sphere(world, ParticleTypes.REVERSE_PORTAL, eye, 140, 0.8);
        Fx.shockwave(world, ParticleTypes.ELECTRIC_SPARK, center.add(0, 0.1, 0), 22, 10, 1, 80);
    }

    /** 정지 중 피해 가로채기: 쌓아 두고 취소 */
    public static boolean onDamage(LivingEntity entity, DamageSource source, float amount) {
        if (ACTIVE.isEmpty()) return true;
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true; // /kill, 허공 낙사 등
        for (Active a : ACTIVE) {
            Frozen f = a.frozen.get(entity.getUuid());
            if (f != null) {
                f.pending += amount;
                f.source = source;
                if (entity.getWorld() instanceof ServerWorld sw) {
                    sw.spawnParticles(ParticleTypes.CRIT, entity.getX(), entity.getY() + entity.getHeight() / 2,
                            entity.getZ(), 8, 0.3, 0.4, 0.3, 0.2);
                    sw.spawnParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), entity.getY() + entity.getHeight() / 2,
                            entity.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
                }
                return false;
            }
        }
        return true;
    }

    public static void endAll(MinecraftServer server) {
        List<Active> copy = new ArrayList<>(ACTIVE);
        ACTIVE.clear();
        for (Active a : copy) {
            ServerWorld w = server.getWorld(a.dim);
            if (w != null) end(w, a, server.getPlayerManager().getPlayer(a.caster));
        }
    }
}
