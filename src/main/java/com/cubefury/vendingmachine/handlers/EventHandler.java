package com.cubefury.vendingmachine.handlers;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import org.apache.commons.lang3.Validate;

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
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class EventHandler {

    public static final EventHandler INSTANCE = new EventHandler();

    private static final ArrayDeque<FutureTask> serverTasks = new ArrayDeque<>();
    private static Thread serverThread = null;
    private final ArrayDeque<EntityPlayerMP> opQueue = new ArrayDeque<>();
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
                || !(event.player instanceof EntityPlayerMP)
        ) return;

        EntityPlayerMP mpPlayer = (EntityPlayerMP) event.player;

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

    @SuppressWarnings("UnstableApiUsage")
    public static <T> ListenableFuture<T> scheduleServerTask(Callable<T> task) {
        Validate.notNull(task);

        if (Thread.currentThread() != serverThread) {
            ListenableFutureTask<T> lft = ListenableFutureTask.create(task);

            synchronized (serverTasks) {
                serverTasks.add(lft);
                return lft;
            }
        } else {
            try {
                return Futures.immediateFuture(task.call());
            } catch (Exception exception) {
                return Futures.immediateFailedCheckedFuture(exception);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (serverThread == null) serverThread = Thread.currentThread();

            synchronized (serverTasks) {
                while (!serverTasks.isEmpty()) serverTasks.poll()
                    .run();
            }

            return;
        }

        MinecraftServer server = MinecraftServer.getServer();

        if (!server.isDedicatedServer()) {
            boolean tmp = openToLAN;
            openToLAN = server instanceof IntegratedServer && ((IntegratedServer) server).getPublic();
            if (openToLAN && !tmp) opQueue.addAll(server.getConfigurationManager().playerEntityList);
        } else if (!openToLAN) {
            openToLAN = true;
        }

        while (!opQueue.isEmpty()) {
            EntityPlayerMP playerMP = opQueue.poll();
            if (playerMP != null) {
                NameCache.INSTANCE.updateName(playerMP);
            }
        }

        for (EntityPlayerMP player : server.getConfigurationManager().playerEntityList) {
            livingPlayerTick(player);
        }
    }

    private void livingPlayerTick(@Nonnull EntityPlayerMP player) {
        if (
            !VMConfig.vendingMachineSettings.restock_notifications_enabled
                || player.ticksExisted % VMConfig.vendingMachineSettings.restock_notifications_interval != 0
        ) {
            return;
        }

        TradeDatabase.INSTANCE.sendTradeNotifications(player);
    }

    private void terminateVendingSession(@Nonnull EntityPlayer player) {
        if (VendingMachine.proxy.isClient()) {
            return;
        }
        if (
            !(player.openContainer instanceof ModularContainer
                && ((ModularContainer) player.openContainer).getGuiData() instanceof PosGuiData)
        ) {
            return;
        }
        TileEntity te = ((PosGuiData) ((ModularContainer) player.openContainer).getGuiData()).getTileEntity();

        if (
            te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine
        ) {
            VendingMachine.LOG.info("Force terminating VM session for {}", player);
            ((MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity()).resetCurrentUser(player);
            SaveLoadHandler.INSTANCE
                .writeTradeState(Collections.singleton(NameCache.INSTANCE.getUUIDFromPlayer(player)));
        }
    }

}
