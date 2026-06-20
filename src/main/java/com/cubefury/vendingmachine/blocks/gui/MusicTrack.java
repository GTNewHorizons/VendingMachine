package com.cubefury.vendingmachine.blocks.gui;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.gui.GuiTextures;

public enum MusicTrack {

    NONE("none", null, GuiTextures.AUDIO_OFF),
    LUNCH_BREAK("lunch_break", new ResourceLocation(VendingMachine.MODID, "track.lunch_break"), GuiTextures.AUDIO_ON);

    private final String name;
    private final ResourceLocation sound;
    private final Icon texture;

    MusicTrack(String name, @Nullable ResourceLocation sound, UITexture texture) {
        this.name = name;
        this.sound = sound;
        this.texture = texture.asIcon();
    }

    public String getLocalizedName() {
        return IKey.lang("vendingmachine.gui.display_track_" + this.name)
            .toString();
    }

    public ResourceLocation getSoundLoc() {
        return sound;
    }

    public Icon getTexture() {
        return this.texture;
    }

}
