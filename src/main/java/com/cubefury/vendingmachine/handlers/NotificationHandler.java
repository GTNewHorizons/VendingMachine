package com.cubefury.vendingmachine.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;
import com.cubefury.vendingmachine.util.BigItemStack;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@SideOnly(Side.CLIENT)
public class NotificationHandler {

    private static final int MAX_NOTIFICATION_ITEMS = 4;
    public static NotificationHandler INSTANCE = new NotificationHandler();

    private NotificationHandler() {}

    public void displayNotification(List<UUID> tradeGroups) {
        Minecraft mc = Minecraft.getMinecraft();

        // If player is already accessing a vending machine, don't send notifications
        if (
            mc.thePlayer.openContainer instanceof ModularContainer container
                && container.getGuiData() instanceof PosGuiData guiData
        ) {
            TileEntity te = guiData.getTileEntity();
            if (te instanceof IGregTechTileEntity gte && gte.getMetaTileEntity() instanceof MTEVendingMachine) {
                return;
            }
        }

        List<String> refreshedTrades = new ArrayList<>();
        for (UUID tgid : tradeGroups) {
            TradeGroup tradeGroup = TradeDatabase.INSTANCE.getTradeGroupFromId(tgid);
            if (tradeGroup == null) {
                continue;
            }
            for (Trade trade : tradeGroup.getTrades()) {
                for (BigItemStack stack : trade.toItems) {
                    refreshedTrades.add(
                        stack.getBaseStack()
                            .getDisplayName());
                }
            }
        }

        int count = refreshedTrades.size();
        String items_output = "";
        if (count == 0) {
            return;
        } else if (count > MAX_NOTIFICATION_ITEMS) {
            refreshedTrades = refreshedTrades.subList(0, MAX_NOTIFICATION_ITEMS);
            items_output += String.join(", ", refreshedTrades) + ", ... [+" + (count - MAX_NOTIFICATION_ITEMS) + "]";
        } else {
            items_output += String.join(", ", refreshedTrades);
        }

        mc.thePlayer.addChatComponentMessage(
            new ChatComponentTranslation(
                "vendingmachine.chat.trade_restock",
                EnumChatFormatting.YELLOW + items_output));
        mc.thePlayer.playSound("random.orb", 0.2F, 1.8F);
    }
}
