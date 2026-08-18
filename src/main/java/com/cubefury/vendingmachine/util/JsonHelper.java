package com.cubefury.vendingmachine.util;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.integration.materiallib.MaterialLibStacks;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.FavouritesTracker;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.google.gson.JsonObject;

public class JsonHelper {

    private static final String ML_PREFIX = "ml:";

    private static int mlResolved = 0;
    private static int mlInvalid = 0;

    public static BigItemStack JsonToItemStack(@Nonnull NBTTagCompound nbt) {
        BigItemStack mlStack = JsonToMaterialLibStack(nbt);
        if (mlStack != null) {
            return mlStack;
        }

        String idName = nbt.hasKey("id", Constants.NBT.TAG_ANY_NUMERIC) ? "" + nbt.getShort("id") : nbt.getString("id");
        Item preCheck = nbt.hasKey("id", Constants.NBT.TAG_ANY_NUMERIC) ? Item.getItemById(nbt.getShort("id"))
            : (Item) Item.itemRegistry.getObject(idName);
        if (preCheck == null && nbt.hasKey("id", Constants.NBT.TAG_STRING)) {
            try {
                preCheck = Item.getItemById(Short.parseShort(idName));
            } catch (Exception ignored) {}
        }

        if (preCheck != null && preCheck != ItemPlaceholder.placeholder) { // valid item
            return BigItemStack.loadItemStackFromNBT(nbt);
        }
        return ItemPlaceholder.getBigItemStackFrom(
            preCheck,
            idName,
            nbt.getInteger("Count"),
            nbt.getShort("Damage"),
            nbt.getString("OreDict"),
            !nbt.hasKey("tag", Constants.NBT.TAG_COMPOUND) ? null : nbt.getCompoundTag("tag"));
    }

    /// The stack an `ml:<material>:<shape>` entry names, or null when the entry names something else, when MaterialLib
    /// is absent, or when the reference resolves to nothing. The stored `Damage` is ignored in favor of the resolved
    /// stack's.
    public static BigItemStack JsonToMaterialLibStack(@Nonnull NBTTagCompound nbt) {
        if (!nbt.hasKey("id", Constants.NBT.TAG_STRING) || !VendingMachine.isMaterialLibLoaded) {
            return null;
        }
        String idName = nbt.getString("id");
        if (!idName.startsWith(ML_PREFIX)) {
            return null;
        }

        BigItemStack stack = resolveMaterialLibStack(idName, nbt);
        if (stack == null) {
            mlInvalid++;
            return null;
        }
        mlResolved++;
        return stack;
    }

    private static BigItemStack resolveMaterialLibStack(String reference, NBTTagCompound nbt) {
        ItemStack resolved = MaterialLibStacks.resolve(reference, 1);
        if (resolved == null) {
            return null;
        }
        BigItemStack stack = new BigItemStack(resolved).setOreDict(nbt.getString("OreDict"));
        stack.stackSize = nbt.getInteger("Count");
        if (nbt.hasKey("tag", Constants.NBT.TAG_COMPOUND)) {
            stack.setTagCompound(nbt.getCompoundTag("tag"));
        }
        return stack;
    }

    // We use this instead of converting ItemStack to NBT since this doesn't use ID numbers
    public static NBTTagCompound ItemStackToJson(BigItemStack stack, NBTTagCompound nbt) {
        if (stack != null) {
            return stack.writeToNBT(nbt);
        }
        return nbt;
    }

    public static void populateTradeDatabaseFromFile(File file) {
        TradeDatabase db = TradeDatabase.INSTANCE;

        Function<File, NBTTagCompound> readNbt = f -> NBTConverter
            .JSONtoNBT_Object(FileIO.ReadFromFile(f), new NBTTagCompound(), true);

        mlResolved = 0;
        mlInvalid = 0;
        db.readFromNBT(readNbt.apply(file), false, true);
        if (mlResolved + mlInvalid > 0) {
            VendingMachine.LOG
                .info("{}: resolved {} MaterialLib entries ({} invalid)", VendingMachine.NAME, mlResolved, mlInvalid);
        }
    }

    public static void populateNameCacheFromFile(File file) {
        JsonObject json = FileIO.ReadFromFile(file);

        NBTTagCompound nbt = NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound(), true);
        NameCache.INSTANCE.readFromNBT(nbt.getTagList("nameCache", Constants.NBT.TAG_COMPOUND), false);
    }

    public static void populateFavouritesFromFile(File file) {
        JsonObject json = FileIO.ReadFromFile(file);
        NBTTagCompound nbt = NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound(), true);
        FavouritesTracker.INSTANCE.readFromNBT(nbt, false);
    }

    @FunctionalInterface
    public interface IOConsumer<T> {

        void accept(T arg) throws IOException;
    }
}
