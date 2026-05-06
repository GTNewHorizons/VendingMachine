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
    private final Map<UUID, Wallet> personalWallets = new HashMap<>();

    public Wallet getWallet(UUID uuid, WalletMode walletMode) {
        switch (walletMode) {
            case PERSONAL -> {
                return personalWallets.computeIfAbsent(uuid, id -> new Wallet());
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

        if (!personalWallets.isEmpty()) {
            NBTTagList personalList = new NBTTagList();
            for (Entry<UUID, Wallet> entry : personalWallets.entrySet()) {
                NBTTagCompound personalTag = new NBTTagCompound();
                personalList.appendTag(personalTag);
                personalTag.setString(
                    "uuid",
                    entry.getKey()
                        .toString());
                NBTTagCompound walletTag = new NBTTagCompound();
                entry.getValue()
                    .writeToNBT(walletTag);
                personalTag.setTag("wallet", walletTag);
            }
            tag.setTag("personal", personalList);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        personalWallets.clear();
        if (tag.hasKey("wallet")) {
            wallet.readFromNBT(tag.getCompoundTag("wallet"), false);
        }
        if (tag.hasKey("personal")) {
            NBTTagList personalList = tag.getTagList("personal", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < personalList.tagCount(); i++) {
                NBTTagCompound personalTag = personalList.getCompoundTagAt(i);
                UUID uuid = UUID.fromString(personalTag.getString("uuid"));
                Wallet personalWallet = new Wallet();
                personalWallet.readFromNBT(personalTag.getCompoundTag("wallet"), false);
                personalWallets.put(uuid, personalWallet);
            }
        }
    }
}
