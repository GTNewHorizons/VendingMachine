package com.cubefury.vendingmachine.network.handlers;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetCurrencySync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:currency_sync");

    public static void registerHandler() {
        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetCurrencySync::onClient);
        }
    }

    // server side code for sending currency data when player opens gui
    public static void syncCurrencyToClient(@Nonnull EntityPlayerMP player) {
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);

        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("dataType", "currencySync");
        NBTConverter.UuidValueType.PLAYER.writeId(playerId, payload);
        payload.setTag("data", TradeManager.INSTANCE.writeCurrencyToNBT(new NBTTagCompound(), playerId));
        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
    }

    public static void sendPlayerCurrency(@Nonnull EntityPlayerMP player, CurrencyItem currencyItem) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("dataType", "currencyAdd");
        currencyItem.writeToNBT(payload);

        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
    }

    public static void resetPlayerCurrency(@Nonnull EntityPlayerMP player, @Nullable CurrencyType type) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setString("dataType", "currencyReset");
        if (type != null) {
            payload.setString("type", type.id);
        }
        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        // Don't wipe everyone else's data if on LAN, since
        // we receive only the requested player's data
        // In SP and LAN, we'll have this data already anyway
        if (
            Minecraft.getMinecraft()
                .isIntegratedServerRunning()
        ) {
            return;
        }
        String dataType = message.getString("dataType");
        UUID player = NBTConverter.UuidValueType.PLAYER.readId(message);
        switch (dataType) {
            case "currencySync" -> {
                TradeManager.INSTANCE.populateCurrencyFromNBT(
                    message.getCompoundTag("data"),
                    NBTConverter.UuidValueType.PLAYER.readId(message),
                    false);
            }
            case "currencyAdd" -> {
                CurrencyItem currencyItem = CurrencyItem.fromNBT(message.getCompoundTag("currencyItem"));

                TradeManager.INSTANCE.addCurrency(player, currencyItem);
            }
            case "currencyReset" -> TradeManager.INSTANCE.resetCurrency(
                player,
                message.hasKey("type") ? CurrencyType.getTypeFromId(message.getString("type")) : null);
            default -> VendingMachine.LOG.warn("Unknown trade state sync data received: {}", dataType);
        }
    }
}
