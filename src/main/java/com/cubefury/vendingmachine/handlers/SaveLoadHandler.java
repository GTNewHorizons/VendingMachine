package com.cubefury.vendingmachine.handlers;

import static com.cubefury.vendingmachine.util.FileIO.CopyPaste;

import java.io.File;
import java.io.FileReader;
import java.util.UUID;
import java.util.concurrent.Future;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import org.apache.commons.io.FileUtils;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.storage.VMPlayerData;
import com.cubefury.vendingmachine.storage.VMTeamData;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.trade.FavouritesTracker;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.util.FileIO;
import com.cubefury.vendingmachine.util.JsonHelper;
import com.cubefury.vendingmachine.util.NBTConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class SaveLoadHandler {

    public static SaveLoadHandler INSTANCE = new SaveLoadHandler();

    private static final Gson GSON = new GsonBuilder().create();

    private File fileDatabase = null;
    private File fileNames = null;
    private File dirTradeState = null;

    private File dirFavourites = null;

    private SaveLoadHandler() {}

    public void init(MinecraftServer server) {
        if (VendingMachine.proxy.isClient()) {
            VMConfig.world_dir = server.getFile("saves/" + server.getFolderName() + "/" + VMConfig.developer.data_dir);
        } else {
            VMConfig.world_dir = server.getFile(server.getFolderName() + "/" + VMConfig.developer.data_dir);
        }

        fileDatabase = new File(VMConfig.developer.trade_db_dir, "tradeDatabase.json");
        dirTradeState = new File(VMConfig.world_dir, "tradeState");
        fileNames = new File(VMConfig.world_dir, "names.json");

        createFilesAndDirectories();

        unloadAll();

        loadDatabase();
        loadNames();
    }

    public void clientInit() {
        dirFavourites = new File(VMConfig.developer.trade_db_dir, "favourites");

        if (dirFavourites.mkdirs()) {
            VendingMachine.LOG.info("Created favourited trades directory");
        }
    }

    public void createFilesAndDirectories() {
        if (!fileDatabase.exists()) {
            try {
                if (fileDatabase.createNewFile()) {
                    VendingMachine.LOG.info("Created new trade database file");
                }
            } catch (Exception ignored) {
                VendingMachine.LOG.warn("Could not create new trade database file");
            }
        }
        if (!fileNames.exists()) {
            try {
                if (fileNames.createNewFile()) {
                    VendingMachine.LOG.info("Created new name cache file");
                }
            } catch (Exception ignored) {
                VendingMachine.LOG.warn("Could not create new name cache file");
            }
        }
    }

    public void loadDatabase() {
        JsonHelper.populateTradeDatabaseFromFile(fileDatabase);
    }

    public Future<Void> writeDatabase() {
        CopyPaste(fileDatabase, new File(VMConfig.developer.trade_db_dir + "/backup", "tradeDatabase.json"));
        return FileIO.WriteToFile(
            fileDatabase,
            out -> NBTConverter.NBTtoJSON_Compound(TradeDatabase.INSTANCE.writeToNBT(new NBTTagCompound()), out, true));
    }

    public void loadNames() {
        JsonHelper.populateNameCacheFromFile(fileNames);
    }

    public Future<Void> writeNames() {
        NBTTagCompound json = new NBTTagCompound();
        json.setTag("nameCache", NameCache.INSTANCE.writeToNBT(new NBTTagList(), null));
        return FileIO.WriteToFile(fileNames, out -> NBTConverter.NBTtoJSON_Compound(json, out, true));
    }

    public void unloadAll() {
        NameCache.INSTANCE.clear();
        TradeDatabase.INSTANCE.clear();
    }

    public void reloadDatabase() {
        TradeDatabase.INSTANCE.clear();
        loadDatabase();
    }

    public Future<Void> writeFavourites(UUID player, String world_identifier) {
        if (player == null || world_identifier == null) {
            return null;
        }
        NBTTagCompound json = FavouritesTracker.INSTANCE.writeToNBT(new NBTTagCompound());
        File playerDir = new File(dirFavourites, player.toString());
        playerDir.mkdirs();
        File worldFavourites = new File(playerDir, world_identifier + ".json");
        return FileIO.WriteToFile(worldFavourites, out -> NBTConverter.NBTtoJSON_Compound(json, out, true));
    }

    public void readFavourites(UUID player, String world_identifier) {
        FavouritesTracker.INSTANCE.clearFavourites();
        File playerDir = new File(dirFavourites, player.toString());
        if (!playerDir.exists()) {
            return;
        }
        File worldFavourites = new File(playerDir, world_identifier + ".json");
        if (worldFavourites.exists()) {
            JsonHelper.populateFavouritesFromFile(worldFavourites);
        }
    }

    public boolean attemptMigrate(VMTeamData teamData, UUID playerId) {
        File oldTradeStateFile = new File(dirTradeState, playerId.toString() + ".json");
        if (!oldTradeStateFile.exists()) return false;
        try {
            try (FileReader reader = new FileReader(oldTradeStateFile)) {
                JsonObject obj = GSON.fromJson(reader, JsonObject.class);
                VMPlayerData pd = teamData.getPlayerData(playerId);
                if (obj.has("playerCurrency:9")) {
                    JsonArray currencies = obj.getAsJsonArray("playerCurrency:9");
                    for (JsonElement elem : currencies) {
                        int amount = elem.getAsJsonObject()
                            .get("amount:3")
                            .getAsInt();
                        String currencyStr = elem.getAsJsonObject()
                            .get("currency:8")
                            .getAsString();
                        CurrencyType currencyType = CurrencyType.getTypeFromId(currencyStr);
                        if (currencyType != null) {
                            pd.wallet.addCount(currencyType, amount);
                        }
                    }
                }
            }
            FileUtils.moveFile(oldTradeStateFile, new File(dirTradeState, playerId + "_migrated.json"));
            VendingMachine.LOG.info("Successfully migrated trade state for {}", playerId);
            return true;
        } catch (Exception ex) {
            VendingMachine.LOG.error("Unable to migrate trade state for {}", playerId, ex);
        }
        return false;
    }

}
