package com.cubefury.vendingmachine.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.util.Wallet;
import com.gtnewhorizon.gtnhlib.teams.ITeamData;

public class VMTeamData implements ITeamData {

    public static String ID = "VM";

    private final Wallet wallet = new Wallet();
    private final Map<UUID, VMPlayerData> playerData = new HashMap<>();

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
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        playerData.clear();
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
    }
}
