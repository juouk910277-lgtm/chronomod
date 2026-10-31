package com.example.chronomod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** 시간 감속장: 반구형 돔 안의 적과 투사체를 느리게 만든다. 시전자는 오히려 빨라진다. */
public final class SlowField {
    public static final int DURATION = 240;   // 12초
    public static final double RADIUS = 9.0;

    private record Field(UUID owner, RegistryKey<World> dim, Vec3d center, long endAt) {}

    private static final List<Field> FIELDS = new ArrayList<>();

    private SlowField() {}

    public static boolean start(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        Vec3d c = p.getPos();
        FIELDS.add(new Field(p.getUuid(), w.getRegistryKey(), c, TimeManager.now + DURATION));
        SummonedFx.slowField(w, c, RADIUS);

        Fx.sound(w, c, SoundEvents.BLOCK_BEACON_ACTIVATE, 2.0f, 1.4f);
        Fx.sound(w, c, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.5f, 0.5f);

        return true;
    }

    public static void tick(MinecraftServer server) {
        if (FIELDS.isEmpty()) return;
        for (Iterator<Field> it = FIELDS.iterator(); it.hasNext(); ) {
            Field f = it.next();
            ServerWorld w = server.getWorld(f.dim());
            if (w == null) {
                it.remove();
                continue;
            }
            if (TimeManager.now >= f.endAt()) {
                it.remove();
                Fx.sound(w, f.center(), SoundEvents.BLOCK_BEACON_DEACTIVATE, 2.0f, 1.2f);
                ModelFx.ring(w, f.center().add(0, 0.15, 0), RADIUS, 4, 8);
                continue;
            }
            applyField(w, f);
            visuals(w, f);
        }
    }

    private static void applyField(ServerWorld w, Field f) {
        Box box = new Box(f.center(), f.center()).expand(RADIUS);
        boolean refresh = TimeManager.now % 5 == 0;

        for (Entity e : w.getOtherEntities(null, box, x -> true)) {
            if (e.getPos().squaredDistanceTo(f.center()) > RADIUS * RADIUS) continue;

            if (e.getUuid().equals(f.owner())) {
                if (refresh && e instanceof LivingEntity le) {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20, 1, false, false));
                }
                continue;
            }

            if (e instanceof LivingEntity le) {
                if (refresh) {
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 20, 3, false, false));
                    le.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 20, 2, false, false));
                    // The field boundary, not individual entity markers, communicates the slow effect.
                }
            } else if (e instanceof ProjectileEntity) {
                e.setVelocity(e.getVelocity().multiply(0.55));
            }
        }
    }

    private static void visuals(ServerWorld w, Field f) {
        // Four stationary anchors from SummonedFx define the field boundary.
    }

    public static void clear() {
        FIELDS.clear();
    }
}
