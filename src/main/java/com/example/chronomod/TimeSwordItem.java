package com.example.chronomod;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** A cosmic blade that leaves a three-second time-rift burn. */
public class TimeSwordItem extends Item {
    public TimeSwordItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld world) {
            final var targetId = target.getUuid();
            final var worldKey = world.getRegistryKey();
            final var attackerId = attacker.getUuid();
            for (int second = 1; second <= 3; second++) {
                final int pulse = second;
                Scheduler.later(second * 20, () -> {
                    var targetWorld = world.getServer().getWorld(worldKey);
                    if (targetWorld == null) return;
                    var victim = targetWorld.getEntity(targetId);
                    var sourceEntity = targetWorld.getEntity(attackerId);
                    if (!(victim instanceof LivingEntity living) || !living.isAlive()) return;
                    if (sourceEntity == null || !sourceEntity.isAlive()) return;

                    living.damage(targetWorld, targetWorld.getDamageSources().magic(), 3.0f);
                    Vec3d center = living.getPos().add(0, living.getHeight() * 0.55, 0);
                    targetWorld.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                            18, 0.32, 0.42, 0.32, 0.08);
                    targetWorld.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                            8, 0.22, 0.35, 0.22, 0.02);
                    targetWorld.spawnParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
                            10, 0.28, 0.38, 0.28, 0.12);
                });
            }
            Vec3d center = target.getPos().add(0, target.getHeight() * 0.55, 0);
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z,
                    24, 0.35, 0.5, 0.35, 0.1);
            world.spawnParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    12, 0.3, 0.45, 0.3, 0.03);
        }
        return true;
    }
}
