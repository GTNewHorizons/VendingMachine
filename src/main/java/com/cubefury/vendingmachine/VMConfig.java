package com.cubefury.vendingmachine;

import java.io.File;

import com.cubefury.vendingmachine.blocks.gui.DisplayType;
import com.cubefury.vendingmachine.blocks.gui.MusicTrack;
import com.cubefury.vendingmachine.blocks.gui.SortMode;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;
import com.gtnewhorizon.gtnhlib.config.Config;

@Config(modid = VendingMachine.MODID, category = "vendingmachine", filename = "vendingmachine")
@Config.LangKey("vendingmachine.config.title.main")
public class VMConfig {

    @Config.LangKey("vendingmachine.config.title.main_settings")
    public static final VendingMachineSettings vendingMachineSettings = new VendingMachineSettings();

    @Config.LangKey("vendingmachine.config.title.gui_settings")
    public static final GUI gui = new GUI();

    @Config.LangKey("vendingmachine.config.title.team_settings")
    public static final TeamSettings team = new TeamSettings();

    @Config.LangKey("vendingmachine.config.title.music_settings")
    public static final Music music = new Music();

    @Config.LangKey("vendingmachine.config.title.dev_settings")
    public static final Developer developer = new Developer();

    public static class VendingMachineSettings {

        @Config.LangKey("vendingmachine.config.main.data_refresh")
        @Config.DefaultInt(20)
        @Config.RequiresWorldRestart
        public int gui_refresh_interval;

        @Config.LangKey("vendingmachine.config.main.restock")
        @Config.DefaultBoolean(true)
        @Config.RequiresWorldRestart
        public boolean restock_notifications_enabled;

        @Config.LangKey("vendingmachine.config.main.restock_check")
        @Config.DefaultInt(200)
        @Config.RequiresWorldRestart
        public int restock_notifications_interval;
    }

    public static class GUI {

        @Config.LangKey("vendingmachine.config.gui.trade_display")
        @Config.DefaultEnum("TILE")
        public DisplayType display_type = DisplayType.TILE;

        @Config.LangKey("vendingmachine.config.gui.sort_mode")
        @Config.DefaultEnum("SMART")
        public SortMode sort_mode = SortMode.SMART;

        @Config.LangKey("vendingmachine.config.gui.wallet_mode")
        @Config.DefaultEnum("PERSONAL")
        public WalletMode wallet_mode = WalletMode.PERSONAL;

        @Config.LangKey("vendingmachine.config.gui.show_coins")
        @Config.DefaultBoolean(true)
        public boolean show_coins = true;

        @Config.Comment("Update coin icon depending on amount.")
        @Config.DefaultBoolean(false)
        public boolean update_coin_icon = false;
    }

    public static class TeamSettings {

        @Config.LangKey("vendingmachine.config.team.allow_in_teams")
        @Config.DefaultBoolean(false)
        public boolean soloTeam = false;

        @Config.LangKey("vendingmachine.config.team.cooldown_increase")
        @Config.DefaultInt(-1)
        public int maxTradeLimit = -1;
    }

    public static class Music {

        @Config.LangKey("vendingmachine.config.music.track")
        @Config.DefaultEnum("LUNCH_BREAK")
        public MusicTrack current_track = MusicTrack.LUNCH_BREAK;

        @Config.LangKey("vendingmachine.config.music.volume")
        @Config.DefaultFloat(0.75f)
        @Config.RangeFloat(min = 0, max = 2)
        public float music_volume = 0.75f;
    }

    public static class Developer {

        @Config.LangKey("vendingmachine.config.dev.subdirectory")
        @Config.DefaultString("vendingmachine")
        @Config.RequiresMcRestart
        public String data_dir;

        @Config.LangKey("vendingmachine.config.dev.folder")
        @Config.DefaultString("config/vendingmachine")
        @Config.RequiresMcRestart
        public String trade_db_dir = "config/vendingmachine";

        @Config.LangKey("vendingmachine.config.dev.database")
        @Config.DefaultBoolean(false)
        @Config.RequiresWorldRestart
        public boolean force_rewrite_database = false;
    }

    @Config.Ignore
    public static File world_dir = null;
}
