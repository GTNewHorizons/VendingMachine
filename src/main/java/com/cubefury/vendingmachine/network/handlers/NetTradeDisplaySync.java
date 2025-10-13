package com.cubefury.vendingmachine.network.handlers;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.blocks.gui.MTEVendingMachineGui;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.NBTConverter;
import com.cubefury.vendingmachine.util.Translator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetTradeDisplaySync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:availabletrade_sync");

    public static void registerHandler() {

        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetTradeDisplaySync::onClient);
        }
    }

    private static class Tradable {

        public UUID tgID;
        public int tradeGroupOrder;
        public long cooldown;
        public boolean enabled;
        public boolean tradableNow;

        public Tradable(UUID tgID, int tradeGroupOrder, long cooldown, boolean enabled, boolean tradableNow) {
            this.tgID = tgID;
            this.tradeGroupOrder = tradeGroupOrder;
            this.cooldown = cooldown;
            this.enabled = enabled;
            this.tradableNow = tradableNow;
        }

        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTConverter.UuidValueType.TRADEGROUP.writeId(this.tgID, nbt);
            nbt.setInteger("order", this.tradeGroupOrder);
            nbt.setLong("cooldown", this.cooldown);
            nbt.setBoolean("enabled", this.enabled);
            nbt.setBoolean("tradableNow", this.tradableNow);

            return nbt;
        }

        public static Tradable readFromNBT(NBTTagCompound nbt) {
            return new Tradable(
                NBTConverter.UuidValueType.TRADEGROUP.readId(nbt),
                nbt.getInteger("order"),
                nbt.getLong("cooldown"),
                nbt.getBoolean("enabled"),
                nbt.getBoolean("tradableNow"));
        }

        public TradeItemDisplay formatItemDisplay() {
            TradeGroup tg = TradeDatabase.INSTANCE.getTradeGroupFromId(this.tgID);
            Trade t = tg.getTrades()
                .get(this.tradeGroupOrder);
            return new TradeItemDisplay(
                t.fromCurrency,
                t.fromItems,
                t.toItems,
                t.getDisplayItem(),
                this.tgID,
                this.tradeGroupOrder,
                this.cooldown,
                convertCooldownText(this.cooldown),
                this.cooldown > 0,
                this.enabled,
                this.tradableNow);
        }

        public static String convertCooldownText(long cd) {
            if (cd < 60) {
                return cd + Translator.translate("vendingmachine.gui.cooldown_display.second");
            }
            if (cd < 3600) {
                return cd / 60 + Translator.translate("vendingmachine.gui.cooldown_display.minute");
            }
            if (cd < 86400) {
                return cd / 3600 + Translator.translate("vendingmachine.gui.cooldown_display.hour");
            }
            return cd / 86400 + Translator.translate("vendingmachine.gui.cooldown_display.day"); // doom.jpg
        }
    }

    public static void syncTradesToClient(@Nonnull EntityPlayerMP player, MTEVendingMachine base) {
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);
        List<TradeGroup> availableGroups = TradeManager.INSTANCE.getAvailableTradeGroups(playerId);
        base.refreshInputSlotCache();

        long currentTimestamp = System.currentTimeMillis();

        NBTTagCompound payload = new NBTTagCompound();
        NBTTagList trades = new NBTTagList();
        for (TradeGroup tg : availableGroups) {
            long lastTradeTime = tg.getTradeState(playerId).lastTrade;
            long tradeCount = tg.getTradeState(playerId).tradeCount;

            long cooldownRemaining;
            if (tg.cooldown != -1 && lastTradeTime != -1 && (currentTimestamp - lastTradeTime) / 1000 < tg.cooldown) {
                cooldownRemaining = tg.cooldown - (currentTimestamp - lastTradeTime) / 1000;
            } else {
                cooldownRemaining = -1;
            }
            boolean enabled = tg.maxTrades == -1 || tradeCount < tg.maxTrades;

            for (int i = 0; i < tg.getTrades()
                .size(); i++) {
                Trade trade = tg.getTrades()
                    .get(i);
                boolean tradableNow = base.inputItemsSatisfied(trade.fromItems)
                    && base.inputCurrencySatisfied(trade.fromCurrency, playerId);
                trades.appendTag(
                    new Tradable(tg.getId(), i, cooldownRemaining, enabled, tradableNow)
                        .writeToNBT(new NBTTagCompound()));
            }
            payload.setTag("trades", trades);
        }
        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);

    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        List<TradeItemDisplay> tradeData = TradeManager.INSTANCE.tradeData;
        tradeData.clear();

        NBTTagList trades = message.getTagList("trades", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < trades.tagCount(); i++) {
            tradeData.add(
                Tradable.readFromNBT(trades.getCompoundTagAt(i))
                    .formatItemDisplay());
        }
        MTEVendingMachineGui.setForceRefresh();
    }
}
