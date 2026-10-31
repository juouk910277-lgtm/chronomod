package com.example.chronomod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.codec.PacketCodecs;

/** Compact server-to-client snapshot for the held-watch HUD. */
public record AbilityHudPayload(String data) implements CustomPayload {
    public static final Id<AbilityHudPayload> ID = new Id<>(ChronoMod.id("ability_hud"));
    public static final PacketCodec<RegistryByteBuf, AbilityHudPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING, AbilityHudPayload::data, AbilityHudPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
