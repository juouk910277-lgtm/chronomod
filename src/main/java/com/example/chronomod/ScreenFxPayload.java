package com.example.chronomod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** One-shot first-person ability cast cue. */
public record ScreenFxPayload(int ability) implements CustomPayload {
    public static final Id<ScreenFxPayload> ID = new Id<>(ChronoMod.id("screen_fx"));
    public static final PacketCodec<RegistryByteBuf, ScreenFxPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, ScreenFxPayload::ability, ScreenFxPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
