package com.example.chronomod;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity.BlockDisplayEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Temporary 3D spell structures built from Block Display entities.
 * These are actual summoned world models, not particle-only effects.
 */
public final class SummonedFx {
    private SummonedFx() {}

    private static BlockDisplayEntity block(ServerWorld world, Vec3d pos, BlockState state, int life) {
        BlockDisplayEntity display = new BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
        display.setBlockState(state);
        display.setPosition(pos);
        display.setNoGravity(true);
        display.setInvulnerable(true);
        display.setSilent(true);
        world.spawnEntity(display);
        Scheduler.later(life, display::discard);
        return display;
    }

    private static void ring(ServerWorld w, Vec3d c, double radius, int count, double y,
                             BlockState state, int life, double phase) {
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2.0 * i / count;
            block(w, c.add(Math.cos(a) * radius, y, Math.sin(a) * radius), state, life);
        }
    }

    private static void pillar(ServerWorld w, Vec3d c, double x, double z, int height,
                               BlockState state, int life) {
        for (int y = 0; y < height; y++) {
            block(w, c.add(x, y, z), state, life);
        }
    }

    /** Large upright summoned clock face with hour marks, gears, and moving hands. */
    public static void clockFace(ServerWorld w, Vec3d center, float yaw, double radius, long tick) {
        double angle = Math.toRadians(yaw);
        double rx = Math.cos(angle), rz = Math.sin(angle);
        Vec3d face = center.add(0, 2.6, 0);

        // Outer and inner rims in a vertical plane.
        for (int i = 0; i < 48; i++) {
            double a = Math.PI * 2.0 * i / 48.0;
            double x = Math.cos(a) * radius;
            double y = Math.sin(a) * radius;
            block(w, face.add(rx * x, y, rz * x), Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 8);
            if (i % 2 == 0) {
                block(w, face.add(rx * x * 0.88, y * 0.88, rz * x * 0.88),
                        Blocks.CUT_COPPER.getDefaultState(), 8);
            }
        }

        // Twelve prominent hour indices.
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0;
            double x = Math.sin(a) * radius * 0.76;
            double y = Math.cos(a) * radius * 0.76;
            block(w, face.add(rx * x, y, rz * x), Blocks.GILDED_BLACKSTONE.getDefaultState(), 8);
        }

        // Two gear-like cogs on the dial.
        for (int g = 0; g < 2; g++) {
            double gx = (g == 0 ? -0.42 : 0.42) * radius;
            double gy = (g == 0 ? -0.12 : 0.18) * radius;
            double gr = radius * 0.19;
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2.0 * i / 12.0;
                block(w, face.add(rx * (gx + Math.cos(a) * gr),
                        gy + Math.sin(a) * gr, rz * (gx + Math.cos(a) * gr)),
                        Blocks.CUT_COPPER.getDefaultState(), 8);
            }
            block(w, face.add(rx * gx, gy, rz * gx), Blocks.GOLD_BLOCK.getDefaultState(), 8);
        }

        // Sweeping minute and hour hands.
        double handAngle = (tick % 240L) * Math.PI * 2.0 / 240.0;
        for (int i = 1; i <= 10; i++) {
            double d = radius * 0.065 * i;
            block(w, face.add(rx * Math.sin(handAngle) * d,
                    Math.cos(handAngle) * d, rz * Math.sin(handAngle) * d),
                    Blocks.GOLD_BLOCK.getDefaultState(), 4);
        }
        double hourAngle = handAngle * 0.12 + Math.PI;
        for (int i = 1; i <= 6; i++) {
            double d = radius * 0.075 * i;
            block(w, face.add(rx * Math.sin(hourAngle) * d,
                    Math.cos(hourAngle) * d, rz * Math.sin(hourAngle) * d),
                    Blocks.AMETHYST_BLOCK.getDefaultState(), 4);
        }
        block(w, face, Blocks.GOLD_BLOCK.getDefaultState(), 8);
    }

    /** Twin clock rings and four floating hour markers. */
    public static void timeStop(ServerWorld w, Vec3d center) {
        ring(w, center, 3.8, 24, 0.15, Blocks.AMETHYST_BLOCK.getDefaultState(), 110, 0);
        ring(w, center, 2.8, 16, 2.4, Blocks.PURPUR_BLOCK.getDefaultState(), 110, Math.PI / 16);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * i / 2.0;
            pillar(w, center, Math.cos(a) * 3.3, Math.sin(a) * 3.3, 3,
                    Blocks.CRYING_OBSIDIAN.getDefaultState(), 110);
        }
    }

    /** A rewind spiral that looks like a physical time coil. */
    public static void rewind(ServerWorld w, Vec3d center) {
        for (int i = 0; i < 28; i++) {
            double t = i / 27.0;
            double a = -t * Math.PI * 5.0;
            block(w, center.add(Math.cos(a) * (1.2 + t), 0.2 + t * 3.8,
                    Math.sin(a) * (1.2 + t)), Blocks.AMETHYST_BLOCK.getDefaultState(), 45);
        }
        ring(w, center, 2.5, 16, 0.1, Blocks.PURPUR_BLOCK.getDefaultState(), 45, 0);
    }

    /** A wide suspended dome frame for the slow field. */
    public static void slowField(ServerWorld w, Vec3d center, double radius) {
        ring(w, center, radius, 32, 0.15, Blocks.CRYING_OBSIDIAN.getDefaultState(), 250, 0);
        ring(w, center, radius * 0.72, 24, 3.0, Blocks.AMETHYST_BLOCK.getDefaultState(), 250, Math.PI / 24);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2.0 * i / 8;
            for (int y = 1; y <= 3; y++) {
                block(w, center.add(Math.cos(a) * radius * 0.72, y, Math.sin(a) * radius * 0.72),
                        Blocks.PURPUR_BLOCK.getDefaultState(), 250);
            }
        }
    }

    /** Two forward-pointing energy wings around the caster. */
    public static void haste(ServerWorld w, Vec3d center) {
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 7; i++) {
                block(w, center.add(side * (0.8 + i * 0.42), 0.4 + i * 0.22, 1.0 + i * 0.28),
                        Blocks.AMETHYST_BLOCK.getDefaultState(), 55);
            }
        }
        ring(w, center, 1.8, 12, 0.1, Blocks.PURPUR_BLOCK.getDefaultState(), 55, 0);
    }

    /** A standing rectangular portal gate at the destination. */
    public static void leap(ServerWorld w, Vec3d center) {
        for (int y = 0; y < 4; y++) {
            block(w, center.add(-1.5, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), 35);
            block(w, center.add(1.5, y, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), 35);
        }
        for (int x = -1; x <= 1; x++) {
            block(w, center.add(x, 0, 0), Blocks.AMETHYST_BLOCK.getDefaultState(), 35);
            block(w, center.add(x, 3, 0), Blocks.AMETHYST_BLOCK.getDefaultState(), 35);
        }
    }

    /** Layered growth rings with rising crystal-like pillars. */
    public static void growth(ServerWorld w, Vec3d center) {
        ring(w, center, 2.0, 16, 0.1, Blocks.MOSS_BLOCK.getDefaultState(), 100, 0);
        ring(w, center, 4.0, 24, 0.1, Blocks.AMETHYST_BLOCK.getDefaultState(), 100, Math.PI / 24);
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2.0 * i / 6;
            pillar(w, center, Math.cos(a) * 2.8, Math.sin(a) * 2.8, 3,
                    Blocks.MOSS_BLOCK.getDefaultState(), 100);
        }
    }
}
