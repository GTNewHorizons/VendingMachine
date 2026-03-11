package com.cubefury.vendingmachine.trade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.VendingMachine;

public enum TradeCategory {

    FAVOURITES("favourites", "vendingmachine.category.favourites", "gui/icons/favourites.png"),
    UNKNOWN("unknown", "vendingmachine.category.unknown", "gui/icons/unknown.png"),
    ALL("all", "vendingmachine.category.all", "gui/icons/all.png"),
    COMPONENTS("components", "vendingmachine.category.components", "gui/icons/components.png"),
    RAW("raw", "vendingmachine.category.raw", "gui/icons/raw.png"),
    FARMING("farming", "vendingmachine.category.farming", "gui/icons/farming.png"),
    CHEMISTRY("chemistry", "vendingmachine.category.chemistry", "gui/icons/chemistry.png"),
    MAGIC("magic", "vendingmachine.category.magic", "gui/icons/magic.png"),
    BEES("bees", "vendingmachine.category.bees", "gui/icons/bees.png"),
    MISC("misc", "vendingmachine.category.misc", "gui/icons/misc.png");

    private final String key;
    private final String unlocalized_name;
    private final UITexture texture;

    private static final Map<String, TradeCategory> ENUM_MAP;

    private static final int size_x = 32;
    private static final int size_y = 32;

    TradeCategory(String key, String unlocalized_name, String texture) {
        this.key = key;
        this.unlocalized_name = unlocalized_name;
        this.texture = UITexture.builder()
            .location(VendingMachine.MODID, texture)
            .imageSize(size_x, size_y)
            .name(unlocalized_name)
            .build();
    }

    static {
        Map<String, TradeCategory> map = new HashMap<>();
        for (TradeCategory instance : TradeCategory.values()) {
            map.put(instance.getKey(), instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public String getKey() {
        return this.key;
    }

    public String getUnlocalized_name() {
        return this.unlocalized_name;
    }

    public UITexture getTexture() {
        return this.texture;
    }

    public static TradeCategory ofString(String key) {
        if (ENUM_MAP.get(key) == null) {
            VendingMachine.LOG.warn("Unknown trade category {}, defaulting to UNKNOWN", key);
            return UNKNOWN;
        }
        return ENUM_MAP.get(key);
    }

    @Override
    public String toString() {
        return getUnlocalized_name();
    }
}
