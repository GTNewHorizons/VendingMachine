package com.cubefury.vendingmachine.util;

import static com.cubefury.vendingmachine.util.Translator.translate;

import com.cubefury.vendingmachine.VendingMachine;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public enum GuiParams {

    // nei
    display_text_color(0x000000),
    condition_default_color(0x000000),
    condition_satisfied_color(0x55D441),
    condition_unsatisfied_color(0xA87A5E),
    nc_inputs_overlay_color(0xFDD835),

    // trade display
    trade_display_disabled_color(0xBB000000),
    trade_display_list_tradable_now_color(0x883CFF00),
    trade_display_list_untradable_now_color(0x88333333),
    trade_display_list_current_selected_color(0xAA039BE5),
    trade_display_tile_disabled_corner_radius(1),
    trade_display_tile_disabled_corner_radius_segments(1),

    // new line to prevent merge conflicts
    ;

    private final int value;

    GuiParams(final int value) {
        this.value = value;
    }

    public int getColor(boolean hasAlphaChannel) {
        String hex = translate(this.getUnlocalized());
        int bitMask = hasAlphaChannel ? 0xFFFFFFFF : 0xFFFFFF;
        if (hex.equals(this.getUnlocalized())) {
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
}
