package com.cubefury.vendingmachine.handlers;

import java.util.Collections;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.events.MarkDirtyDbEvent;
import com.cubefury.vendingmachine.events.MarkDirtyNamesEvent;
import com.cubefury.vendingmachine.network.handlers.NetBulkSync;
import com.cubefury.vendingmachine.network.handlers.NetTradeDbSync;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.TeamHelper;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class EventHandler {

    private boolean openToLAN = false;

    @SubscribeEvent
    public void onMarkDirtyDb(MarkDirtyDbEvent event) {
        SaveLoadHandler.INSTANCE.writeDatabase();
        NetTradeDbSync.sendDatabase(null, false);
    }

    @SubscribeEvent
    public void onMarkDirtyNames(MarkDirtyNamesEvent event) {
        SaveLoadHandler.INSTANCE.writeNames();
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (
            event.player.worldObj.isRemote || MinecraftServer.getServer() == null
                || !(event.player instanceof EntityPlayerMP mpPlayer)
        ) return;

        if (
            VendingMachine.proxy.isClient() && !MinecraftServer.getServer()
                .isDedicatedServer()
                && MinecraftServer.getServer()
                    .getServerOwner()
                    .equals(
                        event.player.getGameProfile()
                            .getName())
        ) {
            NameCache.INSTANCE.updateName(mpPlayer);
            return;
        }

        NetBulkSync.sendReset(mpPlayer, true, true);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        terminateVendingSession(event.player);
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entityLiving instanceof EntityPlayer) {
            terminateVendingSession((EntityPlayer) event.entityLiving);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        MinecraftServer server = MinecraftServer.getServer();

        if (!server.isDedicatedServer()) {
            boolean tmp = openToLAN;
            openToLAN = server instanceof IntegratedServer iServer && iServer.getPublic();
            if (openToLAN && !tmp) {
                server.getConfigurationManager().playerEntityList.forEach(p -> {
                    if (p != null) {
                        NameCache.INSTANCE.updateName(p);
                    }
                });
            }
        }

        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            livingPlayerTick(player);
        }
    }

    private static void livingPlayerTick(@Nonnull EntityPlayerMP player) {
        if (
            !VMConfig.vendingMachineSettings.restock_notifications_enabled || player.ticksExisted == 0
                || player.ticksExisted % VMConfig.vendingMachineSettings.restock_notifications_interval != 0
        ) {
            return;
        }

        TradeManager.INSTANCE.sendTradeNotifications(player);
    }

    private static void terminateVendingSession(@Nonnull EntityPlayer player) {
        if (VendingMachine.proxy.isClient()) {
            return;
        }
        if (
            player.openContainer instanceof ModularContainer container
                && container.getGuiData() instanceof PosGuiData guiData
                && guiData.getTileEntity() instanceof IGregTechTileEntity gte
                && gte.getMetaTileEntity() instanceof MTEVendingMachine vm
        ) {
            VendingMachine.LOG.info("Force terminating VM session for {}", player);
            vm.resetCurrentUser(player);
            UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);
            SaveLoadHandler.INSTANCE.writeTradeState(Collections.singleton(playerId));
            UUID teamId = TeamHelper.GetTeamUUID(playerId);
            if (teamId != null) {
                SaveLoadHandler.INSTANCE.writeTradeState(Collections.singleton(teamId));
            }
        }
    }

}
