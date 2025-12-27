package com.cubefury.vendingmachine.trade;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.integration.betterquesting.BqAdapter;
import com.cubefury.vendingmachine.integration.nei.NeiRecipeCache;
import com.cubefury.vendingmachine.util.NBTConverter;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TradeDatabase {

    public static final TradeDatabase INSTANCE = new TradeDatabase();
    public int version = -1;
    private final Map<UUID, TradeGroup> tradeGroups = new HashMap<>();
    private final Map<TradeCategory, Set<UUID>> tradeCategories = new HashMap<>();

    public final List<TradeGroup> noConditionTrades = new ArrayList<>();

    private TradeDatabase() {}

    public void clear() {
        noConditionTrades.clear();
        tradeGroups.clear();
        tradeCategories.clear();
    }

    public TradeGroup getTradeGroupFromId(UUID tgId) {
        return tradeGroups.get(tgId);
    }

    public int getTradeGroupCount() {
        return tradeGroups.size();
    }

    public Map<UUID, TradeGroup> getTradeGroups() {
        return tradeGroups;
    }

    public List<TradeCategory> getTradeCategories() {
        List<TradeCategory> tradeCategoryList = new ArrayList<>(tradeCategories.keySet());
        tradeCategoryList.sort(Comparator.comparing(TradeCategory::ordinal));
        return tradeCategoryList;
    }

    public Set<UUID> getTradeGroupsFromCategory(TradeCategory category) {
        return tradeCategories.get(category);
    }

    public int getTradeCount() {
        return tradeGroups.values()
            .stream()
            .mapToInt(
                tg -> tg.getTrades()
                    .size())
            .sum();
    }

    public void readFromNBT(NBTTagCompound nbt, boolean merge, boolean isFileLoad) {
        if (!merge) {
            this.clear();
            if (VendingMachine.isBqLoaded) {
                BqAdapter.INSTANCE.resetQuestTriggers(null);
            }
        }
        int newMetadataCount = 0;
        this.version = nbt.getInteger("version");
        NBTTagList trades = nbt.getTagList("tradeGroups", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < trades.tagCount(); i++) {
            TradeGroup tg = new TradeGroup();
            newMetadataCount += tg.readFromNBT(trades.getCompoundTagAt(i)) ? 1 : 0;
            if (tradeGroups.containsKey(tg.getId())) {
                VendingMachine.LOG.error("Multiple trade groups with id {} exist in the file!", tg);
                continue;
            }
            if (tg.hasNoConditions()) {
                noConditionTrades.add(tg);
            }
            tradeCategories.computeIfAbsent(tg.getCategory(), k -> new HashSet<>());
            tradeCategories.get(tg.getCategory())
                .add(tg.getId());

            tradeGroups.put(tg.getId(), tg);
        }
        if (isFileLoad && (VMConfig.developer.force_rewrite_database || newMetadataCount > 0)) {
            VendingMachine.LOG.info("Appended metadata to {} new trades", newMetadataCount);
            DirtyDbMarker.markDirty();
        }
        if (VendingMachine.proxy.isClient()) {
            refreshNeiCache();
        }

        VendingMachine.LOG.info("Loaded {} trade groups containing {} trades.", getTradeGroupCount(), getTradeCount());
    }

    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        nbt.setInteger("version", this.version);
        NBTTagList tgList = new NBTTagList();
        tradeGroups.values()
            .stream()
            .sorted(Comparator.comparing(TradeGroup::getId))
            .forEach(tg -> tgList.appendTag(tg.writeToNBT(new NBTTagCompound())));
        nbt.setTag("tradeGroups", tgList);
        return nbt;
    }

    @SideOnly(Side.CLIENT)
    public void refreshNeiCache() {
        NeiRecipeCache.refreshCache();
    }

}
