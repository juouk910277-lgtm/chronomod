package com.example.chronomod;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Distinct silhouettes for each ability, using only a handful of 3D parts. */
public final class SummonedFx {
    private SummonedFx() {}

    /** Time stop: the separate floating clock is the main visual; only a brief ground cue is added. */
    public static void timeStop(ServerWorld w, Vec3d c) {
        ModelFx.ring(w, c.add(0, 0.12, 0), 3.4, 4, 8);
    }

    /** Rewind: a sparse reverse spiral, visually distinct from every other ability. */
    public static void rewind(ServerWorld w, Vec3d c) {
        ModelFx.helix(w, c, 1.8, 2.6, 1, Math.PI, 3, 10);
        ModelFx.line(w, c.add(-1.2, 1.3, 0), c.add(1.2, 1.3, 0), 1, 8);
    }

    /** Slow field: four fixed anchors mark the boundary of a calm dome. */
    public static void slowField(ServerWorld w, Vec3d c, double radius) {
        ModelFx.ring(w, c.add(0, 0.15, 0), radius, 4, 12);
    }

    /** Haste: three compact orbiting gears suggest acceleration. */
    public static void haste(ServerWorld w, Vec3d c) {
        ModelFx.helix(w, c.add(0, 0.25, 0), 0.8, 1.7, 1, 0, 3, 8);
    }

    /** Leap: a clean forward arrow from take-off to landing. */
    public static void leap(ServerWorld w, Vec3d c) {
        ModelFx.line(w, c.add(0, 0.15, 0), c.add(0, 1.8, 0), 1, 8);
        ModelFx.ring(w, c.add(0, 0.1, 0), 1.15, 3, 7);
    }

    /** Growth: a rising sprout-like spiral rather than a clock circle. */
    public static void growth(ServerWorld w, Vec3d c) {
        ModelFx.helix(w, c, 1.0, 2.4, 1, -Math.PI / 2, 3, 10);
    }

    /** Kept for older callers, but deliberately minimal. */
    public static void clockFace(ServerWorld w, Vec3d center, float yaw, double radius, long tick) {
        ModelFx.clockEmblem(w, center.add(0, 2.7, 0), radius, tick);
    }
}
