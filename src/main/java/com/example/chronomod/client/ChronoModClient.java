package com.example.chronomod.client;

import com.example.chronomod.Ability;
import com.example.chronomod.AbilityHudPayload;
import com.example.chronomod.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public final class ChronoModClient implements ClientModInitializer {
    private static volatile String hudData = "-";

    private static ItemStack iconFor(int i) {
        return switch (i) {
            case 0 -> new ItemStack(Items.CLOCK);
            case 1 -> new ItemStack(Items.AMETHYST_SHARD);
            case 2 -> new ItemStack(Items.CRYING_OBSIDIAN);
            case 3 -> new ItemStack(Items.FEATHER);
            case 4 -> new ItemStack(Items.ENDER_PEARL);
            default -> new ItemStack(Items.GLOW_BERRIES);
        };
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AbilityHudPayload.ID,
                (payload, context) -> hudData = payload.data());

        HudRenderCallback.EVENT.register((draw, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.textRenderer == null) return;
            if (!client.player.getMainHandStack().isOf(ModItems.CHRONO_WATCH)
                    && !client.player.getOffHandStack().isOf(ModItems.CHRONO_WATCH)) return;

            String[] values = hudData.split(",");
            if (values.length != Ability.values().length + 1) return;
            int selected;
            try { selected = Integer.parseInt(values[0]); }
            catch (NumberFormatException ex) { return; }

            // Compact 3-column x 2-row skill selector, matching the reference image.
            int x = 8, y = 8, cellW = 38, cellH = 37, gap = 3;
            int panelW = cellW * 3 + gap * 2 + 8;
            int panelH = cellH * 2 + gap + 8;
            draw.fill(x - 3, y - 3, x + panelW + 3, y + panelH + 3, 0xA8000715);
            draw.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, 0xD9101428);
            draw.fill(x - 2, y - 2, x + panelW + 2, y, 0xFF8D69D5);

            for (int i = 0; i < Ability.values().length; i++) {
                int col = i % 3, row = i / 3;
                int cx = x + col * (cellW + gap);
                int cy = y + row * (cellH + gap);
                long remaining;
                try { remaining = Math.max(0, Long.parseLong(values[i + 1])); }
                catch (NumberFormatException ex) { remaining = 0; }
                boolean active = i == selected;
                boolean ready = remaining == 0;

                draw.fill(cx, cy, cx + cellW, cy + cellH,
                        active ? 0xEE49366F : 0xCC20263D);
                draw.fill(cx, cy, cx + cellW, cy + 1, active ? 0xFFFFD58A : 0xFF59617E);
                draw.fill(cx, cy + cellH - 1, cx + cellW, cy + cellH,
                        active ? 0xFFFFD58A : 0xFF59617E);
                draw.fill(cx, cy, cx + 1, cy + cellH,
                        active ? 0xFFFFD58A : 0xFF59617E);
                draw.fill(cx + cellW - 1, cy, cx + cellW, cy + cellH,
                        active ? 0xFFFFD58A : 0xFF59617E);

                draw.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(i + 1)),
                        cx + 2, cy + 1, active ? 0xFFFFE9A8 : 0xFFE1DDF5);
                draw.drawItem(iconFor(i), cx + 11, cy + 5);

                int barX = cx + 4, barY = cy + 27, barW = cellW - 8;
                draw.fill(barX, barY, barX + barW, barY + 4, 0xFF090D1B);
                int total = Ability.values()[i].cooldownTicks;
                float progress = total <= 0 ? 1.0f
                        : Math.max(0.0f, Math.min(1.0f, 1.0f - remaining / (float) total));
                int fillW = Math.round(barW * progress);
                if (fillW > 0) draw.fill(barX, barY, barX + fillW, barY + 4,
                        ready ? 0xFF6FE6D0 : 0xFFB18AFF);
                if (!ready) {
                    String seconds = String.format(java.util.Locale.ROOT, "%.0f", remaining / 20.0);
                    draw.drawTextWithShadow(client.textRenderer, Text.literal(seconds),
                            cx + cellW - client.textRenderer.getWidth(seconds) - 3, cy + 1, 0xFFFFC77D);
                }
            }
        });
    }
}
