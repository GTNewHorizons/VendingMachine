package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;

import com.cubefury.vendingmachine.api.trade.ICondition;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.network.handlers.NetTradeNotification;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.storage.VMTeamData;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.teams.ITeamData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamManager;

// Sync the following objects to the client every GUI refresh cycle:
// tradedata, No-condition trades and currency
// Everything else is stored server-side
public class TradeManager {

    public static TradeManager INSTANCE = new TradeManager();

    // availableTrades and noCondition trades technically have information that
    // is extractable from tradegroupStates, but querying that every second
    // for gui display is more expensive so we cache it here
    private final Map<UUID, Set<UUID>> availableTrades = new HashMap<>();

    // Map for tradegroup id -> player unlock status
    public final Map<UUID, TradeGroupState> tradeGroupStates = new HashMap<>();

    public final List<TradeItemDisplay> tradeData = new ArrayList<>();

    public boolean hasCurrencyUpdate = false;

    private TradeManager() {}

    public void addTradeGroup(@Nonnull UUID player, UUID tg) {
        availableTrades.putIfAbsent(player, new HashSet<>());
        availableTrades.get(player)
            .add(tg);
    }

    public void removeTradeGroup(UUID player, UUID tg) {
        if (availableTrades.containsKey(player)) {
            availableTrades.get(player)
                .remove(tg);
        }
    }

    public @Nullable Wallet getWallet(UUID player, WalletMode walletMode) {
        VMTeamData teamData = getTeamData(player);
        if (teamData == null) return null;
        return teamData.getWallet(player, walletMode);
    }

    public void saveTeamData(UUID player) {
        Team team = TeamManager.getTeamByPlayer(player);
        if (team == null) return;
        team.markDirty();
    }

    private @Nullable VMTeamData getTeamData(UUID player) {
        if (player == null) return null;
        Team team = TeamManager.getTeamByPlayer(player);
        if (team == null) return null;
        ITeamData teamData = team.getData(VMTeamData.ID);
        if (teamData instanceof VMTeamData vmTeamData) {
            return vmTeamData;
        }
        return null;
    }

    public void addSatisfiedCondition(TradeGroup tradeGroup, @Nonnull UUID player, ICondition c) {
        UUID tradeGroupId = tradeGroup.getId();
        tradeGroupStates.putIfAbsent(tradeGroupId, new TradeGroupState(tradeGroup));
        tradeGroupStates.get(tradeGroupId)
            .addConditionSatisfied(player, c);

        updateAvailableTrades(tradeGroupId, player);
    }

    public void removeSatisfiedCondition(TradeGroup tradeGroup, @Nullable UUID player, ICondition c) {
        UUID tradeGroupId = tradeGroup.getId();
        tradeGroupStates.putIfAbsent(tradeGroupId, new TradeGroupState(tradeGroup));
        tradeGroupStates.get(tradeGroupId)
            .removeConditionSatisfied(player, c);

        if (player == null) {
            for (UUID p : tradeGroupStates.get(tradeGroupId)
                .getPlayersWithConditionData()) {
                updateAvailableTrades(tradeGroupId, p);
            }
        } else {
            updateAvailableTrades(tradeGroupId, player);
        }
    }

    private void updateAvailableTrades(UUID tradeGroupId, @Nonnull UUID player) {
        if (
            tradeGroupStates.get(tradeGroupId)
                .satisfiesTrade(player)
        ) {
            addTradeGroup(player, tradeGroupId);
        } else {
            removeTradeGroup(player, tradeGroupId);
        }
    }

    public List<TradeGroup> getAvailableTradeGroups(UUID player) {
        availableTrades.putIfAbsent(player, new HashSet<>());
        ArrayList<TradeGroup> tradeList = new ArrayList<>();
        for (UUID tgId : availableTrades.get(player)) {
            tradeList.add(TradeDatabase.INSTANCE.getTradeGroupFromId(tgId));
        }
        tradeList.addAll(TradeDatabase.INSTANCE.noConditionTrades);
        return tradeList;
    }

    public TradeHistory getTradeState(@Nonnull UUID player, TradeGroup tg) {
        VMTeamData teamData = getTeamData(player);
        if (teamData == null) return new TradeHistory();
        return teamData.getTradeState(tg.getId());
    }

    public void setTradeState(@Nonnull UUID player, TradeGroup tg, TradeHistory history) {
        VMTeamData teamData = getTeamData(player);
        if (teamData == null) return;
        teamData.setTradeHistory(tg.getId(), history);
        saveTeamData(player);
    }

    public boolean canExecuteTrade(@Nonnull UUID player, TradeGroup tg) {
        if (tg == null) {
            return false;
        }
        long currentTimestamp = System.currentTimeMillis();
        TradeHistory history = getTradeState(player, tg);
        long lastTradeTime = history.lastTrade;
        long tradeCount = history.tradeCount;
        long cooldownRemaining;
        if (tg.cooldown != -1 && lastTradeTime != -1 && (currentTimestamp - lastTradeTime) / 1000 < tg.cooldown) {
            cooldownRemaining = tg.cooldown - (currentTimestamp - lastTradeTime) / 1000;
        } else {
            cooldownRemaining = -1;
        }

        boolean enabled = tg.maxTrades == -1 || tradeCount < tg.maxTrades;

        return getAvailableTradeGroups(player).contains(tg) && enabled && cooldownRemaining < 0;
    }

    public void executeTrade(@Nonnull UUID player, TradeGroup tg) {
        TradeHistory newTradeHistory = getTradeState(player, tg);
        newTradeHistory.executeTrade(tg.maxTrades, tg.cooldown != -1);
        setTradeState(player, tg, newTradeHistory);
        if (newTradeHistory.notificationQueued) {
            addNotification(player, tg);
        }
    }

    public void addNotification(UUID player, TradeGroup tg) {
        VMTeamData teamData = getTeamData(player);
        if (teamData == null) return;
        Set<UUID> notificationQueue = teamData.getPlayerData(player).notificationQueue;
        if (notificationQueue.add(tg.getId())) {
            saveTeamData(player);
        }
    }

    public void sendTradeNotifications(EntityPlayerMP player) {
        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);
        VMTeamData teamData = getTeamData(playerId);
        if (teamData == null) return;
        Set<UUID> notifGroups = teamData.getPlayerData(playerId).notificationQueue;
        if (notifGroups == null || notifGroups.isEmpty()) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();

        List<UUID> notifList = new ArrayList<>();
        for (UUID tgId : notifGroups) {
            TradeGroup tradeGroup = TradeDatabase.INSTANCE.getTradeGroupFromId(tgId);
            if (tradeGroup == null) {
                continue;
            }
            TradeHistory th = getTradeState(playerId, tradeGroup);
            if ((currentTimestamp - th.lastTrade) / 1000 > tradeGroup.cooldown) {
                notifList.add(tgId);
                th.setNotified();
            }
        }

        if (!notifList.isEmpty()) {
            NetTradeNotification.sendNotification(player, notifList);
        }

        for (UUID tg : notifList) {
            notifGroups.remove(tg);
        }
        saveTeamData(playerId);
    }
}
