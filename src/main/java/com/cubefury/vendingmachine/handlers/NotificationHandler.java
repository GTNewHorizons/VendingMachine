package com.cubefury.vendingmachine.handlers;

import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NotificationHandler {

    public static NotificationHandler INSTANCE = new NotificationHandler();

    private NotificationHandler() {}

    public void displayNotification(List<UUID> tradeGroups) {
        String refreshedTrades = "a";
        Minecraft.getMinecraft().thePlayer.addChatComponentMessage(
            new ChatComponentTranslation(
                "vendingmachine.chat.trade_restock",
                EnumChatFormatting.YELLOW + refreshedTrades));
    }
}
