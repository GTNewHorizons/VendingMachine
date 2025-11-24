package com.cubefury.vendingmachine;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

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

    private static String DISPLAY_TYPE_COMMENT = "Default trade display format, either TILE or LIST. Case sensitive.";
    private static String SORT_MODE_COMMENT = "Default sort mode, either SMART or ALPHABET. Case sensitive.";

    public static File worldDir = null;

    public static void init(File configFile) {
        if (configuration == null) {
            configuration = new Configuration(configFile);
            loadConfiguration(false);
        }
    }

    private static void loadConfiguration(boolean rewriteConfig) {
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

        Property display_type_prop = configuration
            .get(CONFIG_CATEGORY_VM, "display_type", "TILE", DISPLAY_TYPE_COMMENT);
        try {
            display_type = DisplayType.valueOf(display_type_prop.getString());
        } catch (IllegalArgumentException e) {
            VendingMachine.LOG.warn("Invalid display type: {}, defaulting to TILE", display_type_prop.getString());
            display_type = DisplayType.TILE;
        }
        display_type_prop.set(display_type.toString());
        display_type_prop.comment = DISPLAY_TYPE_COMMENT;

        Property sort_mode_prop = configuration.get(CONFIG_CATEGORY_VM, "sort_mode", "SMART", SORT_MODE_COMMENT);
        try {
            sort_mode = MTEVendingMachineGui.SortMode.valueOf(sort_mode_prop.getString());
        } catch (IllegalArgumentException e) {
            VendingMachine.LOG.warn("Invalid sort mode: {}, defaulting to SMART", sort_mode_prop.getString());
            sort_mode = MTEVendingMachineGui.SortMode.SMART;
        }
        sort_mode_prop.set(sort_mode.toString());
        sort_mode_prop.comment = SORT_MODE_COMMENT;

        if (configuration.hasChanged() || rewriteConfig) {
            configuration.save();
        }
    }

    @SubscribeEvent
    public void onConfigChangeEvent(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equalsIgnoreCase(VendingMachine.MODID)) {
            VendingMachine.LOG.info("config changed");
            loadConfiguration(true);
        }
    }
}
