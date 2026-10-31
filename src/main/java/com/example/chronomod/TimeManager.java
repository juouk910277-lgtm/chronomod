package com.example.chronomod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 능력 선택 / 쿨타임 / 발동 / 틱 처리의 중심 */
public final class TimeManager {
    /** 서버가 켜진 뒤 흐른 틱 수 (월드 시간이 명령어로 바뀌어도 영향받지 않도록 자체 카운터 사용) */
    public static long now = 0;

    private static final Map<UUID, Ability> SELECTED = new HashMap<>();
    private static final Map<UUID, EnumMap<Ability, Long>> COOLDOWNS = new HashMap<>();

    private TimeManager() {}

    public static void tick(MinecraftServer server) {
        now++;
        Rewind.record(server);
        TimeStop.tick(server);
        Rewind.tick(server);
        SlowField.tick(server);
        Haste.tick(server);
        Scheduler.tick(now);
        if (now % 5 == 0) syncHud(server);
    }

    private static void syncHud(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ItemStack main = player.getMainHandStack();
            ItemStack off = player.getOffHandStack();
            if (!main.isOf(ModItems.CHRONO_WATCH) && !off.isOf(ModItems.CHRONO_WATCH)) {
                ServerPlayNetworking.send(player, new AbilityHudPayload("-"));
                continue;
            }
            StringBuilder data = new StringBuilder().append(selected(player).ordinal());
            for (Ability ability : Ability.values()) {
                data.append(',').append(remaining(player.getUuid(), ability));
            }
            ServerPlayNetworking.send(player, new AbilityHudPayload(data.toString()));
        }
    }

    public static Ability selected(ServerPlayerEntity p) {
        return SELECTED.getOrDefault(p.getUuid(), Ability.TIME_STOP);
    }

    public static void cycle(ServerPlayerEntity p) {
        Ability next = selected(p).next();
        SELECTED.put(p.getUuid(), next);
        p.sendMessage(Text.translatable("msg.chronomod.selected", next.displayName()), true);
        Fx.sound((ServerWorld) p.getWorld(), p.getPos(), SoundEvents.BLOCK_COMPARATOR_CLICK, 1.0f, 1.5f);
    }

    public static long remaining(UUID id, Ability a) {
        EnumMap<Ability, Long> m = COOLDOWNS.get(id);
        if (m == null) return 0;
        Long end = m.get(a);
        return end == null ? 0 : Math.max(0, end - now);
    }

    private static void setCooldown(UUID id, Ability a) {
        COOLDOWNS.computeIfAbsent(id, k -> new EnumMap<>(Ability.class)).put(a, now + a.cooldownTicks);
    }

    public static void cast(ServerPlayerEntity p) {
        Ability a = selected(p);
        boolean creative = p.isCreative();

        long rem = remaining(p.getUuid(), a);
        if (!creative && rem > 0) {
            p.sendMessage(Text.translatable("msg.chronomod.cooldown", a.displayName(), (rem + 19) / 20), true);
            return;
        }

        boolean ok = switch (a) {
            case TIME_STOP -> TimeStop.start(p);
            case REWIND -> Rewind.start(p);
            case SLOW_FIELD -> SlowField.start(p);
            case HASTE -> Haste.start(p);
            case LEAP -> Leap.start(p);
            case GROWTH -> Growth.start(p);
        };

        if (ok) {
            ServerPlayNetworking.send(p, new ScreenFxPayload(a.ordinal()));
            if (!creative) setCooldown(p.getUuid(), a);
            p.sendMessage(Text.translatable("msg.chronomod.cast", a.displayName()), true);
        }
    }

    public static void onDisconnect(UUID id) {
        Rewind.onDisconnect(id);
    }

    public static void shutdown(MinecraftServer server) {
        TimeStop.endAll(server);   // 몹의 NoAI 상태가 월드에 저장되지 않도록 반드시 복구
        Rewind.clear();
        SlowField.clear();
        Haste.clear();
        Scheduler.clear();
        COOLDOWNS.clear();
        SELECTED.clear();
    }
}
