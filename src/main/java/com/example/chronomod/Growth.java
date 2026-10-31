package com.example.chronomod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.SaplingBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/** 시간 성장: 주변의 작물/묘목/식물의 시간을 빠르게 흘려 순식간에 자라게 한다. 시전자는 재생 효과. */
public final class Growth {
    public static final int RADIUS = 7;
    public static final int WAVES = 6;

    private Growth() {}

    public static boolean start(ServerPlayerEntity p) {
        ServerWorld w = (ServerWorld) p.getWorld();
        BlockPos center = p.getBlockPos();
        Vec3d cv = p.getPos();

        // 시간 성장의 시전자 보상: 체력 회복과 함께 허기/포만감도 보충한다.
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120, 1, false, true));
        p.getHungerManager().setFoodLevel(20);
        p.getHungerManager().setSaturationLevel(5.0f);

        Fx.sound(w, cv, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.5f, 1.8f);
        w.spawnParticles(ParticleTypes.FLASH, cv.x, cv.y + 1, cv.z, 1, 0, 0, 0, 0);
        Fx.helix(w, ParticleTypes.HAPPY_VILLAGER, cv, 1.2, 3.0, 4, 0, 14);
        Fx.sphere(w, ParticleTypes.TOTEM_OF_UNDYING, cv.add(0, 1, 0), 50, 0.5);

        for (int wave = 0; wave < WAVES; wave++) {
            final int wv = wave;
            Scheduler.later(wave * 8, () -> pulse(w, center, cv, wv));
        }
        return true;
    }

    private static void pulse(ServerWorld w, BlockPos c, Vec3d cv, int wave) {
        int r = RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = c.add(dx, dy, dz);
                    BlockState st = w.getBlockState(pos);
                    Block b = st.getBlock();

                    boolean grew = false;
                    if (b instanceof CropBlock crop) {
                        if (!crop.isMature(st)) {
                            crop.applyGrowth(w, pos, st);
                            grew = true;
                        }
                    } else if (b instanceof SaplingBlock || b instanceof StemBlock) {
                        Fertilizable fz = (Fertilizable) b;
                        if (fz.isFertilizable(w, pos, st) && fz.canGrow(w, w.random, pos, st)) {
                            fz.grow(w, w.random, pos, st);
                            grew = true;
                        }
                    } else if (st.hasRandomTicks() && !(b instanceof FireBlock) && !(b instanceof FluidBlock)) {
                        // 그 외 블록은 랜덤 틱을 몰아서 돌려 준다 (사탕수수, 선인장, 구리 산화, 잔디 번짐 ...)
                        for (int i = 0; i < 4; i++) {
                            BlockState cur = w.getBlockState(pos);
                            if (!cur.hasRandomTicks()) break;
                            cur.randomTick(w, pos, w.random);
                        }
                        grew = true;
                    }

                    if (grew && w.random.nextInt(4) == 0) {
                        w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                                3, 0.3, 0.3, 0.3, 0.0);
                    }
                }
            }
        }

        double radius = r * (wave + 1) / (double) WAVES;
        Fx.ring(w, ParticleTypes.HAPPY_VILLAGER, cv.add(0, 0.2, 0), radius, 48, 0.05);
        Fx.ring(w, ParticleTypes.TOTEM_OF_UNDYING, cv.add(0, 0.4, 0), radius, 16, 0.15);
        Fx.sound(w, cv, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2f, 0.8f + wave * 0.2f);
    }
}
