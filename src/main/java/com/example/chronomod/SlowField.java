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
        ModelFx.impact(w, c.add(0, 1, 0), 0.8);
        Fx.shockwave(w, ParticleTypes.SOUL_FIRE_FLAME, c.add(0, 0.1, 0), RADIUS, 10, 1, 80);
        Fx.shockwave(w, ParticleTypes.END_ROD, c.add(0, 0.1, 0), RADIUS * 0.6, 6, 2, 50);
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
                Fx.sphere(w, ParticleTypes.END_ROD, f.center().add(0, 1, 0), 80, 0.7);
                Fx.shockwave(w, ParticleTypes.ELECTRIC_SPARK, f.center().add(0, 0.1, 0), RADIUS + 3, 6, 1, 80);
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
                    ModelFx.marker(w, e.getPos().add(0, e.getHeight() / 2, 0), 5);
                }
            } else if (e instanceof ProjectileEntity) {
                e.setVelocity(e.getVelocity().multiply(0.55));
            }
        }
    }

    private static void visuals(ServerWorld w, Field f) {
        Vec3d c = f.center();
        for (int i = 0; i < 10; i++) {
            double u = w.random.nextDouble() * Math.PI * 2;
            double v = Math.acos(w.random.nextDouble());   // 반구 위의 균일 분포
            double x = Math.sin(v) * Math.cos(u), y = Math.cos(v), z = Math.sin(v) * Math.sin(u);
            ModelFx.marker(w, c.add(x * RADIUS, y * RADIUS, z * RADIUS), 5);
        }
        if (TimeManager.now % 4 == 0) {
            Fx.ring(w, ParticleTypes.SOUL_FIRE_FLAME, c.add(0, 0.1, 0), RADIUS, 48, 0);
        }
        if (TimeManager.now % 2 == 0) {
            // 바닥 시계 마법진: 일반보다 훨씬 느리게 돈다
            Fx.clockEmblem(w, c.add(0, 0.05, 0), RADIUS * 0.6, TimeManager.now / 4.0);
        }
    }

    public static void clear() {
        FIELDS.clear();
    }
}
