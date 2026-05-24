package com.cubefury.vendingmachine.util;

import static com.cubefury.vendingmachine.util.Translator.translate;

import java.util.EnumSet;
import java.util.Set;

import com.cubefury.vendingmachine.VendingMachine;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum GuiParams {

    // nei
    display_text_color(0x000000, true),
    condition_default_color(0x000000, true),
    condition_satisfied_color(0x55D441, true),
    condition_unsatisfied_color(0xA87A5E, true),
    nc_inputs_overlay_color(0xFDD835, true),

    // trade display
    trade_display_disabled_color(0xBB000000, false),
    trade_display_list_tradable_now_color(0x883CFF00, false),
    trade_display_list_untradable_now_color(0x88333333, false),
    trade_display_list_current_selected_color(0xAA039BE5, false),
    trade_display_text_color(0xFFFFFF, true),

    // volume slider
    volume_slider_background(0xFF000000, false),

    // new line to prevent merge conflicts
    ;

    private static final Set<GuiParams> warnedParams = EnumSet.noneOf(GuiParams.class);
    private static Boolean angelicaLoaded = null;

    private final int value;
    private final boolean isTextColor;

    GuiParams(final int value, final boolean isTextColor) {
        this.value = value;
        this.isTextColor = isTextColor;
    }

    public int getColor(boolean hasAlphaChannel) {
        String hex = translate(this.getUnlocalized());
        int bitMask = hasAlphaChannel ? 0xFFFFFFFF : 0xFFFFFF;
        if (hex.equals(this.getUnlocalized())) {
            return this.value & bitMask;
        }

        if (isAngelicaFormatString(hex)) {
            if (!isTextColor && warnedParams.add(this)) {
                VendingMachine.LOG.warn(
                    "GuiParams '" + getUnlocalized()
                        + "' is not a text color and does not support Angelica color formatting; using default");
            }
            return this.value & bitMask;
        }

        int color = this.value;
        try {
            color = Integer.parseUnsignedInt(hex, 16) & bitMask;
        } catch (NumberFormatException e) {
            VendingMachine.LOG.warn("Couldn't format color correctly for: " + getUnlocalized(), e);
        }
        return color;
    }

    /**
     * Returns the Angelica color format string for this param when all conditions are met:
     * this is a text-color param, Angelica is loaded, and the lang override is a format string.
     * Returns null in all other cases; callers should fall back to {@link #getColor(boolean)}.
     */
    public String getColorString() {
        if (!isTextColor) {
            return null;
        }
        String translated = translate(this.getUnlocalized());
        if (translated.equals(this.getUnlocalized()) || !isAngelicaFormatString(translated)) {
            return null;
        }
        if (!isAngelicaLoaded()) {
            return null;
        }
        return translated;
    }

    public int getInt() {
        String rawInt = translate(this.getUnlocalized());
        if (rawInt.equals(this.getUnlocalized())) {
            return this.value;
        }

        int value = this.value;
        try {
            value = Integer.parseInt(rawInt);
        } catch (NumberFormatException e) {
            VendingMachine.LOG.warn("Couldn't fetch integer correctly for: " + getUnlocalized(), e);
        }
        return value;
    }

    public String getUnlocalized() {
        return "vendingmachine.gui." + this;
    }

    private static boolean isAngelicaFormatString(String value) {
        return value.indexOf('&') >= 0 || value.indexOf('§') >= 0;
    }

    private static boolean isAngelicaLoaded() {
        if (angelicaLoaded == null) {
            angelicaLoaded = Loader.isModLoaded("angelica");
        }
        return angelicaLoaded;
    }
}
