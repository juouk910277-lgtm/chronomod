package com.example.chronomod;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** 시간 가속: 10초 동안 자신의 시간만 빨라진다. 이동 경로에 잔상이 남는다. */
public final class Haste {
    public static final int DURATION = 200;

    private static final class State {
        final long endAt;
        Vec3d last;

        State(long endAt, Vec3d last) {
            this.endAt = endAt;
            this.last = last;
        }
    }

    private static final Map<UUID, State> ACTIVE = new HashMap<>();

    private Haste() {}

    public static boolean start(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        ACTIVE.put(p.getUuid(), new State(TimeManager.now + DURATION, p.getPos()));
        SummonedFx.haste(w, p.getPos());

        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION, 4, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, DURATION, 3, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, DURATION, 1, false, true));

        Vec3d eye = p.getPos().add(0, 1.0, 0);
        Fx.sound(w, p.getPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f);
        Fx.sound(w, p.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.6f);
        w.spawnParticles(ParticleTypes.FLASH, eye.x, eye.y, eye.z, 1, 0, 0, 0, 0);
        Fx.sphere(w, ParticleTypes.ELECTRIC_SPARK, eye, 90, 1.0);
        Fx.shockwave(w, ParticleTypes.END_ROD, p.getPos().add(0, 0.1, 0), 6, 5, 1, 50);
        return true;
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Iterator<Map.Entry<UUID, State>> it = ACTIVE.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, State> en = it.next();
            State s = en.getValue();
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
            if (p == null || !p.isAlive()) {
                it.remove();
                continue;
            }
            ServerWorld w = (ServerWorld) p.getWorld();
            Vec3d pos = p.getPos();

            if (TimeManager.now >= s.endAt) {
                it.remove();
                Fx.sound(w, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.8f);
                Fx.sphere(w, ParticleTypes.END_ROD, pos.add(0, 1, 0), 40, 0.4);
                continue;
            }

            Fx.helix(w, ParticleTypes.ELECTRIC_SPARK, pos, 0.9, 2.0, 2, TimeManager.now * 0.7, 8);

            double moved = pos.distanceTo(s.last);
            if (moved > 0.25 && moved < 20) {
                w.spawnParticles(ParticleTypes.END_ROD, s.last.x, s.last.y + 1.0, s.last.z, 6, 0.25, 0.6, 0.25, 0.01);
                w.spawnParticles(ParticleTypes.CLOUD, s.last.x, s.last.y + 0.1, s.last.z, 2, 0.2, 0.05, 0.2, 0.02);
                if (TimeManager.now % 2 == 0) Fx.ghost(w, s.last);   // 잔상
            }
            s.last = pos;
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
