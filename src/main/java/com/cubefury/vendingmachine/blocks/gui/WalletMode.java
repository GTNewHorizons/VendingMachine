package com.cubefury.vendingmachine.blocks.gui;

import static com.cubefury.vendingmachine.gui.GuiTextures.SORT_ALPHABET;
import static com.cubefury.vendingmachine.gui.GuiTextures.SORT_SMART;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;

public enum WalletMode {

    PERSONAL("personal", SORT_SMART),
    TEAM("team", SORT_ALPHABET);

    private String mode;
    private Icon texture;

    WalletMode(String mode, UITexture texture) {
        this.mode = mode;
        this.texture = texture.asIcon();
    }

    public String getLocalizedName() {
        return IKey.lang("vendingmachine.gui.display_wallet_" + this.mode)
            .toString();
    }

    public Icon getTexture() {
        return this.texture;
    }

}
