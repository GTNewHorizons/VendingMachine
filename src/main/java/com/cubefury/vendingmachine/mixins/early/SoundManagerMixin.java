package com.cubefury.vendingmachine.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.*;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cubefury.vendingmachine.util.VMMusicManager;
import com.cubefury.vendingmachine.util.VMMusicManager.AudioContext;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(
        method = "playSound",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/audio/SoundManager$SoundSystemStarterThread;play(Ljava/lang/String;)V"))
    private static void onPlaySound(ISound sound, CallbackInfo ci, @Local String s, @Local SoundCategory category,
        @Local SoundPoolEntry entry) {
        if (Minecraft.getMinecraft().theWorld == null) return;
        if (category == SoundCategory.MUSIC) {
            VMMusicManager.setCurrentGameMusic(new AudioContext(s, sound, category, entry));
        } else if (VMMusicManager.inVendingMachine() && sound instanceof PositionedSound ps) {
            if (
                ps.getPositionedSoundLocation()
                    .equals(VMMusicManager.VM_MUSIC)
            ) {
                VMMusicManager.setCurrentVendingMusic(new AudioContext(s, sound, category, entry));
            }
        }
    }

}
