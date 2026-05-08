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
import com.cubefury.vendingmachine.blocks.gui.MTEVendingMachineGui;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeRequest;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class NetTradeRequestSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:traderequest_sync");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetTradeRequestSync::onServer);

        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetTradeRequestSync::onClient);
        }
    }

    public static void sendTradeRequest(TradeItemDisplay trade, World world, int x, int y, int z,
        WalletMode walletMode) {
        NBTTagCompound payload = new NBTTagCompound();
        NBTConverter.UuidValueType.TRADEGROUP.writeId(trade.tgID, payload);
        payload.setInteger("tradeGroupOrder", trade.tradeGroupOrder);
        payload.setInteger("dim", world.provider.dimensionId);
        payload.setInteger("x", x);
        payload.setInteger("y", y);
        payload.setInteger("z", z);
        payload.setByte("wallet", (byte) walletMode.ordinal());
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    public static void sendAck(EntityPlayerMP player) {
        PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, new NBTTagCompound()), player);
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
            ((MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity()).addTradeRequest(
                new TradeRequest(
                    message.second(),
                    NameCache.INSTANCE.getUUIDFromPlayer(message.second()),
                    NBTConverter.UuidValueType.TRADEGROUP.readId(message.first()),
                    message.first()
                        .getInteger("tradeGroupOrder"),
                    WalletMode.values()[message.first()
                        .getByte("wallet")],
                    (MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity()));
        }
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        MTEVendingMachineGui.setForceRefresh();
    }
}
