package com.cubefury.vendingmachine.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.trade.TradeHistory;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.teams.ITeamData;
import com.gtnewhorizon.gtnhlib.teams.Team;
import com.gtnewhorizon.gtnhlib.teams.TeamDataCopyReason;

public class VMTeamData implements ITeamData {

    public static String ID = "VM";

    private final Wallet wallet = new Wallet();
    private final Map<UUID, VMPlayerData> playerData = new HashMap<>();

    // Trade group UUID -> history
    private final Map<UUID, TradeHistory> tradeHistory = new HashMap<>();

    public VMPlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, id -> new VMPlayerData());
    }

    public Wallet getWallet(UUID uuid, WalletMode walletMode) {
        switch (walletMode) {
            case PERSONAL -> {
                return getPlayerData(uuid).wallet;
            }
            case TEAM -> {
                return wallet;
            }
        }
        return null;
    }

    public void setTradeHistory(UUID tg, TradeHistory history) {
        if (history == null || TradeHistory.DEFAULT.equals(history)) {
            tradeHistory.remove(tg);
        } else {
            tradeHistory.put(tg, history);
        }
    }

    public TradeHistory getTradeState(@Nonnull UUID tg) {
        return tradeHistory.getOrDefault(tg, new TradeHistory());
    }

    @Override
    public void mergeData(Team consumed, Team surviving, ITeamData oldTeamData) {
        if (oldTeamData instanceof VMTeamData other) {
            wallet.merge(other.wallet);

            playerData.putAll(other.playerData);
            for (Entry<UUID, TradeHistory> entry : other.tradeHistory.entrySet()) {
                tradeHistory.merge(entry.getKey(), entry.getValue(), TradeHistory::merge);
            }
        }
    }

    @Override
    public void copyData(Team prevTeam, Team newTeam, UUID playerId, ITeamData prevTeamData,
        TeamDataCopyReason reason) {
        VMTeamData oldTeamData = (VMTeamData) prevTeamData;
        VMPlayerData pd = oldTeamData.playerData.getOrDefault(playerId, null);
        if (pd != null) {
            playerData.put(playerId, pd);
        }
        for (Entry<UUID, TradeHistory> entry : oldTeamData.tradeHistory.entrySet()) {
            tradeHistory.merge(
                entry.getKey(),
                entry.getValue()
                    .copy(),
                TradeHistory::merge);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound teamWalletTag = new NBTTagCompound();
        wallet.writeToNBT(teamWalletTag);
        tag.setTag("wallet", teamWalletTag);

        if (!playerData.isEmpty()) {
            NBTTagList playerDataList = new NBTTagList();
            for (Entry<UUID, VMPlayerData> entry : playerData.entrySet()) {
                NBTTagCompound playerDataTag = new NBTTagCompound();
                playerDataList.appendTag(playerDataTag);
                playerDataTag.setString(
                    "uuid",
                    entry.getKey()
                        .toString());
                entry.getValue()
                    .writeToNBT(playerDataTag);
            }
            tag.setTag("players", playerDataList);
        }
        if (!tradeHistory.isEmpty()) {
            NBTTagList tradeHistoryList = new NBTTagList();
            for (Entry<UUID, TradeHistory> entry : tradeHistory.entrySet()) {
                NBTTagCompound historyTag = new NBTTagCompound();
                historyTag.setString(
                    "uuid",
                    entry.getKey()
                        .toString());

                TradeHistory history = entry.getValue();
                historyTag.setLong("lastTrade", history.lastTrade);
                historyTag.setInteger("tradeCount", history.tradeCount);
                historyTag.setBoolean("notificationQueued", history.notificationQueued);

                tradeHistoryList.appendTag(historyTag);
            }
            tag.setTag("tradeHistory", tradeHistoryList);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        playerData.clear();
        tradeHistory.clear();
        if (tag.hasKey("wallet")) {
            wallet.readFromNBT(tag.getCompoundTag("wallet"), false);
        }
        if (tag.hasKey("players")) {
            NBTTagList playersList = tag.getTagList("players", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < playersList.tagCount(); i++) {
                NBTTagCompound playerTag = playersList.getCompoundTagAt(i);
                UUID uuid = UUID.fromString(playerTag.getString("uuid"));
                VMPlayerData pd = new VMPlayerData();
                pd.readFromNBT(playerTag);
                playerData.put(uuid, pd);
            }
        }
        if (tag.hasKey("tradeHistory")) {
            NBTTagList tradeHistoryList = tag.getTagList("tradeHistory", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < tradeHistoryList.tagCount(); i++) {
                NBTTagCompound historyTag = tradeHistoryList.getCompoundTagAt(i);
                UUID uuid = UUID.fromString(historyTag.getString("uuid"));

                long lastTrade = historyTag.getLong("lastTrade");
                int tradeCount = historyTag.getInteger("tradeCount");
                boolean notificationQueued = historyTag.getBoolean("notificationQueued");
                tradeHistory.put(uuid, new TradeHistory(lastTrade, tradeCount, notificationQueued));
            }
        }
    }
}
