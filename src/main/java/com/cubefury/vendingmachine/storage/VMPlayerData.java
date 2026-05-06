package com.cubefury.vendingmachine.storage;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants.NBT;

import com.cubefury.vendingmachine.util.Wallet;

public class VMPlayerData {

    public final Wallet wallet = new Wallet();

    // trades with pending refresh notifications
    public final Set<UUID> notificationQueue = new HashSet<>();

    protected void writeToNBT(NBTTagCompound tag) {
        NBTTagCompound walletTag = new NBTTagCompound();
        wallet.writeToNBT(walletTag);
        tag.setTag("wallet", walletTag);

        if (!notificationQueue.isEmpty()) {
            NBTTagList notificationQueueList = new NBTTagList();
            for (UUID uuid : notificationQueue) {
                notificationQueueList.appendTag(new NBTTagString(uuid.toString()));
            }
            tag.setTag("notificationQueue", notificationQueueList);
        }
    }

    protected void readFromNBT(NBTTagCompound tag) {
        wallet.readFromNBT(tag.getCompoundTag("wallet"), false);

        notificationQueue.clear();
        if (tag.hasKey("notificationQueue")) {
            NBTTagList notificationQueueList = tag.getTagList("notificationQueue", NBT.TAG_STRING);
            for (int i = 0; i < notificationQueueList.tagCount(); i++) {
                notificationQueue.add(UUID.fromString(notificationQueueList.getStringTagAt(i)));
            }
        }
    }
}
