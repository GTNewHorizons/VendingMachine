package com.cubefury.vendingmachine.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ISound.AttenuationType;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.mixins.early.SoundHandlerAccessor;
import com.cubefury.vendingmachine.mixins.early.SoundManagerAccessor;
import com.gtnewhorizon.gtnhlib.eventbus.EventBusSubscriber;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.relauncher.Side;
import paulscode.sound.SoundSystem;

@EventBusSubscriber(side = Side.CLIENT)
public final class VMMusicManager {

    public static final ResourceLocation VM_MUSIC = new ResourceLocation(VendingMachine.MODID, "vendingmachine.music");
    private static final int FADE_TIME = 1000;

    private static AudioContext gameMusic;
    private static AudioContext vmMusic;

    private static boolean inVm;
    private static boolean running = false;
    private static long musicStartTime;

    public static void setCurrentGameMusic(AudioContext audioContext) {
        gameMusic = audioContext;
    }

    public static void setCurrentVendingMusic(AudioContext audioContext) {
        vmMusic = audioContext;
    }

    public static void startVendingMachineMusic() {
        inVm = true;
        running = true;
        if (vmMusic == null) {
            musicStartTime = System.currentTimeMillis();
            SoundHandler soundHandler = Minecraft.getMinecraft()
                .getSoundHandler();
            soundHandler
                .playSound(new PositionedSoundRecord(VM_MUSIC, 0.5f, 1f, true, 0, AttenuationType.NONE, 0, 0, 0));
        } else {
            musicStartTime = Math.min(FADE_TIME - (System.currentTimeMillis() - musicStartTime), FADE_TIME);
        }
    }

    public static void stopVendingMachineMusic() {
        inVm = false;
        running = true;
        musicStartTime = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRender(RenderTickEvent e) {
        if (e.phase == Phase.END) return;
        if (!running) return;
        SoundSystem sys = getSoundManager().vendingmachine$getSoundSystem();
        int msPassed = (int) (System.currentTimeMillis() - musicStartTime);
        float volume = Math.min(msPassed / (float) FADE_TIME, 1);
        if (volume == 1) {
            running = false;
        }
        if (inVm) volume = 1 - volume;
        if (gameMusic != null && sys.playing(gameMusic.id)) {
            sys.setVolume(gameMusic.id, volume * gameMusic.getVolume());
        }
        if (vmMusic != null && sys.playing(vmMusic.id)) {
            sys.setVolume(vmMusic.id, (1 - volume) * vmMusic.getVolume());
        }

        if (!inVm && !running) {
            reset();
        }
    }

    public static boolean inVendingMachine() {
        return inVm;
    }

    public static void reset() {
        SoundHandler soundHandler = Minecraft.getMinecraft()
            .getSoundHandler();
        if (vmMusic != null) {
            soundHandler.stopSound(vmMusic.sound);
        }

        vmMusic = null;
    }

    private static SoundManagerAccessor getSoundManager() {
        return (SoundManagerAccessor) ((SoundHandlerAccessor) Minecraft.getMinecraft()
            .getSoundHandler()).vendingmachine$getSoundManager();
    }

    public static class AudioContext {

        public final String id;
        public final ISound sound;
        public final SoundCategory category;
        public final SoundPoolEntry soundPoolEntry;

        public AudioContext(String id, ISound sound, SoundCategory category, SoundPoolEntry soundPoolEntry) {
            this.id = id;
            this.sound = sound;
            this.category = category;
            this.soundPoolEntry = soundPoolEntry;
        }

        public float getVolume() {
            return getSoundManager().vendingmachine$getNormalizedVolume(sound, soundPoolEntry, category);
        }
    }

}
