package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cubefury.vendingmachine.api.trade.ICondition;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.network.handlers.NetTradeNotification;
import com.cubefury.vendingmachine.storage.NameCache;

// Sync the following objects to the client every GUI refresh cycle:
// Available trades, No-condition trades and currency
// Everything else is stored server-side
public class TradeManager {

    public static TradeManager INSTANCE = new TradeManager();

    // availableTrades and noCondition trades technically have information that
    // is extractable from tradegroupStates, but querying that every second
    // for gui display is more expensive so we cache it here
    // only the available trades + currency data is sent to player
    private final Map<UUID, Set<UUID>> availableTrades = new HashMap<>();

    // Map for tradegroup id -> player trade states and unlock status
    public final Map<UUID, TradeGroupState> tradeGroupStates = new HashMap<>();

    // Map for player id -> currency data
    public final Map<UUID, Map<CurrencyType, Integer>> playerCurrency = new HashMap<>();

    // Map for player id -> trades with pending refresh notifications
    public final Map<UUID, Set<UUID>> notificationQueue = new HashMap<>();

    // For writeback to file in original format, to prevent data loss
    private final Map<UUID, List<NBTTagCompound>> invalidCurrency = new HashMap<>();

    public final List<TradeItemDisplay> tradeData = new ArrayList<>();

    public boolean hasCurrencyUpdate = false;

    private TradeManager() {}

    public void addTradeGroup(@Nonnull UUID player, UUID tg) {
        availableTrades.putIfAbsent(player, new HashSet<>());
        availableTrades.get(player).add(tg);
    }

    public void removeTradeGroup(UUID player, UUID tg) {
        if (availableTrades.containsKey(player)) {
            availableTrades.get(player).remove(tg);
        }
    }

    public void addSatisfiedCondition(TradeGroup tradeGroup, @Nonnull UUID player, ICondition c) {
        UUID tradeGroupId = tradeGroup.getId();
        tradeGroupStates.putIfAbsent(tradeGroupId, new TradeGroupState(tradeGroup));
        tradeGroupStates.get(tradeGroupId).addConditionSatisfied(player, c);

        updateAvailableTrades(tradeGroupId, player);
    }

    public void removeSatisfiedCondition(TradeGroup tradeGroup, @Nullable UUID player, ICondition c) {
        UUID tradeGroupId = tradeGroup.getId();
        tradeGroupStates.putIfAbsent(tradeGroupId, new TradeGroupState(tradeGroup));
        tradeGroupStates.get(tradeGroupId).removeConditionSatisfied(player, c);

        if (player == null) {
            for (UUID p : tradeGroupStates.get(tradeGroupId).getPlayersWithConditionData()) {
                updateAvailableTrades(tradeGroupId, p);
            }
        } else {
            updateAvailableTrades(tradeGroupId, player);
        }
    }

    private void updateAvailableTrades(UUID tradeGroupId, @Nullable UUID player) {
        if (tradeGroupStates.get(tradeGroupId).satisfiesTrade(player)) {
            TradeManager.INSTANCE.addTradeGroup(player, tradeGroupId);
        } else {
            TradeManager.INSTANCE.removeTradeGroup(player, tradeGroupId);
        }
    }

    public List<TradeGroup> getAvailableTradeGroups(UUID player) {
        synchronized (availableTrades) {
            availableTrades.computeIfAbsent(player, k -> new HashSet<>());
            ArrayList<TradeGroup> tradeList = new ArrayList<>();
            for (UUID tgId : availableTrades.get(player)) {
                tradeList.add(TradeDatabase.INSTANCE.getTradeGroupFromId(tgId));
            }
            tradeList.addAll(TradeDatabase.INSTANCE.noConditionTrades);
            return tradeList;
        }
    }

    public void populateCurrencyFromNBT(NBTTagCompound nbt, UUID player, boolean merge) {
        NBTTagList tagList = nbt.getTagList("playerCurrency", Constants.NBT.TAG_COMPOUND);
        if (!merge) {
            this.clearCurrency(player);
        }
        this.playerCurrency.computeIfAbsent(player, k -> new HashMap<>());
        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound currencyEntry = tagList.getCompoundTagAt(i);
            CurrencyType type = CurrencyType.getTypeFromId(currencyEntry.getString("currency"));
            if (type == null) {
                VendingMachine.LOG.warn("Unknown currency type found: {}", currencyEntry.getString("currency"));
                this.invalidCurrency.computeIfAbsent(player, k -> new ArrayList<>());
                this.invalidCurrency.get(player)
                    .add(currencyEntry);
                continue;
            }
            int amount = currencyEntry.getInteger("amount");
            this.playerCurrency.get(player)
                .computeIfAbsent(type, k -> 0);
            this.playerCurrency.get(player)
                .put(
                    type,
                    amount + (merge ? this.playerCurrency.get(player)
                        .get(type) : 0));
        }
        this.hasCurrencyUpdate = true;
    }

    public NBTTagCompound writeCurrencyToNBT(NBTTagCompound nbt, @Nonnull UUID player) {
        if (this.playerCurrency.get(player) == null) {
            return nbt;
        }
        NBTTagList nbtCurrencyList = new NBTTagList();
        for (Map.Entry<CurrencyType, Integer> entry : this.playerCurrency.get(player)
            .entrySet()) {
            NBTTagCompound currencyEntry = new NBTTagCompound();
            currencyEntry.setString("currency", entry.getKey().id);
            currencyEntry.setInteger("amount", entry.getValue());
            nbtCurrencyList.appendTag(currencyEntry);
        }

        if (this.invalidCurrency.get(player) != null) {
            for (NBTTagCompound tag : this.invalidCurrency.get(player)) {
                nbtCurrencyList.appendTag(tag);
            }
        }
        nbt.setTag("playerCurrency", nbtCurrencyList);
        return nbt;
    }

    public void resetCurrency(UUID playerId, CurrencyType type) {
        this.playerCurrency.computeIfAbsent(playerId, k -> new HashMap<>());
        if (type == null) {
            this.playerCurrency.get(playerId)
                .clear();
        } else {
            this.playerCurrency.get(playerId)
                .put(type, 0);
        }
        this.hasCurrencyUpdate = true;
    }

    public void addCurrency(UUID playerId, CurrencyItem mapped) {
        if (mapped != null) {
            this.playerCurrency.computeIfAbsent(playerId, k -> new HashMap<>());
            this.playerCurrency.get(playerId)
                .computeIfAbsent(mapped.type, k -> 0);
            this.playerCurrency.get(playerId)
                .put(
                    mapped.type,
                    this.playerCurrency.get(playerId)
                        .get(mapped.type) + mapped.value);
        }
        this.hasCurrencyUpdate = true;
    }

    public void clearCurrency(UUID player) {
        if (player == null) {
            this.playerCurrency.clear();
            this.invalidCurrency.clear();
        } else {
            this.playerCurrency.remove(player);
            this.invalidCurrency.remove(player);
        }
    }

    public void clearNotificationQueue(UUID playerOrNull) {
        if (playerOrNull == null) {
            this.notificationQueue.clear();
        } else {
            this.notificationQueue.remove(playerOrNull);
        }
    }

    public void addNotification(UUID player, TradeGroup tg) {
        this.notificationQueue.putIfAbsent(player, new HashSet<>());
        this.notificationQueue.get(player)
            .add(tg.getId());
    }

    public void sendTradeNotifications(EntityPlayerMP player) {
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);
        Set<UUID> notifGroups = this.notificationQueue.get(playerId);
        if (notifGroups == null) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();

        List<UUID> notifList = new ArrayList<>();
        for (UUID tgId : notifGroups) {
            TradeGroup tradeGroup = TradeDatabase.INSTANCE.getTradeGroupFromId(tgId);
            if (tradeGroup == null) {
                continue;
            }
            TradeHistory th = tradeGroup.getTradeState(playerId);
            if ((currentTimestamp - th.lastTrade) / 1000 > tradeGroup.cooldown) {
                notifList.add(tgId);
                th.setNotified();
            }
        }

        if (!notifList.isEmpty()) {
            NetTradeNotification.sendNotification(player, notifList);
            SaveLoadHandler.INSTANCE.writeTradeState(Collections.singleton(playerId));
        }

        for (UUID tg : notifList) {
            notifGroups.remove(tg);
        }
    }
}
