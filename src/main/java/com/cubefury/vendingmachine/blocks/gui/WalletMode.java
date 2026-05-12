package com.cubefury.vendingmachine.blocks.gui;

import static com.cubefury.vendingmachine.gui.GuiTextures.WALLET_PERSONAL;
import static com.cubefury.vendingmachine.gui.GuiTextures.WALLET_TEAM;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;

public enum WalletMode {

    PERSONAL("personal", WALLET_PERSONAL),
    TEAM("team", WALLET_TEAM);

    private final String mode;
    private final Icon texture;

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
