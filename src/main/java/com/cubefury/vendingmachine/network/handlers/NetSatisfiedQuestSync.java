package com.cubefury.vendingmachine.network.handlers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.api.network.UnserializedPacket;
import com.cubefury.vendingmachine.integration.betterquesting.BqAdapter;
import com.cubefury.vendingmachine.network.PacketSender;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class NetSatisfiedQuestSync {

    private static final ResourceLocation ID_NAME = new ResourceLocation("vendingmachine:questdone_sync");

    public static void registerHandler() {
        if (VendingMachine.proxy.isClient()) {
            PacketTypeRegistry.INSTANCE.registerClientHandler(ID_NAME, NetSatisfiedQuestSync::onClient);
        }
    }

    public static void sendSync() {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        Map<UUID, Set<UUID>> cache = BqAdapter.INSTANCE.getPlayerSatisfiedCache();
        if (server != null) { // Sync to all connected players
            List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;

            for (EntityPlayerMP player : players) {
                UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(player);
                NBTTagCompound payload = new NBTTagCompound();
                NBTTagList questList = new NBTTagList();
                // have to do cache null check here as it may not be initialized
                if (cache != null && cache.get(playerId) != null) {
                    for (UUID quest : cache.get(playerId)) {
                        NBTTagCompound questInfo = new NBTTagCompound();
                        NBTConverter.UuidValueType.QUEST.writeId(quest, questInfo);
                        questList.appendTag(questInfo);
                    }
                }
                payload.setTag("questData", questList);
                PacketSender.INSTANCE.sendToPlayers(new UnserializedPacket(ID_NAME, payload), player);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void onClient(NBTTagCompound message) {
        if (
            Minecraft.getMinecraft()
                .isIntegratedServerRunning()
        ) {
            // Don't override in singleplayer or on LAN
            return;
        }

        UUID playerId = NameCache.INSTANCE.getUUIDFromPlayer(Minecraft.getMinecraft().thePlayer);

        Map<UUID, Set<UUID>> newCache = new HashMap<>();
        newCache.put(playerId, new HashSet<>());
        NBTTagList questList = message.getTagList("questData", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < questList.tagCount(); i++) {
            newCache.get(playerId)
                .add(NBTConverter.UuidValueType.QUEST.readId(questList.getCompoundTagAt(i)));
        }
        BqAdapter.INSTANCE.setPlayerSatisfiedCache(newCache);
    }
}
