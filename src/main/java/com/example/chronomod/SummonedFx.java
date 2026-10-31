package com.example.chronomod;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Ability-specific spell visuals composed of short-lived 3D item-display models. */
public final class SummonedFx {
    private SummonedFx() {}

    public static void clockFace(ServerWorld w, Vec3d center, float yaw, double radius, long tick) {
        Vec3d face = center.add(0, 2.7, 0);
        ModelFx.ring(w, face, radius, 24, 10);
        ModelFx.ring(w, face, radius * 0.72, 16, 10);
        ModelFx.helix(w, face, radius * 0.28, 0.25, 2, tick * 0.04, 6, 10);
        ModelFx.line(w, face, face.add(Math.sin(tick * 0.025) * radius * 0.62, 0,
                Math.cos(tick * 0.025) * radius * 0.62), 0.55, 10);
        ModelFx.line(w, face, face.add(Math.sin(tick * 0.006 + Math.PI) * radius * 0.38, 0,
                Math.cos(tick * 0.006 + Math.PI) * radius * 0.38), 0.65, 10);
    }

    public static void timeStop(ServerWorld w, Vec3d c) {
        ModelFx.ring(w, c.add(0, 0.12, 0), 4.2, 24, 10);
        ModelFx.ring(w, c.add(0, 2.2, 0), 2.8, 16, 10);
        clockFace(w, c, 0, 3.6, 0);
    }

    public static void rewind(ServerWorld w, Vec3d c) {
        ModelFx.helix(w, c, 2.8, 4.0, 3, 0, 8, 12);
        ModelFx.ring(w, c.add(0, 0.15, 0), 2.8, 20, 12);
    }

    public static void slowField(ServerWorld w, Vec3d c, double radius) {
        ModelFx.ring(w, c.add(0, 0.15, 0), radius, 24, 12);
        ModelFx.ring(w, c.add(0, 3.0, 0), radius * 0.72, 20, 12);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            Vec3d from = c.add(Math.cos(a) * radius * 0.72, 0, Math.sin(a) * radius * 0.72);
            Vec3d to = from.add(0, 3.0, 0);
            ModelFx.line(w, from, to, 0.7, 12);
        }
    }

    public static void haste(ServerWorld w, Vec3d c) {
        ModelFx.helix(w, c, 1.25, 2.2, 2, 0, 8, 8);
        ModelFx.ring(w, c.add(0, 0.2, 0), 2.0, 16, 8);
    }

    public static void leap(ServerWorld w, Vec3d c) {
        ModelFx.ring(w, c.add(0, 0.15, 0), 1.5, 12, 8);
        ModelFx.helix(w, c, 1.5, 4.0, 2, 0, 8, 8);
        ModelFx.ring(w, c.add(0, 4.0, 0), 1.5, 12, 8);
    }

    public static void growth(ServerWorld w, Vec3d c) {
        ModelFx.ring(w, c.add(0, 0.15, 0), 2.0, 16, 10);
        ModelFx.ring(w, c.add(0, 0.2, 0), 4.0, 24, 10);
        ModelFx.helix(w, c, 2.8, 2.8, 2, 0, 8, 10);
    }
}
