package com.cubefury.vendingmachine;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import com.cubefury.vendingmachine.blocks.gui.MTEVendingMachineGui;
import com.cubefury.vendingmachine.blocks.gui.TradeItemDisplayWidget.DisplayType;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class Config {

    public static Configuration configuration;

    public static final String CONFIG_CATEGORY_VM = "vending machine settings";
    public static final String CONFIG_CATEGORY_DEVELOPER = "developer settings";

    public static String data_dir = "vendingmachine";
    public static String config_dir = "config/vendingmachine";
    public static int gui_refresh_interval = 20;
    public static int dispense_frequency = 10;
    public static int dispense_amount = 16;
    public static DisplayType display_type = DisplayType.TILE;
    public static MTEVendingMachineGui.SortMode sort_mode = MTEVendingMachineGui.SortMode.SMART;
    public static boolean forceRewriteDatabase = false;

    public static File worldDir = null;

    public static void init(File configFile) {
        if (configuration == null) {
            configuration = new Configuration(configFile);
            loadConfiguration();
        }
    }

    private static void loadConfiguration() {
        data_dir = configuration
            .getString("data_dir", Configuration.CATEGORY_GENERAL, data_dir, "World vendingmachine data directory");
        config_dir = configuration
            .getString("config_dir", Configuration.CATEGORY_GENERAL, config_dir, "Configuration directory");

        configuration.addCustomCategoryComment(CONFIG_CATEGORY_VM, "Vending Machine Settings");
        gui_refresh_interval = configuration
            .getInt("gui_refresh_interval", CONFIG_CATEGORY_VM, gui_refresh_interval, 20, 3600, "In number of ticks");
        dispense_frequency = configuration
            .getInt("dispense_frequency", CONFIG_CATEGORY_VM, dispense_frequency, 1, 9000, "In number of ticks");
        dispense_amount = configuration.getInt(
            "dispense_amount",
            CONFIG_CATEGORY_VM,
            dispense_amount,
            1,
            Integer.MAX_VALUE,
            "Number of items per dispense cycle");

        configuration.addCustomCategoryComment(CONFIG_CATEGORY_DEVELOPER, "Developer Settings");
        forceRewriteDatabase = configuration.getBoolean(
            "force_rewrite_database",
            CONFIG_CATEGORY_DEVELOPER,
            forceRewriteDatabase,
            "Force rewrite database on load, for add/remove trades or change of format");

        try {
            display_type = DisplayType.valueOf(
                configuration.getString(
                    "display_type",
                    CONFIG_CATEGORY_VM,
                    "TILE",
                    "Default trade display format, either TILE or LIST. Case sensitive."));
        } catch (IllegalArgumentException e) {
            display_type = DisplayType.TILE;
        }
        try {
            sort_mode = MTEVendingMachineGui.SortMode.valueOf(
                configuration.getString(
                    "sort_mode",
                    CONFIG_CATEGORY_VM,
                    "SMART",
                    "Default sort mode, either SMART or ALPHABET. Case sensitive."));
        } catch (IllegalArgumentException e) {
            sort_mode = MTEVendingMachineGui.SortMode.SMART;
        }

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }

    @SubscribeEvent
    public void onConfigChangeEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equalsIgnoreCase(VendingMachine.MODID)) {
            loadConfiguration();
        }
    }
}
