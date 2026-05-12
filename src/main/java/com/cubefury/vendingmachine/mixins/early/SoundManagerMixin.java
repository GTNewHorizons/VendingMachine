package com.cubefury.vendingmachine.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.util.VMMusicManager;
import com.cubefury.vendingmachine.util.VMMusicManager.AudioContext;
import com.llamalad7.mixinextras.sugar.Local;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Inject(
        method = "playSound",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/audio/SoundManager$SoundSystemStarterThread;play(Ljava/lang/String;)V",
            remap = false))
    private static void onPlaySound(ISound sound, CallbackInfo ci, @Local String s, @Local SoundCategory category,
        @Local SoundPoolEntry entry) {
        if (Minecraft.getMinecraft().theWorld == null) return;
        if (sound == getCurrentMusicTrack()) {
            VMMusicManager.setCurrentGameMusic(new AudioContext(s, sound, category, entry));
        } else if (VMMusicManager.inVendingMachine() && sound instanceof PositionedSound ps) {
            if (
                ps.getPositionedSoundLocation()
                    .equals(VMConfig.music.current_track.getSoundLoc())
            ) {
                VMMusicManager.setCurrentVendingMusic(new AudioContext(s, sound, category, entry));
            }
        }
    }

    private static ISound getCurrentMusicTrack() {
        return ((MusicTickerAccessor) ((MinecraftAccessor) Minecraft.getMinecraft()).vendingmachine$getMusicTicker())
            .vendingmachine$getCurrentMusicSound();
    }

}
