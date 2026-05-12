package com.cubefury.vendingmachine;

import java.io.File;

import com.cubefury.vendingmachine.blocks.gui.DisplayType;
import com.cubefury.vendingmachine.blocks.gui.MusicTrack;
import com.cubefury.vendingmachine.blocks.gui.SortMode;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = VendingMachine.MODID, category = "vendingmachine", filename = "vendingmachine")
public class VMConfig {

    @Config.Comment("Vending Machine Settings")
    public static final VendingMachineSettings vendingMachineSettings = new VendingMachineSettings();

    @Config.Comment("GUI Settings")
    public static final GUI gui = new GUI();

    @Config.Comment("Team Settings")
    public static final TeamSettings team = new TeamSettings();

    @Config.Comment("Music Settings")
    public static final Music music = new Music();

    @Config.Comment("Developer Settings")
    public static final Developer developer = new Developer();

    public static class VendingMachineSettings {

        @Config.Comment("How often the vending machine refreshes its data, in number of ticks")
        @Config.DefaultInt(20)
        @Config.RequiresWorldRestart
        public int gui_refresh_interval;

        @Config.Comment("Enable restock notifications, disabling on server will disable notifications for everyone")
        @Config.DefaultBoolean(true)
        @Config.RequiresWorldRestart
        public boolean restock_notifications_enabled;

        @Config.Comment("How often the server checks for restocked trades, in number of ticks.")
        @Config.DefaultInt(200)
        @Config.RequiresWorldRestart
        public int restock_notifications_interval;

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
        public SortMode sort_mode = SortMode.SMART;

        @Config.Comment("Default wallet mode, either PERSONAL or TEAM. Case sensitive.")
        @Config.DefaultEnum("PERSONAL")
        public WalletMode wallet_mode = WalletMode.PERSONAL;
    }

    public static class TeamSettings {

        @Config.Comment("Allows using the team wallet in solo teams")
        @Config.DefaultBoolean(false)
        public boolean soloTeam = false;
    }

    public static class Music {

        @Config.Comment("Music track to play in the vending machine, either NONE or LUNCH_BREAK. Case sensitive.")
        @Config.DefaultEnum("LUNCH_BREAK")
        public MusicTrack current_track = MusicTrack.LUNCH_BREAK;

        @Config.Comment("Volume of the vending machine music")
        @Config.DefaultFloat(0.75f)
        @Config.RangeFloat(min = 0, max = 2)
        public float music_volume = 0.75f;
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
