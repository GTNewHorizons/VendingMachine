package com.cubefury.vendingmachine.api.storage;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public interface INameCache {

    boolean updateName(@Nonnull EntityPlayerMP player);

    String getName(@Nonnull UUID uuid);

    UUID getUUID(@Nonnull String name);

    UUID getUUIDFromPlayer(EntityPlayer player);

    List<String> getAllNames();

    List<UUID> getAllUUIDS();

    /**
     * Used primarily to know if a user is an OP client side<br>
     */
    boolean isOP(UUID uuid);

    int size();

    void clear();
}
