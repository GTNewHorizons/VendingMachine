package com.cubefury.vendingmachine.network.handlers;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeManager;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class NetResetVMUser {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:reset_vmuser");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetResetVMUser::onServer);
    }

    public static void sendReset(MTEVendingMachine base) {
        IGregTechTileEntity baseTile = base.getBaseMetaTileEntity();
        if (baseTile == null) {
            VendingMachine.LOG.warn("attempting to reset user for null base MTE");
            return;
        }
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("dim", baseTile.getWorld().provider.dimensionId);
        payload.setInteger("x", baseTile.getXCoord());
        payload.setInteger("y", baseTile.getYCoord());
        payload.setInteger("z", baseTile.getZCoord());
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    public static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message) {
        World world = DimensionManager.getWorld(
            message.first()
                .getInteger("dim"));
        TileEntity te = world.getTileEntity(
            message.first()
                .getInteger("x"),
            message.first()
                .getInteger("y"),
            message.first()
                .getInteger("z"));
        if (
            te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine
        ) {
            ((MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity()).resetCurrentUser(message.second());
        }
        TradeManager.INSTANCE.saveTeamData(NameCache.INSTANCE.getUUIDFromPlayer(message.second()));
    }
}
