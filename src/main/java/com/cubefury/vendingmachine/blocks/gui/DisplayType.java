package com.cubefury.vendingmachine.blocks.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;

import static com.cubefury.vendingmachine.gui.GuiTextures.MODE_LIST;
import static com.cubefury.vendingmachine.gui.GuiTextures.MODE_TILE;

public enum DisplayType {

    TILE("tile", MODE_TILE),
    LIST("list", MODE_LIST);

    private final String type;
    private final Icon texture;

    DisplayType(String type, UITexture texture) {
        this.type = type;
        this.texture = texture.asIcon();
    }

    public String getLocalizedName() {
        return IKey.lang("vendingmachine.gui.display_mode_" + this.type)
            .toString();
    }

    public Icon getTexture() {
        return this.texture;
    }
}
