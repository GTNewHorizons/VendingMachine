package com.cubefury.vendingmachine.network.handlers;

import net.minecraft.client.Minecraft;
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

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

public class NetWorldMusicSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:worldmusic_sync");
    private static final int BROADCAST_RADIUS = 64;

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetWorldMusicSync::onServer);
        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetWorldMusicSync::onClient);
        }
    }

    private static NBTTagCompound coords(IGregTechTileEntity te) {
        NBTTagCompound payload = new NBTTagCompound();
        payload.setInteger("dim", te.getWorld().provider.dimensionId);
        payload.setInteger("x", te.getXCoord());
        payload.setInteger("y", te.getYCoord());
        payload.setInteger("z", te.getZCoord());
        return payload;
    }

    // Client -> server: ask for the current state (sent when a client tile first ticks).
    public static void requestState(MTEVendingMachine base) {
        IGregTechTileEntity te = base.getBaseMetaTileEntity();
        if (te == null) return;
        NBTTagCompound payload = coords(te);
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    // Server -> nearby clients: broadcast the current state (sent when it changes).
    public static void broadcastState(MTEVendingMachine base) {
        IGregTechTileEntity te = base.getBaseMetaTileEntity();
        if (te == null) return;
        NBTTagCompound payload = coords(te);
        payload.setBoolean("enabled", base.isWorldMusicEnabled());
        PacketSender.INSTANCE.sendToAround(
            new UnserializedPacket(ID_NAME, payload),
            new NetworkRegistry.TargetPoint(
                te.getWorld().provider.dimensionId,
                te.getXCoord() + 0.5,
                te.getYCoord() + 0.5,
                te.getZCoord() + 0.5,
                BROADCAST_RADIUS));
    }

    public static void onServer(Tuple2<NBTTagCompound, EntityPlayerMP> message) {
        NBTTagCompound m = message.first();
        World world = DimensionManager.getWorld(m.getInteger("dim"));
        if (world == null) return;
        TileEntity te = world.getTileEntity(m.getInteger("x"), m.getInteger("y"), m.getInteger("z"));
        if (
            te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine
        ) {
            MTEVendingMachine vm = (MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity();
            NBTTagCompound payload = new NBTTagCompound();
            payload.setInteger("dim", m.getInteger("dim"));
            payload.setInteger("x", m.getInteger("x"));
            payload.setInteger("y", m.getInteger("y"));
            payload.setInteger("z", m.getInteger("z"));
            payload.setBoolean("enabled", vm.isWorldMusicEnabled());
            PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), message.second());
        }
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound m) {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        TileEntity te = world.getTileEntity(m.getInteger("x"), m.getInteger("y"), m.getInteger("z"));
        if (
            te instanceof IGregTechTileEntity
                && ((IGregTechTileEntity) te).getMetaTileEntity() instanceof MTEVendingMachine
        ) {
            ((MTEVendingMachine) ((IGregTechTileEntity) te).getMetaTileEntity()).clientWorldMusicEnabled = m
                .getBoolean("enabled");
        }
    }
}
