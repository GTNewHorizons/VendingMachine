package com.cubefury.vendingmachine.util;

import net.minecraft.client.audio.PositionedSound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.client.ISeekingSound;

@SideOnly(Side.CLIENT)
public class VMSound extends PositionedSound implements ISeekingSound {

    public final long seekMs;

    public VMSound(ResourceLocation soundResource, long seekMs) {
        super(soundResource);
        this.seekMs = seekMs;

        this.volume = 1.0f;
        this.field_147663_c = 1.0f;
        this.xPosF = 0.0f;
        this.yPosF = 0.0f;
        this.zPosF = 0.0f;
        this.repeat = false;
        this.field_147665_h = 0;
        this.field_147666_i = AttenuationType.NONE;
    }

    @Override
    public long getSeekMillisecondOffset() {
        return this.seekMs;
    }
}
