package com.example.chronomod;

import net.minecraft.text.Text;

public enum Ability {
    TIME_STOP("time_stop", 900),   // 45초
    REWIND("rewind", 600),         // 30초
    SLOW_FIELD("slow_field", 500), // 25초
    HASTE("haste", 600),           // 30초
    LEAP("leap", 100),             // 5초
    GROWTH("growth", 400);         // 20초

    public final String key;
    public final int cooldownTicks;

    Ability(String key, int cooldownTicks) {
        this.key = key;
        this.cooldownTicks = cooldownTicks;
    }

    public Text displayName() {
        return Text.translatable("ability.chronomod." + key);
    }

    public Ability next() {
        Ability[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
