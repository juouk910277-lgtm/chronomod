package com.example.chronomod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Short-lived 3D display-entity effects. These helpers intentionally spawn no particles. */
public final class ModelFx {
    private ModelFx() {}

    private static void display(ServerWorld world, Item item, Vec3d pos, float yaw, float pitch, int lifetime) {
        ItemDisplayEntity entity = EntityType.ITEM_DISPLAY.create(world, SpawnReason.TRIGGERED);
        if (entity == null) return;
        entity.getStackReference(0).set(new ItemStack(item));
        entity.setPosition(pos.x, pos.y, pos.z);
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.setNoGravity(true);
        entity.setSilent(true);
        entity.setInvulnerable(true);
        entity.addCommandTag("chronomod_temporary_fx");
        world.spawnEntity(entity);
        Scheduler.later(lifetime, () -> {
            if (!entity.isRemoved()) entity.discard();
        });
    }

    private static Item gear(int index) {
        return index % 2 == 0 ? ModItems.TIME_STOP_GEAR_A : ModItems.TIME_STOP_GEAR_B;
    }

    public static void marker(ServerWorld world, Vec3d pos, int lifetime) {
        display(world, ModItems.TIME_STOP_GEAR_A, pos, 0, 90, lifetime);
    }

    /** Small 3D gear markers arranged as a horizontal ring. */
    public static void ring(ServerWorld world, Vec3d center, double radius, int count, int lifetime) {
        int n = Math.max(8, Math.min(32, count));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0 * i / n;
            Vec3d p = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            display(world, gear(i), p, (float) Math.toDegrees(-a), 90, lifetime);
        }
    }

    public static void shockwave(ServerWorld world, Vec3d center, double maxRadius, int steps, int lifetime) {
        int count = Math.max(1, Math.min(8, steps));
        for (int i = 1; i <= count; i++) {
            final double radius = maxRadius * i / count;
            final int delay = (i - 1) * Math.max(1, lifetime / count);
            Scheduler.later(delay, () -> ring(world, center, radius, 20, Math.max(2, lifetime - delay + 2)));
        }
    }

    /** Compact 3D burst; capped to keep entity count reasonable and avoid blocking the view. */
    public static void burst(ServerWorld world, Vec3d center, int points, double radius, int lifetime) {
        int n = Math.max(8, Math.min(24, points));
        for (int i = 0; i < n; i++) {
            double y = 1.0 - 2.0 * (i + 0.5) / n;
            double horizontal = Math.sqrt(Math.max(0, 1.0 - y * y));
            double a = i * 2.399963229728653;
            Vec3d p = center.add(Math.cos(a) * horizontal * radius, y * radius,
                    Math.sin(a) * horizontal * radius);
            display(world, gear(i), p, (float) Math.toDegrees(a), 90, lifetime);
        }
    }

    public static void line(ServerWorld world, Vec3d from, Vec3d to, double spacing, int lifetime) {
        Vec3d delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) return;
        int count = Math.max(2, Math.min(20, (int) Math.ceil(length / Math.max(0.25, spacing))));
        for (int i = 0; i <= count; i++) {
            Vec3d p = from.add(delta.multiply(i / (double) count));
            display(world, i % 2 == 0 ? ModItems.TIME_STOP_HOUR_HAND : ModItems.TIME_STOP_MINUTE_HAND,
                    p, (float) Math.toDegrees(Math.atan2(delta.z, delta.x)), 90, lifetime);
        }
    }

    public static void helix(ServerWorld world, Vec3d base, double radius, double height,
                             int strands, double phase, int pointsPerStrand, int lifetime) {
        int sCount = Math.max(1, Math.min(3, strands));
        int points = Math.max(4, Math.min(10, pointsPerStrand));
        for (int s = 0; s < sCount; s++) {
            for (int i = 0; i < points; i++) {
                double t = i / (double) Math.max(1, points - 1);
                double a = phase + s * Math.PI * 2 / sCount + t * Math.PI * 2;
                Vec3d p = base.add(Math.cos(a) * radius, t * height, Math.sin(a) * radius);
                display(world, gear(i + s), p, (float) Math.toDegrees(a), 90, lifetime);
            }
        }
    }

    public static void ghost(ServerWorld world, Vec3d feet) {
        // A subtle vertical stack of small 3D clock components instead of a particle silhouette.
        for (int i = 0; i < 4; i++) {
            display(world, i % 2 == 0 ? ModItems.TIME_STOP_HOUR_HAND : ModItems.TIME_STOP_MINUTE_HAND,
                    feet.add(0, 0.35 + i * 0.38, 0), i * 22.5f, 90, 8);
        }
    }

    public static void clockEmblem(ServerWorld world, Vec3d center, double radius, double tick) {
        ring(world, center, radius, 24, 6);
        display(world, ModItems.TIME_STOP_CLOCK, center.add(0, 0.08, 0), (float) tick, 90, 6);
        display(world, ModItems.TIME_STOP_HOUR_HAND, center.add(0, 0.12, 0), (float) (tick * 0.7), 90, 6);
        display(world, ModItems.TIME_STOP_MINUTE_HAND, center.add(0, 0.14, 0), (float) (tick * 3.0), 90, 6);
    }

    public static void impact(ServerWorld world, Vec3d center, double strength) {
        double spread = Math.max(0.25, Math.min(1.25, strength));
        display(world, ModItems.TIME_STOP_GEAR_A, center.add(-spread, 0, 0), 0, 90, 8);
        display(world, ModItems.TIME_STOP_GEAR_B, center.add(spread, 0, 0), 35, 90, 8);
        display(world, ModItems.TIME_STOP_HOUR_HAND, center.add(0, 0.12, 0), 90, 90, 8);
    }

    /** Compatibility bridge for call sites being migrated away from particle visuals. */
    public static void burst(ServerWorld world, Vec3d center, ParticleEffect ignored, int points, double radius, int lifetime) {
        burst(world, center, points, radius, lifetime);
    }
}
