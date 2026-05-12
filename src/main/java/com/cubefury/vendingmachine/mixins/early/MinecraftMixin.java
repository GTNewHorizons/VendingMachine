package com.cubefury.vendingmachine.mixins.early;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cubefury.vendingmachine.util.VMMusicManager;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/MusicTicker;update()V"))
    private static void preMusicTick(CallbackInfo ci) {
        VMMusicManager.tickingMusic = true;
    }

    @Inject(
        method = "runTick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/MusicTicker;update()V", shift = Shift.AFTER))
    private static void postMusicTick(CallbackInfo ci) {
        VMMusicManager.tickingMusic = false;
    }

}
