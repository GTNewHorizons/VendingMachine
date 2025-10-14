package com.cubefury.vendingmachine.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nonnull;

import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.google.gson.JsonObject;

public class JsonHelper {

    public static BigItemStack JsonToItemStack(@Nonnull NBTTagCompound nbt) {
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

        db.readFromNBT(readNbt.apply(file), false, true);
    }

    public static void populateTradeStateFromFiles(List<File> files) {
        files.forEach(JsonHelper::populateTradeStateFromFile);
    }

    public static void populateTradeStateFromFile(File file) {
        JsonObject json = FileIO.ReadFromFile(file);
        NBTTagCompound nbt = NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound(), true);
        TradeDatabase.INSTANCE.populateTradeStateFromNBT(nbt, UUID.fromString(FileIO.getFileName(file)), false);
    }

    public static void populateNameCacheFromFile(File file) {
        JsonObject json = FileIO.ReadFromFile(file);

        NBTTagCompound nbt = NBTConverter.JSONtoNBT_Object(json, new NBTTagCompound(), true);
        NameCache.INSTANCE.readFromNBT(nbt.getTagList("nameCache", Constants.NBT.TAG_COMPOUND), false);
    }

    @FunctionalInterface
    public interface IOConsumer<T> {

        void accept(T arg) throws IOException;
    }
}
