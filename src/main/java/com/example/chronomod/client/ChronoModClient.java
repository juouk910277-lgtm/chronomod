package com.example.chronomod.client;

import com.example.chronomod.Ability;
import com.example.chronomod.AbilityHudPayload;
import com.example.chronomod.ChronoMod;
import com.example.chronomod.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class ChronoModClient implements ClientModInitializer {
    private static volatile String hudData = "-";

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AbilityHudPayload.ID, (payload, context) ->
                hudData = payload.data());

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.textRenderer == null) return;
            if (!client.player.getMainHandStack().isOf(ModItems.CHRONO_WATCH)
                    && !client.player.getOffHandStack().isOf(ModItems.CHRONO_WATCH)) return;
            String data = hudData;
            if (data.equals("-") || data.isBlank()) return;

            String[] values = data.split(",");
            if (values.length != Ability.values().length + 1) return;
            int selected;
            try { selected = Integer.parseInt(values[0]); }
            catch (NumberFormatException ignored) { return; }

            int width = client.getWindow().getScaledWidth();
            int x = width - 126;
            int y = 42;
            int height = 18 + Ability.values().length * 13;
            drawContext.fill(x - 5, y - 5, width - 5, y + height, 0xB0101428);
            drawContext.fill(x - 5, y - 5, width - 5, y - 3, 0xFF8A68FF);
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal("✦ TIME ARTS"), x, y, 0xFFD9D0FF);
            y += 17;
            for (Ability ability : Ability.values()) {
                int index = ability.ordinal();
                long ticks;
                try { ticks = Long.parseLong(values[index + 1]); }
                catch (NumberFormatException ignored) { ticks = 0; }
                boolean active = index == selected;
                String name = ability.displayName().getString();
                String timer = ticks <= 0 ? "READY" : String.format("%ds", (ticks + 19) / 20);
                int color = active ? 0xFFBDA8FF : (ticks <= 0 ? 0xFFE5E9FF : 0xFF8992B8);
                if (active) drawContext.fill(x - 3, y - 1, width - 8, y + 11, 0x663D326D);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal((active ? "› " : "  ") + name), x, y, color);
                drawContext.drawTextWithShadow(client.textRenderer, Text.literal(timer), width - 45, y,
                        ticks <= 0 ? 0xFF77E6C0 : 0xFFFFC879);
                y += 13;
            }
        });
    }
}
