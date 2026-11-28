package com.cubefury.vendingmachine.network.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.events.MarkDirtyNamesEvent;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

// This is meant for syncing multiplayer uuid-name & op status pairings in multiplayer
// It's mostly ignored in singleplayer
public class NetNameSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:name_sync");

    public static void registerHandler() {
        PacketTypeRegistry.INSTANCE.registerServerHandler(ID_NAME, NetNameSync::onServer);

        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetNameSync::onClient);
        }
    }

    // request for either uuids or names from server
    @SideOnly(Side.CLIENT)
    public static void sendRequest(@Nullable UUID[] uuids, @Nullable String[] names) {
        NBTTagCompound payload = new NBTTagCompound();
        if (uuids != null) {
            NBTTagList uList = new NBTTagList();
            for (UUID id : uuids) {
                if (id == null) {
                    continue;
                }
                uList.appendTag(new NBTTagString(id.toString()));
            }
            payload.setTag("uuids", uList);
        }
        if (names != null) {
            NBTTagList nList = new NBTTagList();
            for (String s : names) {
                if (StringUtils.isNullOrEmpty(s)) {
                    continue;
                }
                nList.appendTag(new NBTTagString(s));
            }
            payload.setTag("names", nList);
        }
        PacketSender.INSTANCE.sendToServer(new UnserializedPacket(ID_NAME, payload));
    }

    public static void sendNames(@Nullable EntityPlayerMP[] players, @Nullable UUID[] uuids, @Nullable String[] names) {
        // idk if this needs to happen on SP/LAN, but we'll just let it sync anyway
        // BetterQuesting's code says apparently the player IDs behave weird otherwise
        List<UUID> idList = (uuids == null && names == null) ? null : new ArrayList<>();
        if (uuids != null) idList.addAll(Arrays.asList(uuids));
        if (names != null) {
            for (String s : names) {
                UUID id = NameCache.INSTANCE.getUUID(s);
                if (id != null) {
                    idList.add(id);
                }
            }
        }

        NBTTagCompound payload = new NBTTagCompound();
        payload.setTag("data", NameCache.INSTANCE.writeToNBT(new NBTTagList(), idList));
        payload.setBoolean("merge", idList != null);

        if (players == null) {
            PacketSender.INSTANCE.sendToAll(new UnserializedPacket(ID_NAME, payload));
        } else {
            PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), players);
        }
    }

    private static void onServer(NBTTagCompound tag, EntityPlayerMP player) {
        UUID[] uuids = null;
        String[] names = null;

        if (tag.hasKey("uuids", Constants.NBT.TAG_LIST)) {
            NBTTagList uList = tag.getTagList("uuids", Constants.NBT.TAG_STRING);
            uuids = new UUID[uList.tagCount()];
            for (int i = 0; i < uuids.length; i++) {
                try {
                    uuids[i] = UUID.fromString(uList.getStringTagAt(i));
                } catch (Exception ignored) {}
            }
        }
        if (tag.hasKey("names", Constants.NBT.TAG_LIST)) {
            NBTTagList uList = tag.getTagList("names", Constants.NBT.TAG_STRING);
            names = new String[uList.tagCount()];
            for (int i = 0; i < names.length; i++) {
                names[i] = uList.getStringTagAt(i);
            }
        }
        sendNames(new EntityPlayerMP[] { player }, uuids, names);
    }

    @SideOnly(Side.CLIENT)
    private static void onClient(NBTTagCompound message) {
        NameCache.INSTANCE
            .readFromNBT(message.getTagList("data", Constants.NBT.TAG_COMPOUND), message.getBoolean("merge"));
        MinecraftForge.EVENT_BUS.post(new MarkDirtyNamesEvent());
    }

}
