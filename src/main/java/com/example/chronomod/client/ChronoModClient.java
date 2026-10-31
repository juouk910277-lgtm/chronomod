package com.example.chronomod.client;

import com.example.chronomod.Ability;
import com.example.chronomod.AbilityHudPayload;
import com.example.chronomod.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ChronoModClient implements ClientModInitializer {
    private static volatile String hudData = "-";

    // 7x7 pixel-art glyphs: each ability has its own silhouette and accent color.
    private static final String[][] GLYPHS = {
            {"0011100","0111110","1101011","1101011","1100011","0111110","0011100"}, // clock
            {"0001000","0011100","0111110","1111111","0111110","0011100","0001000"}, // rewind
            {"1000001","0100010","0010100","0001000","0010100","0100010","1000001"}, // slow
            {"0001000","0011000","0111110","1111111","0001100","0001100","0001000"}, // haste
            {"0001000","0011100","0111110","0011100","0011100","0110110","1100011"}, // leap
            {"0011100","0111110","1101011","1101011","0111110","0011100","0001000"}  // growth
    };
    private static final int[] ACCENTS = {
            0xFFFFD36E, 0xFFB69CFF, 0xFF73D9F2,
            0xFFFFA66E, 0xFF83E5B5, 0xFF9DDB86
    };

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

            int cellW = 53, cellH = 43, gap = 3;
            int panelW = cellW * 3 + gap * 2 + 12;
            int panelH = cellH * 2 + gap + 25;
            int x = 8;
            int y = client.getWindow().getScaledHeight() - panelH - 8;

            // Dark glass-like panel with a restrained brass/purple frame.
            draw.fill(x - 3, y - 3, x + panelW + 3, y + panelH + 3, 0x88000000);
            draw.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, 0xE90B1020);
            draw.fill(x - 2, y - 2, x + panelW + 2, y, 0xFFFFD36E);
            draw.fill(x - 2, y + 17, x + panelW + 2, y + 18, 0x664B5678);
            draw.drawTextWithShadow(client.textRenderer, Text.literal("CHRONO  /  ABILITIES"),
                    x + 5, y + 5, 0xFFE8D8B4);
            draw.drawTextWithShadow(client.textRenderer, Text.literal("1 - 6"),
                    x + panelW - client.textRenderer.getWidth("1 - 6") - 5, y + 5, 0xFF8C96B5);

            for (int i = 0; i < Ability.values().length; i++) {
                int col = i % 3, row = i / 3;
                int cx = x + 3 + col * (cellW + gap);
                int cy = y + 22 + row * (cellH + gap);
                long remaining;
                try { remaining = Math.max(0, Long.parseLong(values[i + 1])); }
                catch (NumberFormatException ex) { remaining = 0; }
                boolean active = i == selected;
                boolean ready = remaining == 0;
                int accent = ACCENTS[i];

                draw.fill(cx, cy, cx + cellW, cy + cellH, active ? 0xF02B2940 : 0xD8172032);
                draw.fill(cx, cy, cx + cellW, cy + 1, active ? accent : 0xFF46516D);
                if (active) {
                    draw.fill(cx, cy + 1, cx + 2, cy + cellH - 1, accent);
                    draw.fill(cx + cellW - 2, cy + 1, cx + cellW, cy + cellH - 1, accent);
                }

                // Crisp 7x7 pixel icon, not a vanilla Minecraft item stack.
                int px = cx + 5, py = cy + 7, pixel = 2;
                for (int gy = 0; gy < 7; gy++) {
                    for (int gx = 0; gx < 7; gx++) {
                        if (GLYPHS[i][gy].charAt(gx) == '1') {
                            draw.fill(px + gx * pixel, py + gy * pixel,
                                    px + gx * pixel + pixel, py + gy * pixel + pixel,
                                    ready ? accent : 0xFF777F98);
                        }
                    }
                }

                String label = Ability.values()[i].displayName().getString();
                if (label.length() > 8) label = label.substring(0, 8);
                draw.drawTextWithShadow(client.textRenderer, Text.literal(label),
                        cx + 25, cy + 7, active ? 0xFFFFF0D2 : 0xFFD5D9E7);

                String status = ready ? "READY" :
                        String.format(java.util.Locale.ROOT, "%.0fs", remaining / 20.0);
                int statusColor = ready ? 0xFF83E5B5 : 0xFFFFC77D;
                draw.drawTextWithShadow(client.textRenderer, Text.literal(status),
                        cx + 25, cy + 20, statusColor);

                int barX = cx + 5, barY = cy + 35, barW = cellW - 10;
                draw.fill(barX, barY, barX + barW, barY + 3, 0xFF080C16);
                int total = Ability.values()[i].cooldownTicks;
                float progress = total <= 0 ? 1.0f
                        : Math.max(0.0f, Math.min(1.0f, 1.0f - remaining / (float) total));
                int fillW = Math.round(barW * progress);
                if (fillW > 0) draw.fill(barX, barY, barX + fillW, barY + 3,
                        ready ? 0xFF70DDBA : accent);
                draw.drawTextWithShadow(client.textRenderer, Text.literal(Integer.toString(i + 1)),
                        cx + cellW - 8, cy + 2, active ? 0xFFFFE6A8 : 0xFF8D98B4);
            }
        });
    }
}
