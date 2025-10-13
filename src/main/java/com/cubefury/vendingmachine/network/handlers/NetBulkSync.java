package com.cubefury.vendingmachine.network.handlers;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.events.MarkDirtyNamesEvent;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetBulkSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:bulk_sync");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetBulkSync::onServer);

        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetBulkSync::onClient);
        }
    }

    public static void sendReset(@Nullable EntityPlayerMP player, boolean reset, boolean respond) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setBoolean("reset", reset);
        payload.setBoolean("respond", respond);

        if (player == null) // Don't use this on a large server unless absolutely necessary!
        {
            PacketSender.INSTANCE.sendToAll(new UnserializedPacket(ID_NAME, payload));
        } else {
            PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
        }
    }

    public static void sendSync(@Nonnull EntityPlayerMP player) {
        NameCache.INSTANCE.updateName(player);
        MinecraftForge.EVENT_BUS.post(new MarkDirtyNamesEvent());

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);

        NetNameSync.sendNames(new EntityPlayerMP[] { player }, new UUID[] { playerId }, null);
        NetTradeDbSync.sendDatabase(player, false);
    }

    private static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message) {
        sendSync(message.second());
    }

    @SideOnly(Side.CLIENT)
    private static void onClient(NBTTagCompound message) {
        if (
            message.getBoolean("reset") && !Minecraft.getMinecraft()
                .isIntegratedServerRunning()
        ) {
            // Only run this if it is a MP client connecting to a server
            SaveLoadHandler.INSTANCE.unloadAll();
        }

        if (message.getBoolean("respond")) {
            PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, new NBTTagCompound()));
        }
    }
}
