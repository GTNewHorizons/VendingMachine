package com.cubefury.vendingmachine.trade;

import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplay;
import com.cubefury.vendingmachine.util.NBTConverter;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class FavouritesTracker {
    public static final FavouritesTracker INSTANCE = new FavouritesTracker();
    private boolean dirty = false;

    public final Set<Pair<UUID, Integer>> favourites = new HashSet<>();

    private FavouritesTracker() {}

    public void toggleFavourites(UUID tradeGroupId, int tradeGroupOrder) {
        Pair pair = new ImmutablePair<>(tradeGroupId, tradeGroupOrder);
        if (favourites.contains(pair)) {
            favourites.remove(pair);
        } else {
            favourites.add(pair);
        }
        dirty = true;
    }

    public void saveFavourites() {
        if (dirty) {
            NBTTagList nbt = writeToNBT();
        }
        dirty = false;
    }

    private NBTTagList writeToNBT() {
        NBTTagList favouritesNbt = new NBTTagList();
        for (Pair<UUID, Integer> fave : favourites) {
            NBTTagCompound faveNbt = new NBTTagCompound();
            NBTConverter.UuidValueType.TRADEGROUP.writeId(fave.getLeft(), faveNbt);
            faveNbt.setInteger("tgOrder", fave.getRight());
            favouritesNbt.appendTag(faveNbt);
        }
        return favouritesNbt;
    }

    public void readFromNBT(NBTTagList nbt) {
        favourites.clear();
        for (int i = 0; i < nbt.tagCount(); i++) {
            NBTTagCompound faveNbt = nbt.getCompoundTagAt(i);
            favourites.add(new ImmutablePair<>(
                NBTConverter.UuidValueType.TRADEGROUP.readId(faveNbt),
                faveNbt.getInteger("tgOrder")
            ));
        }
    }

    public List<TradeItemDisplay> filterTrades(List<TradeItemDisplay> trades) {
        return Collections.EMPTY_LIST;
    }
}
