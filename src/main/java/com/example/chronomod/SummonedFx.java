package com.example.chronomod;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Spell visuals made entirely from particles.
 * No block states or BlockDisplay entities are spawned.
 */
public final class SummonedFx {
    private SummonedFx() {}

    private static void dot(ServerWorld w, ParticleEffect p, Vec3d at) {
        w.spawnParticles(p, at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    private static Vec3d plane(Vec3d c, double right, double up, double rx, double rz) {
        return c.add(rx * right, up, rz * right);
    }

    private static void ring(ServerWorld w, ParticleEffect p, Vec3d c, double radius,
                             int count, double rx, double rz, double phase) {
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2.0 * i / count;
            dot(w, p, plane(c, Math.cos(a) * radius, Math.sin(a) * radius, rx, rz));
        }
    }

    private static void line(ServerWorld w, ParticleEffect p, Vec3d c,
                             double x1, double y1, double x2, double y2,
                             double rx, double rz, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            dot(w, p, plane(c, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, rx, rz));
        }
    }

    private static void gear(ServerWorld w, Vec3d c, double x, double y, double r,
                             double rx, double rz, double rotation) {
        ring(w, ParticleTypes.WAX_ON, plane(c, x, y, rx, rz), r, 20, rx, rz, rotation);
        for (int i = 0; i < 8; i++) {
            double a = rotation + Math.PI * 2 * i / 8;
            Vec3d from = plane(c, x + Math.cos(a) * r, y + Math.sin(a) * r, rx, rz);
            Vec3d to = plane(c, x + Math.cos(a) * r * 1.22, y + Math.sin(a) * r * 1.22, rx, rz);
            w.spawnParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 1,
                    to.x - from.x, to.y - from.y, to.z - from.z, 0.01);
        }
    }

    /** Large upright clock sigil: layered rims, twelve hour marks, gears and animated hands. */
    public static void clockFace(ServerWorld w, Vec3d center, float yaw, double radius, long tick) {
        double a = Math.toRadians(yaw);
        double rx = Math.cos(a), rz = Math.sin(a);
        Vec3d face = center.add(0, 2.7, 0);

        ring(w, ParticleTypes.WAX_ON, face, radius, 112, rx, rz, 0);
        ring(w, ParticleTypes.END_ROD, face, radius * 0.94, 96, rx, rz, Math.PI / 96);
        ring(w, ParticleTypes.GLOW, face, radius * 0.78, 88, rx, rz, 0);

        // Hour marks and longer cardinal markers.
        for (int i = 0; i < 12; i++) {
            double h = Math.PI * 2 * i / 12.0;
            double sx = Math.sin(h), sy = Math.cos(h);
            double inner = radius * (i % 3 == 0 ? 0.68 : 0.73);
            double outer = radius * 0.84;
            line(w, ParticleTypes.WAX_ON, face, sx * inner, sy * inner,
                    sx * outer, sy * outer, rx, rz, i % 3 == 0 ? 8 : 4);
        }

        // Roman-numeral-inspired radial strokes around the dial.
        for (int i = 0; i < 12; i++) {
            double h = Math.PI * 2 * i / 12.0;
            double x = Math.sin(h) * radius * 0.58;
            double y = Math.cos(h) * radius * 0.58;
            line(w, ParticleTypes.ELECTRIC_SPARK, face, x - radius * 0.035, y,
                    x + radius * 0.035, y, rx, rz, 3);
        }

        gear(w, face, -radius * 0.30, -radius * 0.12, radius * 0.17, rx, rz, tick * 0.025);
        gear(w, face, radius * 0.29, radius * 0.16, radius * 0.13, rx, rz, -tick * 0.035);

        double minute = (tick % 240L) * Math.PI * 2.0 / 240.0;
        double hx = Math.sin(minute), hy = Math.cos(minute);
        line(w, ParticleTypes.END_ROD, face, 0, 0, hx * radius * 0.60,
                hy * radius * 0.60, rx, rz, 24);
        double hour = minute * 0.12 + Math.PI;
        line(w, ParticleTypes.WAX_ON, face, 0, 0, Math.sin(hour) * radius * 0.38,
                Math.cos(hour) * radius * 0.38, rx, rz, 16);
        ring(w, ParticleTypes.GLOW, face, radius * 0.045, 16, rx, rz, tick * 0.04);
        dot(w, ParticleTypes.FLASH, face);
    }

    public static void timeStop(ServerWorld w, Vec3d c) {
        clockFace(w, c, 0, 3.6, 0);
        ring(w, ParticleTypes.REVERSE_PORTAL, c.add(0, 0.15, 0), 4.2, 64, 1, 0, 0);
        ring(w, ParticleTypes.END_ROD, c.add(0, 2.2, 0), 2.8, 48, 1, 0, Math.PI / 48);
    }

    public static void rewind(ServerWorld w, Vec3d c) {
        for (int i = 0; i < 90; i++) {
            double t = i / 89.0, a = -t * Math.PI * 8;
            dot(w, ParticleTypes.REVERSE_PORTAL,
                    c.add(Math.cos(a) * (0.8 + t * 2.4), 0.3 + t * 4.0, Math.sin(a) * (0.8 + t * 2.4)));
        }
        ring(w, ParticleTypes.WAX_ON, c.add(0, 0.15, 0), 2.8, 64, 1, 0, 0);
    }

    public static void slowField(ServerWorld w, Vec3d c, double radius) {
        ring(w, ParticleTypes.REVERSE_PORTAL, c.add(0, 0.15, 0), radius, 96, 1, 0, 0);
        ring(w, ParticleTypes.END_ROD, c.add(0, 3.0, 0), radius * 0.72, 72, 1, 0, 0.03);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            line(w, ParticleTypes.ELECTRIC_SPARK, c, Math.cos(a) * radius * 0.72, 0,
                    Math.cos(a) * radius * 0.72, 3.0, 1, 0, 10);
        }
    }

    public static void haste(ServerWorld w, Vec3d c) {
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 24; i++) {
                double t = i / 23.0;
                dot(w, ParticleTypes.END_ROD, c.add(side * (0.5 + t * 3.0), 0.4 + t * 1.8, t * 1.5));
            }
        }
        ring(w, ParticleTypes.ELECTRIC_SPARK, c.add(0, 0.2, 0), 2.0, 56, 1, 0, 0);
    }

    public static void leap(ServerWorld w, Vec3d c) {
        for (int i = 0; i <= 32; i++) {
            double t = i / 32.0;
            double y = t * 4.0;
            dot(w, ParticleTypes.REVERSE_PORTAL, c.add(-1.5, y, 0));
            dot(w, ParticleTypes.REVERSE_PORTAL, c.add(1.5, y, 0));
            dot(w, ParticleTypes.END_ROD, c.add(-1.5 + t * 3.0, 0, 0));
            dot(w, ParticleTypes.END_ROD, c.add(-1.5 + t * 3.0, 4.0, 0));
        }
    }

    public static void growth(ServerWorld w, Vec3d c) {
        ring(w, ParticleTypes.HAPPY_VILLAGER, c.add(0, 0.15, 0), 2.0, 48, 1, 0, 0);
        ring(w, ParticleTypes.WAX_ON, c.add(0, 0.2, 0), 4.0, 80, 1, 0, Math.PI / 80);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            line(w, ParticleTypes.HAPPY_VILLAGER, c,
                    Math.cos(a) * 2.8, 0, Math.cos(a) * 2.8, 2.8, 1, 0, 14);
        }
    }
}
