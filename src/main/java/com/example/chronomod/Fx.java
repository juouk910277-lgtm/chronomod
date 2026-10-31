package com.example.chronomod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

/** Shared sound and 3D model effects. ParticleEffect parameters remain for source compatibility only. */
public final class Fx {
    private Fx() {}

    public static void sound(ServerWorld w, Vec3d p, SoundEvent s, float volume, float pitch) {
        w.playSound((PlayerEntity) null, p.x, p.y, p.z, s, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void ring(ServerWorld w, ParticleEffect ignored, Vec3d c, double r, int points, double outSpeed) {
        ModelFx.ring(w, c, r, points, 8);
    }

    public static void sphere(ServerWorld w, ParticleEffect ignored, Vec3d c, int points, double speed) {
        ModelFx.burst(w, c, points, Math.max(0.5, speed * 2.0), 8);
    }

    public static void shockwave(ServerWorld w, ParticleEffect ignored, Vec3d c, double maxR,
                                 int steps, int ticksPerStep, int maxPoints) {
        ModelFx.shockwave(w, c, maxR, steps, Math.max(4, steps * ticksPerStep + 4));
    }

    public static void line(ServerWorld w, ParticleEffect ignored, Vec3d a, Vec3d b, double spacing) {
        ModelFx.line(w, a, b, spacing, 8);
    }

    public static void helix(ServerWorld w, ParticleEffect ignored, Vec3d base, double radius, double height,
                             int strands, double phase, int pointsPerStrand) {
        ModelFx.helix(w, base, radius, height, strands, phase, pointsPerStrand, 8);
    }

    public static void ghost(ServerWorld w, Vec3d feet) {
        ModelFx.ghost(w, feet);
    }

    public static void clockEmblem(ServerWorld w, Vec3d c, double r, double t) {
        ModelFx.clockEmblem(w, c, r, t);
    }
}
