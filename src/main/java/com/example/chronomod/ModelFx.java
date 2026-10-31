package com.example.chronomod;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.decoration.DisplayEntity.ItemDisplayEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Lightweight 3D-only combat visuals built from the mod's baked item models.
 * Displays are short-lived and never use particle systems.
 */
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

    /** A restrained 3D gear burst at the point of impact. */
    public static void impact(ServerWorld world, Vec3d center, double strength) {
        float spread = (float) Math.max(0.25, Math.min(1.25, strength));
        display(world, ModItems.TIME_STOP_GEAR_A,
                center.add(-spread, 0, 0), 0, 90, 8);
        display(world, ModItems.TIME_STOP_GEAR_B,
                center.add(spread, 0, 0), 35, 90, 8);
        display(world, ModItems.TIME_STOP_HOUR_HAND,
                center.add(0, 0.12, 0), 90, 90, 8);
    }
}
