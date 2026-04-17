package com.cubefury.vendingmachine.util;

import net.minecraft.client.resources.I18n;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class Translator {

    public static String translate(String text, Object... args) {
        String out = I18n.format(text, args);
        if (out.startsWith("Format error: ")) {
            return text; // TODO: Find a more reliable way of detecting translation failure
        }
        return out;
    }

}
