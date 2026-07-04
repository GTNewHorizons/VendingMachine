package com.cubefury.vendingmachine.network.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class NetCoinDeposit {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:coin_deposit");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetCoinDeposit::onServer);
    }

    public static void sendDeposit(World world, int x, int y, int z, WalletMode walletMode) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("dim", world.provider.dimensionId);
        payload.setInteger("x", x);
        payload.setInteger("y", y);
        payload.setInteger("z", z);
        payload.setByte("wallet", (byte) walletMode.ordinal());
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    public static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message) {
        NBTTagCompound payload = message.first();
        World world = DimensionManager.getWorld(payload.getInteger("dim"));
        if (world == null) {
            return;
        }
        TileEntity te = world.getTileEntity(payload.getInteger("x"), payload.getInteger("y"), payload.getInteger("z"));
        if (
            te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine
        ) {
            MTEVendingMachine machine = (MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity();
            machine.depositHeldCoins(message.second(), WalletMode.values()[payload.getByte("wallet")]);
        }
    }
}
