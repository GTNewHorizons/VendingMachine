package com.cubefury.vendingmachine.mixins.early;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SoundManager.class)
public interface SoundManagerAccessor {

    @Accessor("sndSystem")
    SoundManager.SoundSystemStarterThread vendingmachine$getSoundSystem();

    @Invoker(value = "getNormalizedVolume")
    float vendingmachine$getNormalizedVolume(ISound sound, SoundPoolEntry entry, SoundCategory category);
}
