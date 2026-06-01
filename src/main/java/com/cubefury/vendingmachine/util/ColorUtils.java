package com.cubefury.vendingmachine.util;

import com.gtnewhorizon.gtnhlib.color.ColorResource;

public class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("vendingmachine");

    public static final ColorResource
    // spotless:off
        displayText                     = color.rgb("displayText",                      "#000000"),
        textColorConditionDefault       = color.rgb("textColorConditionDefault",        "#000000"),
        textColorConditionSatisfied     = color.rgb("textColorConditionSatisfied",      "#55D441"),
        textColorConditionUnsatisfied   = color.rgb("textColorConditionUnsatisfied",    "#A87A5E"),
        nc_InputsOverlay                = color.rgb("nc_InputsOverlay",                 "#FDD835"),
        tradeDisplayText                = color.rgb("tradeDisplayText",                 "#FFFFFF"),

        tradeDisplayDisabled            = color.argb("tradeDisplayDisabled",            "#BB000000"),
        tradeDisplayListTradableNow     = color.argb("tradeDisplayListTradableNow",     "#883CFF00"),
        tradeDisplayListUntradableNow   = color.argb("tradeDisplayListUntradableNow",   "#88333333"),
        tradeDisplayListCurrentSelected = color.argb("tradeDisplayListCurrentSelected", "#AA039BE5"),
        volumeSliderBackground          = color.argb("volumeSliderBackground",          "#FF000000");
    // spotless:on
}
