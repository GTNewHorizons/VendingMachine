package com.cubefury.vendingmachine.handlers;

import net.minecraft.client.Minecraft;

import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.FavouritesTracker;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientEventHandler {

    public static final ClientEventHandler INSTANCE = new ClientEventHandler();
    private static boolean pendingWorldInit = false;

    @SubscribeEvent
    public void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pendingWorldInit = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (pendingWorldInit) {
            Minecraft mc = Minecraft.getMinecraft();

            if (mc.theWorld != null) {
                SaveLoadHandler.INSTANCE.readFavourites(
                    NameCache.INSTANCE.getUUIDFromPlayer(mc.thePlayer),
                    FavouritesTracker.INSTANCE.computeWorldKey());
                pendingWorldInit = false;
            }
        }
    }
}
