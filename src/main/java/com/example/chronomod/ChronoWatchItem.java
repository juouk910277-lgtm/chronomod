package com.example.chronomod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ChronoWatchItem extends Item {
    public ChronoWatchItem(Settings settings) {
        super(settings);
    }

    /** 우클릭: 능력 발동 / 웅크리고 우클릭: 능력 변경 */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.SUCCESS;
        }
        if (player.isSneaking()) {
            TimeManager.cycle(player);
        } else {
            TimeManager.cast(player);
        }
        return ActionResult.SUCCESS;
    }
}
