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
            case 1 -> new ItemStack(Items.ECHO_SHARD);
            case 2 -> new ItemStack(Items.AMETHYST_SHARD);
            case 3 -> new ItemStack(Items.SUGAR);
            case 4 -> new ItemStack(Items.ENDER_PEARL);
            default -> new ItemStack(Items.WHEAT);
        };
    }

    private static String nameFor(int i) {
        return switch (i) {
            case 0 -> "TIME STOP";
            case 1 -> "REWIND";
            case 2 -> "SLOW FIELD";
            case 3 -> "TIME HASTE";
            case 4 -> "TIME LEAP";
            default -> "GROWTH";
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

            int x = client.getWindow().getScaledWidth() - 174;
            int y = 42;
            int width = 166;
            int rowH = 30;
            int height = 34 + rowH * Ability.values().length + 8;

            draw.fill(x - 5, y - 5, x + width + 5, y + height + 5, 0x99030819);
            draw.fill(x - 3, y - 3, x + width + 3, y + height + 3, 0xEE101A38);
            draw.fill(x - 3, y - 3, x + width + 3, y, 0xFFB18AFF);
            draw.fill(x - 3, y, x, y + height + 3, 0xFF5B8DFF);
            draw.fill(x + width, y, x + width + 3, y + height + 3, 0xFF5B8DFF);

            draw.drawItem(new ItemStack(Items.CLOCK), x + 7, y + 7);
            draw.drawTextWithShadow(client.textRenderer, Text.literal("CHRONO ARTS"),
                    x + 29, y + 7, 0xFFF1E9FF);
            draw.drawTextWithShadow(client.textRenderer, Text.literal("TEMPORAL SKILL MATRIX"),
                    x + 29, y + 19, 0xFF8FA8DB);

            int rowY = y + 34;
            for (int i = 0; i < Ability.values().length; i++) {
                long remaining;
                try { remaining = Math.max(0, Long.parseLong(values[i + 1])); }
                catch (NumberFormatException ex) { remaining = 0; }
                boolean active = i == selected;
                boolean ready = remaining == 0;
                int bottom = rowY + rowH - 2;

                draw.fill(x + 5, rowY, x + width - 5, bottom,
                        active ? 0xDD39336B : 0x99505B7B);
                if (active) {
                    draw.fill(x + 5, rowY, x + 8, bottom, 0xFFD4A8FF);
                    draw.fill(x + 8, rowY, x + width - 5, rowY + 1, 0xFF8D78E8);
                }

                draw.drawItem(iconFor(i), x + 10, rowY + 5);
                draw.drawTextWithShadow(client.textRenderer, Text.literal(nameFor(i)),
                        x + 32, rowY + 5, active ? 0xFFFFFFFF : 0xFFD3DDF5);

                String status = ready ? "READY" : String.format(java.util.Locale.ROOT, "%.1fs", remaining / 20.0);
                int statusColor = ready ? 0xFF83E8D0 : 0xFFFFC77D;
                draw.drawTextWithShadow(client.textRenderer, Text.literal(status),
                        x + width - 42, rowY + 5, statusColor);

                int barX = x + 32;
                int barY = rowY + 19;
                int barW = width - 44;
                draw.fill(barX, barY, barX + barW, barY + 3, 0xAA080D20);
                int total = Ability.values()[i].cooldownTicks;
                float progress = total <= 0 ? 1.0f : Math.max(0.0f, Math.min(1.0f, 1.0f - remaining / (float) total));
                int fillW = Math.round(barW * progress);
                if (fillW > 0) draw.fill(barX, barY, barX + fillW, barY + 3,
                        ready ? 0xFF6FE6D0 : 0xFF9C83FF);

                rowY += rowH;
            }
        });
    }
}
