package com.example.chronomod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public final class ModItems {
    private ModItems() {}

    public static final Item TIME_SWORD = register(
            "time_sword",
            TimeSwordItem::new,
            new Item.Settings().maxCount(1).rarity(Rarity.EPIC)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true));

    // Internal model item used only by the time-stop visual.
    public static final Item TIME_STOP_CLOCK = register(
            "time_stop_clock", Item::new, new Item.Settings().maxCount(1));

    public static final Item TIME_STOP_HOUR_HAND = register(
            "time_stop_hour_hand", Item::new, new Item.Settings().maxCount(1));
    public static final Item TIME_STOP_MINUTE_HAND = register(
            "time_stop_minute_hand", Item::new, new Item.Settings().maxCount(1));
    public static final Item TIME_STOP_GEAR_A = register(
            "time_stop_gear_a", Item::new, new Item.Settings().maxCount(1));
    public static final Item TIME_STOP_GEAR_B = register(
            "time_stop_gear_b", Item::new, new Item.Settings().maxCount(1));

    public static final Item TIME_RED_GEAR = register(
            "time_red_gear", Item::new, new Item.Settings().maxCount(1));
    public static final Item TIME_GREEN_CRYSTAL = register(
            "time_green_crystal", Item::new, new Item.Settings().maxCount(1));

    public static final Item CHRONO_WATCH = register(
            "chrono_watch",
            ChronoWatchItem::new,
            new Item.Settings()
                    .maxCount(1)
                    .rarity(Rarity.EPIC)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true));

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, ChronoMod.id(name));
        return Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(CHRONO_WATCH));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(TIME_SWORD));
    }
}
