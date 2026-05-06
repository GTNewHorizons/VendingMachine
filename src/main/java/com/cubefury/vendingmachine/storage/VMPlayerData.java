package com.cubefury.vendingmachine.storage;

import com.cubefury.vendingmachine.util.Wallet;
import net.minecraft.nbt.NBTTagCompound;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VMPlayerData {
    public final Wallet wallet = new Wallet();
    public final Set<UUID> availableTrades = new HashSet<>();

    //trades with pending refresh notifications
    public final Set<UUID> notificationQueue = new HashSet<>();

    protected void writeToNBT(NBTTagCompound tag){
        NBTTagCompound walletTag = new NBTTagCompound();
        wallet.writeToNBT(walletTag);
        tag.setTag("wallet", walletTag);
    }

    protected void readFromNBT(NBTTagCompound tag){
        wallet.readFromNBT(tag.getCompoundTag("wallet"), false);
    }
}
