package com.example.chronomod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Minimal, low-clutter 3D clockwork motifs. */
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

    public static void marker(ServerWorld world, Vec3d pos, int lifetime) {
        display(world, ModItems.TIME_STOP_GEAR_A, pos, 0, 90, lifetime);
    }

    /** Four-point compass, not a dense ring of clock numerals. */
    public static void ring(ServerWorld world, Vec3d center, double radius, int count, int lifetime) {
        int n = Math.max(3, Math.min(4, count));
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0 * i / n;
            display(world, i % 2 == 0 ? ModItems.TIME_STOP_GEAR_A : ModItems.TIME_STOP_GEAR_B,
                    center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius),
                    (float) Math.toDegrees(-a), 90, lifetime);
        }
    }

    public static void shockwave(ServerWorld world, Vec3d center, double maxRadius, int steps, int lifetime) {
        // One restrained expanding square/compass pulse rather than several overlapping rings.
        ring(world, center, maxRadius, 4, Math.max(3, lifetime));
    }

    public static void burst(ServerWorld world, Vec3d center, int points, double radius, int lifetime) {
        // Three separated clockwork shards form a quick, readable burst.
        for (int i = 0; i < 3; i++) {
            double a = i * Math.PI * 2.0 / 3.0;
            Vec3d p = center.add(Math.cos(a) * radius, (i - 1) * radius * 0.35,
                    Math.sin(a) * radius);
            display(world, i == 1 ? ModItems.TIME_STOP_HOUR_HAND : ModItems.TIME_STOP_GEAR_A,
                    p, (float) Math.toDegrees(a), 90, lifetime);
        }
    }

    public static void line(ServerWorld world, Vec3d from, Vec3d to, double spacing, int lifetime) {
        Vec3d delta = to.subtract(from);
        if (delta.lengthSquared() < 1.0E-4) return;
        // Only endpoints and midpoint: a clean travel trace.
        for (int i = 0; i < 3; i++) {
            Vec3d p = from.add(delta.multiply(i / 2.0));
            display(world, i == 1 ? ModItems.TIME_STOP_HOUR_HAND : ModItems.TIME_STOP_MINUTE_HAND,
                    p, (float) Math.toDegrees(Math.atan2(delta.z, delta.x)), 90, lifetime);
        }
    }

    public static void helix(ServerWorld world, Vec3d base, double radius, double height,
                             int strands, double phase, int pointsPerStrand, int lifetime) {
        // A single rising spiral, represented by three parts at most.
        int points = 3;
        for (int i = 0; i < points; i++) {
            double t = i / 2.0;
            double a = phase + t * Math.PI * 1.5;
            Vec3d p = base.add(Math.cos(a) * radius, t * height, Math.sin(a) * radius);
            display(world, i == 1 ? ModItems.TIME_STOP_HOUR_HAND : ModItems.TIME_STOP_GEAR_B,
                    p, (float) Math.toDegrees(a), 90, lifetime);
        }
    }

    public static void ghost(ServerWorld world, Vec3d feet) {
        // One small offset hand is enough to suggest a temporal afterimage.
        display(world, ModItems.TIME_STOP_MINUTE_HAND, feet.add(0, 0.9, 0), 35, 90, 5);
    }

    public static void clockEmblem(ServerWorld world, Vec3d center, double radius, double tick) {
        // A single dial and one moving hand; the full time-stop clock is rendered separately.
        display(world, ModItems.TIME_STOP_CLOCK, center, (float) tick, 90, 5);
        display(world, ModItems.TIME_STOP_HOUR_HAND, center.add(0, 0.08, 0),
                (float) (tick * 0.7), 90, 5);
    }

    public static void impact(ServerWorld world, Vec3d center, double strength) {
        display(world, ModItems.TIME_STOP_GEAR_A, center, (float) (strength * 35), 90, 6);
    }
}
