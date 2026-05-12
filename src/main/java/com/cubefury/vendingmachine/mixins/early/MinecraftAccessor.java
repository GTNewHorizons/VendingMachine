package com.cubefury.vendingmachine.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("mcMusicTicker")
    MusicTicker vendingmachine$getMusicTicker();

}
