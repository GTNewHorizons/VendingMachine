package com.cubefury.vendingmachine.api.network;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface IPacketRegistry {

    void registerServerHandler(@Nonnull ResourceLocation idName,
        @Nonnull BiConsumer<NBTTagCompound, EntityPlayerMP> method);

    @SideOnly(Side.CLIENT)
    void registerClientHandler(@Nonnull ResourceLocation idName, @Nonnull Consumer<NBTTagCompound> method);

    @Nullable
    BiConsumer<NBTTagCompound, EntityPlayerMP> getServerHandler(@Nonnull ResourceLocation idName);

    @Nullable
    @SideOnly(Side.CLIENT)
    Consumer<NBTTagCompound> getClientHandler(@Nonnull ResourceLocation idName);
}
