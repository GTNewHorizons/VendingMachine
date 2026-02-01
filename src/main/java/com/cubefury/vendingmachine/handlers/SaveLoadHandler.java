package com.cubefury.vendingmachine.handlers;

import static com.cubefury.vendingmachine.util.FileIO.CopyPaste;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.storage.NameCache;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeManager;
import com.cubefury.vendingmachine.util.FileIO;
import com.cubefury.vendingmachine.util.JsonHelper;
import com.cubefury.vendingmachine.util.NBTConverter;

public class SaveLoadHandler {

    public static SaveLoadHandler INSTANCE = new SaveLoadHandler();

    private File fileDatabase = null;
    private File fileNames = null;
    private File dirTradeState = null;
    private File dirBackupTradeState = null;

    private SaveLoadHandler() {}

    public void init(MinecraftServer server) {
        if (VendingMachine.proxy.isClient()) {
            VMConfig.world_dir = server.getFile("saves/" + server.getFolderName() + "/" + VMConfig.developer.data_dir);
        } else {
            VMConfig.world_dir = server.getFile(server.getFolderName() + "/" + VMConfig.developer.data_dir);
        }

        fileDatabase = new File(VMConfig.developer.trade_db_dir, "tradeDatabase.json");
        dirTradeState = new File(VMConfig.world_dir, "tradeState");
        dirBackupTradeState = new File(VMConfig.world_dir, "backup/tradeState");
        fileNames = new File(VMConfig.world_dir, "names.json");

        createFilesAndDirectories();

        unloadAll();

        loadDatabase();
        loadTradeState();
        loadNames();
    }

    public void createFilesAndDirectories() {
        if (!fileNames.exists()) {
            try {
                if (fileNames.createNewFile()) {
                    VendingMachine.LOG.info("Created new name cache file");
                }
            } catch (Exception ignored) {
                VendingMachine.LOG.warn("Could not create new name cache file");
            }
        }
        if (dirTradeState.mkdirs()) {
            VendingMachine.LOG.info("Created trade state directory");
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

    public void loadTradeState() {
        if (dirTradeState.exists()) {
            File[] fileList = dirTradeState.listFiles();

            if (fileList != null) {
                JsonHelper.populateTradeStateFromFiles(
                    Arrays.stream(fileList)
                        .filter(
                            f -> f.getName()
                                .endsWith(".json"))
                        .collect(Collectors.toList()));
            } else {
                JsonHelper.populateTradeStateFromFiles(Collections.emptyList());
            }
        }
    }

    public List<Future<Void>> writeTradeState(Collection<UUID> players) {
        if (!dirBackupTradeState.exists()) {
            CopyPaste(dirTradeState, dirBackupTradeState);
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (UUID player : players) {
            File playerFile = new File(dirTradeState, player.toString() + ".json");
            CopyPaste(playerFile, new File(dirBackupTradeState, player.toString() + ".json"));
            NBTTagCompound state = TradeManager.INSTANCE.writeTradeStateToNBT(new NBTTagCompound(), player);
            futures.add(FileIO.WriteToFile(playerFile, out -> NBTConverter.NBTtoJSON_Compound(state, out, true)));
        }
        return futures;
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
        TradeManager.INSTANCE.clearTradeState(null);
    }

    public void reloadDatabase() {
        TradeDatabase.INSTANCE.clear();
        TradeManager.INSTANCE.clearTradeState(null);

        loadDatabase();
        loadTradeState();
    }

}
