package com.example.chronomod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Animated, multipart 3D clockwork effects shared by the abilities. */
public final class AbilityClockFx {
    private AbilityClockFx() {}

    private static ItemDisplayEntity part(ServerWorld world, Item item, Vec3d pos, float yaw) {
        ItemDisplayEntity e = EntityType.ITEM_DISPLAY.create(world, SpawnReason.TRIGGERED);
        if (e == null) return null;
        e.getStackReference(0).set(new ItemStack(item));
        e.setPosition(pos.x, pos.y, pos.z);
        e.setYaw(yaw);
        e.setPitch(90.0f);
        e.setNoGravity(true);
        e.setSilent(true);
        e.setInvulnerable(true);
        e.addCommandTag("chronomod_ability_clock");
        world.spawnEntity(e);
        return e;
    }

    private static void discard(List<ItemDisplayEntity> parts) {
        for (ItemDisplayEntity e : parts) if (e != null && !e.isRemoved()) e.discard();
    }

    private static void move(ItemDisplayEntity e, Vec3d p, float yaw) {
        if (e == null || e.isRemoved()) return;
        e.setPosition(p.x, p.y, p.z);
        e.setYaw(yaw);
    }


    /** Large segmented hemispherical boundary that follows the time-stop caster. */
    public static void dome(ServerWorld world, ServerPlayerEntity caster, int duration, double radius) {
        List<ItemDisplayEntity> parts = new ArrayList<>();
        List<double[]> offsets = new ArrayList<>();
        int[] counts = {24, 32, 32, 24, 16};
        for (int ring = 0; ring < counts.length; ring++) {
            double theta = (ring + 1) * (Math.PI / 2.0) / (counts.length + 1);
            double r = radius * Math.sin(theta);
            double y = radius * Math.cos(theta);
            for (int i = 0; i < counts[ring]; i++) {
                double a = Math.PI * 2.0 * i / counts[ring];
                offsets.add(new double[]{Math.cos(a) * r, y, Math.sin(a) * r});
                parts.add(part(world, (i + ring) % 2 == 0 ? ModItems.TIME_STOP_GEAR_A : ModItems.TIME_STOP_GEAR_B,
                        caster.getPos(), (float) Math.toDegrees(-a)));
            }
        }
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration || !caster.isAlive() || caster.getWorld() != world) {
                discard(parts);
                return;
            }
            Vec3d base = caster.getPos();
            for (int i = 0; i < parts.size(); i++) {
                double[] o = offsets.get(i);
                move(parts.get(i), base.add(o[0], o[1], o[2]), age[0] * ((i % 2 == 0) ? 0.6f : -0.45f));
            }
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }

    /** Large clock mounted behind the caster. The hands spin up, then stop or run backwards. */
    public static void behind(ServerWorld world, ServerPlayerEntity caster, int duration, boolean reverse, boolean stopAfterWindup) {
        List<ItemDisplayEntity> p = new ArrayList<>();
        p.add(part(world, ModItems.TIME_STOP_CLOCK, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_HOUR_HAND, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_MINUTE_HAND, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_GEAR_A, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_GEAR_B, caster.getPos(), caster.getYaw()));
        final Vec3d[] last = {caster.getPos()};
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration || !caster.isAlive() || caster.getWorld() != world) {
                discard(p);
                return;
            }
            Vec3d forward = caster.getRotationVec(1.0f).multiply(-1.0).normalize();
            Vec3d center = caster.getPos().add(forward.multiply(1.25)).add(0, 2.15, 0);
            float facing = caster.getYaw();
            float spin;
            if (reverse) spin = -age[0] * 15.0f;
            else if (stopAfterWindup && age[0] >= 10) spin = 150.0f;
            else spin = age[0] * 36.0f;
            move(p.get(0), center, facing);
            move(p.get(1), center.add(0, 0.035, 0), facing + spin * 0.20f);
            move(p.get(2), center.add(0, 0.055, 0), facing + spin);
            move(p.get(3), center.add(-0.88, -0.62, 0.015), facing - age[0] * 8.0f);
            move(p.get(4), center.add(0.88, -0.62, 0.015), facing + age[0] * 11.0f);
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }

    /** Small clock above the caster; used for haste and the teleport trigger. */
    public static void overhead(ServerWorld world, ServerPlayerEntity caster, int duration, boolean fast, boolean snap) {
        List<ItemDisplayEntity> p = new ArrayList<>();
        p.add(part(world, ModItems.TIME_STOP_CLOCK, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_MINUTE_HAND, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_GEAR_A, caster.getPos(), caster.getYaw()));
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration || !caster.isAlive() || caster.getWorld() != world) {
                discard(p);
                return;
            }
            Vec3d center = caster.getPos().add(0, 2.45, 0);
            float angle = snap && age[0] < 3 ? 0 : (fast ? age[0] * 34.0f : age[0] * 12.0f);
            move(p.get(0), center, caster.getYaw());
            move(p.get(1), center.add(0, 0.035, 0), caster.getYaw() + angle);
            move(p.get(2), center.add(0.34, 0.02, 0), caster.getYaw() - age[0] * 18.0f);
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }

    /** Large dial projected on the ground; its hand settles at twelve before disappearing. */
    public static void ground(ServerWorld world, Vec3d center, double radius, int duration) {
        List<ItemDisplayEntity> p = new ArrayList<>();
        p.add(part(world, ModItems.TIME_STOP_CLOCK, center.add(0, 0.08, 0), 0));
        p.add(part(world, ModItems.TIME_STOP_HOUR_HAND, center.add(0, 0.12, 0), 0));
        p.add(part(world, ModItems.TIME_STOP_MINUTE_HAND, center.add(0, 0.15, 0), 0));
        int count = 32;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2.0 * i / count;
            Vec3d pos = center.add(Math.cos(a) * radius, 0.10, Math.sin(a) * radius);
            p.add(part(world, i % 2 == 0 ? ModItems.TIME_STOP_GEAR_A : ModItems.TIME_STOP_GEAR_B,
                    pos, (float) Math.toDegrees(-a)));
        }
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration) {
                // A short final beat at twelve o'clock, then remove the complete floor dial.
                discard(p);
                return;
            }
            float progress = Math.min(1.0f, age[0] / (float) Math.max(1, duration - 8));
            float angle = (1.0f - progress) * 720.0f;
            move(p.get(0), center.add(0, 0.08, 0), 0);
            move(p.get(1), center.add(0, 0.12, 0), angle * 0.5f);
            move(p.get(2), center.add(0, 0.15, 0), angle);
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }

    /** Large forward-facing clock for the growth cast. */
    public static void inFront(ServerWorld world, ServerPlayerEntity caster, int duration) {
        List<ItemDisplayEntity> p = new ArrayList<>();
        p.add(part(world, ModItems.TIME_STOP_CLOCK, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_HOUR_HAND, caster.getPos(), caster.getYaw()));
        p.add(part(world, ModItems.TIME_STOP_MINUTE_HAND, caster.getPos(), caster.getYaw()));
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration || !caster.isAlive() || caster.getWorld() != world) {
                discard(p);
                return;
            }
            Vec3d center = caster.getEyePos().add(caster.getRotationVec(1.0f).multiply(1.65));
            float yaw = caster.getYaw();
            move(p.get(0), center, yaw);
            move(p.get(1), center.add(0, 0.035, 0), yaw + age[0] * 13.0f);
            move(p.get(2), center.add(0, 0.055, 0), yaw + age[0] * 28.0f);
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }

    /** A brief red-themed impact cue using the darker gear silhouette above a damaged target. */
    public static void redImpact(ServerWorld world, Vec3d target, int duration, boolean empowered) {
        List<ItemDisplayEntity> p = new ArrayList<>();
        p.add(part(world, ModItems.TIME_STOP_GEAR_B, target.add(0, 0.25, 0), 0));
        p.add(part(world, ModItems.TIME_STOP_HOUR_HAND, target.add(0, 0.29, 0), 0));
        p.add(part(world, ModItems.TIME_STOP_MINUTE_HAND, target.add(0, 0.32, 0), 0));
        final int[] age = {0};
        Runnable[] frame = new Runnable[1];
        frame[0] = () -> {
            if (age[0] >= duration) {
                discard(p);
                return;
            }
            float spin = (empowered ? 48.0f : 24.0f) * age[0];
            Vec3d at = target.add(0, 0.25 + age[0] * 0.012, 0);
            move(p.get(0), at, spin * 0.4f);
            move(p.get(1), at.add(0, 0.035, 0), spin * 0.65f);
            move(p.get(2), at.add(0, 0.055, 0), spin);
            age[0]++;
            Scheduler.later(1, frame[0]);
        };
        frame[0].run();
    }
}
