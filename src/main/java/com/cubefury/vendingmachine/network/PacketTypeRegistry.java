package com.cubefury.vendingmachine.network;

import java.util.HashMap;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.api.network.IPacketRegistry;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.network.handlers.NetBulkSync;
import com.cubefury.vendingmachine.network.handlers.NetNameSync;
import com.cubefury.vendingmachine.network.handlers.NetResetVMUser;
import com.cubefury.vendingmachine.network.handlers.NetSatisfiedQuestSync;
import com.cubefury.vendingmachine.network.handlers.NetTradeDbSync;
import com.cubefury.vendingmachine.network.handlers.NetTradeDisplaySync;
import com.cubefury.vendingmachine.network.handlers.NetTradeRequestSync;

public class PacketTypeRegistry implements IPacketRegistry {

    public static final PacketTypeRegistry INSTANCE = new PacketTypeRegistry();

    private final HashMap<ResourceLocation, Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>>> serverHandlers = new HashMap<>();
    private final HashMap<ResourceLocation, Consumer<NBTTagCompound>> clientHandlers = new HashMap<>();

    public void init() {
        NetTradeDbSync.registerHandler();
        NetTradeDisplaySync.registerHandler();
        NetTradeRequestSync.registerHandler();
        NetSatisfiedQuestSync.registerHandler();
        NetNameSync.registerHandler();
        NetBulkSync.registerHandler();
        NetResetVMUser.registerHandler();
    }

    @Override
    public void registerServerHandler(@Nonnull ResourceLocation idName,
        @Nonnull Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>> method) {
        if (serverHandlers.containsKey(idName)) {
            throw new IllegalArgumentException("Cannot register duplicate packet handler: " + idName);
        }

        serverHandlers.put(idName, method);
    }

    @Override
    public void registerClientHandler(@Nonnull ResourceLocation idName, @Nonnull Consumer<NBTTagCompound> method) {
        if (clientHandlers.containsKey(idName)) {
            throw new IllegalArgumentException("Cannot register duplicate packet handler: " + idName);
        }

        clientHandlers.put(idName, method);
    }

    @Nullable
    @Override
    public Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>> getServerHandler(@Nonnull ResourceLocation idName) {
        return serverHandlers.get(idName);
    }

    @Nullable
    @Override
    public Consumer<NBTTagCompound> getClientHandler(@Nonnull ResourceLocation idName) {
        return clientHandlers.get(idName);
    }
}
