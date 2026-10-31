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
        AbilityClockFx.overhead(w, p, DURATION, true, false);

        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, DURATION, 4, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, DURATION, 3, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, DURATION, 1, false, true));

        Vec3d eye = p.getPos().add(0, 1.0, 0);
        Fx.sound(w, p.getPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.5f, 0.6f);
        Fx.sound(w, p.getPos(), SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5f, 1.6f);
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
                ModelFx.helix(w, pos, 0.8, 1.4, 1, TimeManager.now * 0.5, 3, 6);
                continue;
            }

            if (TimeManager.now % 5 == 0) {
                ModelFx.helix(w, pos, 0.8, 1.5, 1, TimeManager.now * 0.5, 3, 6);
            }

            double moved = pos.distanceTo(s.last);
            if (moved > 0.25 && moved < 20) {
                if (TimeManager.now % 2 == 0) Fx.ghost(w, s.last);   // 3D 잔상
            }
            s.last = pos;
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }
}
