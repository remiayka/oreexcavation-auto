package com.oreexcavationauto;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;

public class OreExcavationAutoConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue AUTO_DETECT_ORE_IN_ID;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WHITELIST;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLACKLIST;

    static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("Ore Excavation Auto Configuration").push("general");

        ENABLED = BUILDER
            .comment("Enable or disable the auto-excavation feature")
            .define("enabled", true);

        AUTO_DETECT_ORE_IN_ID = BUILDER
            .comment("Automatically activate for blocks whose registry ID contains 'ore'")
            .define("autoDetectOreInId", true);

        WHITELIST = BUILDER
            .comment("Additional blocks to always activate excavation for (registry IDs, e.g. 'minecraft:coal_ore')")
            .defineListAllowEmpty("whitelist", new ArrayList<>(), o -> o instanceof String);

        BLACKLIST = BUILDER
            .comment("Blocks to never activate excavation for (overrides whitelist and auto-detection)")
            .defineListAllowEmpty("blacklist", new ArrayList<>(), o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, SPEC, "oreexcavationauto-client.toml");
    }

    public static boolean isEnabled() {
        return ENABLED.get();
    }

    public static boolean isAutoDetect() {
        return AUTO_DETECT_ORE_IN_ID.get();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getWhitelist() {
        return (List<String>) WHITELIST.get();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getBlacklist() {
        return (List<String>) BLACKLIST.get();
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
        SPEC.save();
    }

    public static void setAutoDetect(boolean value) {
        AUTO_DETECT_ORE_IN_ID.set(value);
        SPEC.save();
    }

    public static void addToWhitelist(String blockId) {
        List<String> list = new ArrayList<>(getWhitelist());
        if (!list.contains(blockId)) {
            list.add(blockId);
            WHITELIST.set(list);
            SPEC.save();
        }
    }

    public static void removeFromWhitelist(String blockId) {
        List<String> list = new ArrayList<>(getWhitelist());
        list.remove(blockId);
        WHITELIST.set(list);
        SPEC.save();
    }

    public static void addToBlacklist(String blockId) {
        List<String> list = new ArrayList<>(getBlacklist());
        if (!list.contains(blockId)) {
            list.add(blockId);
            BLACKLIST.set(list);
            SPEC.save();
        }
    }

    public static void removeFromBlacklist(String blockId) {
        List<String> list = new ArrayList<>(getBlacklist());
        list.remove(blockId);
        BLACKLIST.set(list);
        SPEC.save();
    }

    public static boolean shouldActivate(String blockId) {
        if (!isEnabled()) return false;

        // Blacklist always wins
        if (getBlacklist().contains(blockId)) return false;

        // Whitelist
        if (getWhitelist().contains(blockId)) return true;

        // Auto-detect: block ID contains "ore"
        if (isAutoDetect() && blockId.toLowerCase().contains("ore")) return true;

        return false;
    }
}
