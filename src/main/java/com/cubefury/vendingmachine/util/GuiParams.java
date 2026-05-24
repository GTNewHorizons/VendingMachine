package com.cubefury.vendingmachine.util;

import static com.cubefury.vendingmachine.util.Translator.translate;

import com.cubefury.vendingmachine.VendingMachine;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum GuiParams {

    // nei
    display_text_color("000000", true),
    condition_default_color("000000", true),
    condition_satisfied_color("55D441", true),
    condition_unsatisfied_color("A87A5E", true),
    nc_inputs_overlay_color("FDD835", true),

    // trade display
    trade_display_disabled_color("BB000000", false),
    trade_display_list_tradable_now_color("883CFF00", false),
    trade_display_list_untradable_now_color("88333333", false),
    trade_display_list_current_selected_color("AA039BE5", false),
    trade_display_text_color("FFFFFF", true),

    // volume slider
    volume_slider_background("FF000000", false),

    // new line to prevent merge conflicts
    ;

    private static Boolean angelicaLoaded = null;

    private final String value;
    private final boolean isTextColor;

    GuiParams(final String value, final boolean isTextColor) {
        this.value = value;
        this.isTextColor = isTextColor;
    }

    public int getColor(boolean hasAlphaChannel) {
        String hex = translate(this.getUnlocalized());
        int bitMask = hasAlphaChannel ? 0xFFFFFFFF : 0xFFFFFF;
        if (hex.equals(this.getUnlocalized())) {
            return Integer.parseUnsignedInt(this.value, 16) & bitMask;
        }

        try {
            return Integer.parseUnsignedInt(hex, 16) & bitMask;
        } catch (NumberFormatException e) {
            VendingMachine.LOG.warn("Couldn't format color correctly for: " + getUnlocalized(), e);
            return Integer.parseUnsignedInt(this.value, 16) & bitMask;
        }
    }

    /**
     * Returns the Angelica color format string for this param when all conditions are met:
     * this is a text-color param, Angelica is loaded, and the lang override is a format string.
     * Returns null in all other cases; callers should fall back to {@link #getColor(boolean)}.
     */
    public String getAngelicaColorString() {
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
            return Integer.parseInt(this.value);
        }

        try {
            return Integer.parseInt(rawInt);
        } catch (NumberFormatException e) {
            VendingMachine.LOG.warn("Couldn't fetch integer correctly for: " + getUnlocalized(), e);
            return Integer.parseInt(this.value);
        }
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
