package com.cubefury.vendingmachine;

import java.io.File;

import com.cubefury.vendingmachine.blocks.gui.MTEVendingMachineGui;
import com.cubefury.vendingmachine.blocks.gui.DisplayType;
import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = VendingMachine.MODID, category = "vendingmachine", filename = "vendingmachine")
public class VMConfig {

    @Config.Comment("Vending Machine Settings")
    public static final VendingMachineSettings vendingMachineSettings = new VendingMachineSettings();

    @Config.Comment("GUI Settings")
    public static final GUI gui = new GUI();

    @Config.Comment("Developer Settings")
    public static final Developer developer = new Developer();

    public static class VendingMachineSettings {

        @Config.Comment("How often the vending machine refreshes its data, in number of ticks")
        @Config.DefaultInt(20)
        @Config.RequiresWorldRestart
        public int gui_refresh_interval;

        @Config.Comment("How often items items are ejected, in number of ticks")
        @Config.DefaultInt(10)
        @Config.RequiresWorldRestart
        public int dispense_frequency;

        @Config.Comment("How many items are dispensed from the queue at once")
        @Config.DefaultInt(16)
        @Config.RequiresWorldRestart
        public int dispense_amount;
    }

    public static class GUI {

        @Config.Comment("Default trade display format, either TILE or LIST. Case sensitive.")
        @Config.DefaultEnum("TILE")
        public DisplayType display_type = DisplayType.TILE;

        @Config.Comment("Default sort mode, either SMART or ALPHABET. Case sensitive.")
        @Config.DefaultEnum("SMART")
        public MTEVendingMachineGui.SortMode sort_mode = MTEVendingMachineGui.SortMode.SMART;
    }

    public static class Developer {

        @Config.Comment("subdirectory for vending machine data in world save")
        @Config.DefaultString("vendingmachine")
        @Config.RequiresMcRestart
        public String data_dir;

        @Config.Comment("folder where trade database file is located")
        @Config.DefaultString("config/vendingmachine")
        @Config.RequiresMcRestart
        public String trade_db_dir = "config/vendingmachine";

        @Config.Comment("Force rewrite database on load, for add/remove trades or change of format")
        @Config.DefaultBoolean(false)
        @Config.RequiresWorldRestart
        public boolean force_rewrite_database = false;
    }

    @Config.Ignore
    public static File world_dir = null;
}
