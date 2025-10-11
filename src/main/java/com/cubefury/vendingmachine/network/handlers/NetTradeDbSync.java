package com.cubefury.vendingmachine.network.handlers;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.util.ThreadedIO;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetTradeDbSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:tradedb_sync");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetTradeDbSync::onServer);

        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetTradeDbSync::onClient);
        }
    }

    public static void sendDatabase(@Nullable EntityPlayerMP player, boolean merge) {
        // This can take a while, so we offload it to another thread
        ThreadedIO.INSTANCE.enqueue(() -> {
            NBTTagCompound data = TradeDatabase.INSTANCE.writeToNBT(new NBTTagCompound());
            NBTTagCompound payload = new NBTTagCompound();
            payload.setTag("data", data);
            payload.setBoolean("merge", merge);
            if (player == null) {
                PacketSender.INSTANCE.sendToAll(new UnserializedPacket(ID_NAME, payload));
            } else {
                PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
            }
        });
    }

    // request trade db sync
    @SideOnly(Side.CLIENT)
    public static void sendRequest(boolean merge) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setBoolean("merge", merge);
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    public static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message) {
        sendDatabase(
            message.second(),
            message.first()
                .getBoolean("merge"));
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        if (
            Minecraft.getMinecraft()
                .isIntegratedServerRunning()
        ) {
            return;
        }
        TradeDatabase.INSTANCE.readFromNBT(message.getCompoundTag("data"), message.getBoolean("merge"), false);
    }
}
