package com.cubefury.vendingmachine.network;

import java.util.concurrent.Executors;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.util.Tuple2;
import com.cubefury.vendingmachine.storage.NameCache;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SerializedPacket implements IMessage {

    protected NBTTagCompound tags = new NBTTagCompound();

    @SuppressWarnings("unused")
    public SerializedPacket() // For use only by forge
    {}

    public SerializedPacket(NBTTagCompound tags) // Use PacketDataTypes to instantiate new packets
    {
        this.tags = tags;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        tags = ByteBufUtils.readTag(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeTag(buf, tags);
    }

    public static class HandleServer implements IMessageHandler<SerializedPacket, IMessage> {

        @Override
        public IMessage onMessage(SerializedPacket packet, MessageContext ctx) {
            if (packet == null || packet.tags == null || ctx.getServerHandler().playerEntity.mcServer == null) {
                VendingMachine.LOG.error(
                    "A critical NPE error occurred during while handling a VendingMachine packet server side",
                    new NullPointerException());
                return null;
            }

            final EntityPlayerMP sender = ctx.getServerHandler().playerEntity;
            final NBTTagCompound message = PacketAssembly.INSTANCE
                .assemblePacket(sender == null ? null : NameCache.INSTANCE.getUUIDFromPlayer(sender), packet.tags);

            if (message == null) {
                return null;
            } else if (!message.hasKey("ID")) {
                VendingMachine.LOG.warn("Recieved a packet server side without an ID");
                return null;
            }

            final Consumer<Tuple2<NBTTagCompound, EntityPlayerMP>> method = PacketTypeRegistry.INSTANCE
                .getServerHandler(new ResourceLocation(message.getString("ID")));

            if (method == null) {
                VendingMachine.LOG
                    .warn("Recieved a packet server side with an invalid ID: {}", message.getString("ID"));
                return null;
            } else if (sender != null) {
                VendingMachine.proxy
                    .scheduleServerTask(Executors.callable(() -> method.accept(new Tuple2<>(message, sender))));
            }

            return null;
        }
    }

    public static class HandleClient implements IMessageHandler<SerializedPacket, IMessage> {

        @Override
        public IMessage onMessage(SerializedPacket packet, MessageContext ctx) {
            if (packet == null || packet.tags == null) {
                VendingMachine.LOG.error(
                    "Critical NPE error occured while handling Vending Machine packet client side",
                    new NullPointerException());
                return null;
            }

            final NBTTagCompound message = PacketAssembly.INSTANCE.assemblePacket(null, packet.tags);

            if (message == null) {
                return null;
            } else if (!message.hasKey("ID")) {
                VendingMachine.LOG.warn("Received a packet on server-side without an ID");
                return null;
            }

            final Consumer<NBTTagCompound> method = PacketTypeRegistry.INSTANCE
                .getClientHandler(new ResourceLocation(message.getString("ID")));

            if (method == null) {
                VendingMachine.LOG
                    .warn("Received a packet on server-side with invalid ID: {}", message.getString("ID"));
            } else {
                Minecraft.getMinecraft()
                    .func_152343_a(Executors.callable(() -> method.accept(message)));
            }

            return null;
        }
    }
}
