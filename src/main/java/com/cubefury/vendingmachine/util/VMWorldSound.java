package com.cubefury.vendingmachine.util;

import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.VMConfig;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VMWorldSound extends PositionedSound {

    public VMWorldSound(ResourceLocation location, int x, int y, int z) {
        super(location);
        this.xPosF = x + 0.5F;
        this.yPosF = y + 0.5F;
        this.zPosF = z + 0.5F;
        this.repeat = true;
        // repeatDelay defaults to 0 and attenuationType defaults to LINEAR in PositionedSound.
        this.volume = VMConfig.music.music_volume;
        this.field_147663_c = 1.0F; // pitch
    }
}
