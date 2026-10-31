package com.example.chronomod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChronoMod implements ModInitializer {
    public static final String MOD_ID = "chronomod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModItems.init();

        ServerTickEvents.END_SERVER_TICK.register(TimeManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(TimeManager::shutdown);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                TimeManager.onDisconnect(handler.player.getUuid()));

        // 시간 정지 중 받은 피해를 모아 두었다가 정지가 풀릴 때 한꺼번에 적용
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(TimeStop::onDamage);

        LOGGER.info("Chrono Mod loaded. The clock is ticking...");
    }
}
