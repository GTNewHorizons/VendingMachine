package com.cubefury.vendingmachine.network.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.handlers.NotificationHandler;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetTradeNotification {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:trade_notification");

    public static void registerHandler() {
        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetTradeNotification::onClient);
        }
    }

    public static void sendNotification(@Nonnull EntityPlayerMP player, List<UUID> tradeGroups) {
        NBTTagCompound payload = new NBTTagCompound();
        NBTTagList notifications = new NBTTagList();
        for (UUID tgId : tradeGroups) {
            notifications.appendTag(NBTConverter.UuidValueType.TRADEGROUP.writeId(tgId));
        }
        payload.setTag("notifications", notifications);
        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        if (!VMConfig.vendingMachineSettings.restock_notifications_enabled) {
            return;
        }

        List<UUID> tradeNotifications = new ArrayList<>();

        NBTTagList notifNbt = message.getTagList("notifications", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < notifNbt.tagCount(); i++) {
            tradeNotifications.add(NBTConverter.UuidValueType.TRADEGROUP.readId(notifNbt.getCompoundTagAt(i)));
        }

        NotificationHandler.INSTANCE.displayNotification(tradeNotifications);
    }
}
