package com.example.chronomod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

/** 파티클 / 사운드 연출 도우미 */
public final class Fx {
    private Fx() {}

    public static void sound(ServerWorld w, Vec3d p, SoundEvent s, float volume, float pitch) {
        w.playSound((PlayerEntity) null, p.x, p.y, p.z, s, SoundCategory.PLAYERS, volume, pitch);
    }

    /** 바닥에 놓인 원. outSpeed > 0 이면 바깥쪽으로 퍼져나감 */
    public static void ring(ServerWorld w, ParticleEffect p, Vec3d c, double r, int points, double outSpeed) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            double dx = Math.cos(a), dz = Math.sin(a);
            if (outSpeed == 0) {
                w.spawnParticles(p, c.x + dx * r, c.y, c.z + dz * r, 1, 0, 0, 0, 0);
            } else {
                w.spawnParticles(p, c.x + dx * r, c.y, c.z + dz * r, 0, dx, 0, dz, outSpeed);
            }
        }
    }

    /** 구 모양으로 사방에 터지는 폭발 */
    public static void sphere(ServerWorld w, ParticleEffect p, Vec3d c, int points, double speed) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < points; i++) {
            double y = 1 - (i / (double) Math.max(1, points - 1)) * 2;
            double r = Math.sqrt(Math.max(0, 1 - y * y));
            double th = golden * i;
            w.spawnParticles(p, c.x, c.y, c.z, 0, Math.cos(th) * r, y, Math.sin(th) * r, speed);
        }
    }

    /** 커져 나가는 충격파 (링을 여러 틱에 걸쳐 확장) */
    public static void shockwave(ServerWorld w, ParticleEffect p, Vec3d c, double maxR, int steps, int ticksPerStep, int maxPoints) {
        for (int s = 1; s <= steps; s++) {
            final double r = maxR * s / steps;
            final int pts = Math.max(16, maxPoints * s / steps);
            Scheduler.later((s - 1) * ticksPerStep, () -> ring(w, p, c, r, pts, 0));
        }
    }

    public static void line(ServerWorld w, ParticleEffect p, Vec3d a, Vec3d b, double spacing) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) return;
        Vec3d n = d.multiply(1.0 / len);
        for (double t = 0; t <= len; t += spacing) {
            Vec3d q = a.add(n.multiply(t));
            w.spawnParticles(p, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** 나선(헬릭스) */
    public static void helix(ServerWorld w, ParticleEffect p, Vec3d base, double radius, double height,
                             int strands, double phase, int pointsPerStrand) {
        for (int s = 0; s < strands; s++) {
            for (int i = 0; i < pointsPerStrand; i++) {
                double t = i / (double) pointsPerStrand;
                double a = phase + s * Math.PI * 2 / strands + t * Math.PI * 2;
                w.spawnParticles(p, base.x + Math.cos(a) * radius, base.y + t * height, base.z + Math.sin(a) * radius,
                        1, 0, 0, 0, 0);
            }
        }
    }

    /** 사람 모양 잔상 */
    public static void ghost(ServerWorld w, Vec3d feet) {
        for (double y = 0; y <= 1.7; y += 0.2) {
            w.spawnParticles(ParticleTypes.END_ROD, feet.x, feet.y + y, feet.z, 2, 0.22, 0.0, 0.14, 0.0);
        }
        w.spawnParticles(ParticleTypes.END_ROD, feet.x, feet.y + 1.8, feet.z, 4, 0.12, 0.1, 0.12, 0.0);
        w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, feet.x, feet.y + 1.0, feet.z, 5, 0.3, 0.7, 0.2, 0.08);
    }

    /** 시계 모양 마법진: 바깥 원 + 12개의 눈금 + 돌아가는 시침/분침 */
    public static void clockEmblem(ServerWorld w, Vec3d c, double r, double t) {
        ring(w, ParticleTypes.END_ROD, c, r, 36, 0);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            double dx = Math.cos(a), dz = Math.sin(a);
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + dx * r * 0.85, c.y, c.z + dz * r * 0.85, 1, 0, 0, 0, 0);
            w.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x + dx * r * 0.93, c.y, c.z + dz * r * 0.93, 1, 0, 0, 0, 0);
        }
        double minute = t * 0.12;
        double hour = t * 0.01;
        line(w, ParticleTypes.ELECTRIC_SPARK, c, c.add(Math.cos(minute) * r * 0.85, 0, Math.sin(minute) * r * 0.85), 0.4);
        line(w, ParticleTypes.END_ROD, c, c.add(Math.cos(hour) * r * 0.55, 0, Math.sin(hour) * r * 0.55), 0.4);
    }
}
