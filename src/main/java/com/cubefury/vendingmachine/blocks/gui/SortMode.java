package com.cubefury.vendingmachine.blocks.gui;

import static com.cubefury.vendingmachine.gui.GuiTextures.SORT_ALPHABET;
import static com.cubefury.vendingmachine.gui.GuiTextures.SORT_SMART;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;

public enum SortMode {

    SMART("smart", SORT_SMART),
    ALPHABET("alphabet", SORT_ALPHABET);

    private String mode;
    private Icon texture;

    SortMode(String mode, UITexture texture) {
        this.mode = mode;
        this.texture = texture.asIcon();
    }

    public String getLocalizedName() {
        return IKey.lang("vendingmachine.gui.display_sort_" + this.mode)
            .toString();
    }

    public Icon getTexture() {
        return this.texture;
    }
}
