package com.cubefury.vendingmachine.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.network.handlers.NetCoinDeposit;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.FavouritesTracker;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class ClientEventHandler {

    private boolean pendingWorldInit = false;

    /**
     * Sneak + right-click on a vending machine controller while holding coins deposits the whole
     * held stack into the wallet, without opening the GUI. Sneaking with an item in hand skips the
     * usual block activation in 1.7.10, so this is caught here instead of in onRightclick. Only the
     * client knows the selected wallet mode, so it sends it to the server; the packet handler does
     * the actual deposit.
     */
    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        World world = event.world;
        if (world == null || !world.isRemote) return;
        EntityPlayer player = event.entityPlayer;
        if (player == null || !player.isSneaking()) return;
        if (CurrencyItem.fromItemStack(player.getHeldItem()) == null) return;
        TileEntity te = world.getTileEntity(event.x, event.y, event.z);
        if (
            !(te instanceof IGregTechTileEntity)
                || !(((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine)
        ) return;
        NetCoinDeposit.sendDeposit(world, event.x, event.y, event.z, VMConfig.gui.wallet_mode);
        event.setCanceled(true);
    }

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
