package com.cubefury.vendingmachine.util;

public class ColorUtils {

    private static final ColorResource.Factory color = new ColorResource.Factory("vendingmachine");

    public static final ColorResource
    // spotless:off
        displayText                     = color.rgb("displayText",                      "0x000000"),
        textColorConditionDefault       = color.rgb("textColorConditionDefault",        "0x000000"),
        textColorConditionSatisfied     = color.rgb("textColorConditionSatisfied",      "0x55D441"),
        textColorConditionUnsatisfied   = color.rgb("textColorConditionUnsatisfied",    "0xA87A5E"),
        nc_InputsOverlay                = color.rgb("nc_InputsOverlay",                 "0xFDD835"),
        tradeDisplayText                = color.rgb("tradeDisplayText",                 "0xFFFFFF"),

        tradeDisplayDisabled            = color.argb("tradeDisplayDisabled",            "0xBB000000"),
        tradeDisplayListTradableNow     = color.argb("tradeDisplayListTradableNow",     "0x883CFF00"),
        tradeDisplayListUntradableNow   = color.argb("tradeDisplayListUntradableNow",   "0x88333333"),
        tradeDisplayListCurrentSelected = color.argb("tradeDisplayListCurrentSelected", "0xAA039BE5"),
        volumeSliderBackground          = color.argb("volumeSliderBackground",          "0xFF000000");
    // spotless:on
}
