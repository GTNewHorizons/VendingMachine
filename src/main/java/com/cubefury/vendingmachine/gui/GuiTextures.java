package com.cubefury.vendingmachine.gui;

import net.minecraft.util.ResourceLocation;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.drawable.ColorType;
import com.cleanroommc.modularui.drawable.TabTexture;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.VendingMachine;

public final class GuiTextures {

    public static final UITexture OVERLAY_TRADE_DISABLED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/trade_disabled")
        .imageSize(47, 25)
        .canApplyTheme()
        .name("trade_disabled")
        .build();

    public static final UITexture SIDE_PANEL_BACKGROUND = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/panel_side")
        .imageSize(50, 214)
        .canApplyTheme()
        .name("panel_side_background")
        .build();

    public static final UITexture TEXT_FIELD_BACKGROUND = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/text_field_light_gray")
        .imageSize(61, 12)
        .adaptable(1)
        .canApplyTheme()
        .name("text_field_background")
        .build();

    public static final UITexture TILE_TRADE_BUTTON_UNPRESSED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/trade_button_unpressed")
        .canApplyTheme()
        .imageSize(47, 25)
        .name("trade_button_unpressed")
        .build();

    public static final UITexture TILE_TRADE_BUTTON_PRESSED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/trade_button_pressed")
        .canApplyTheme()
        .imageSize(47, 25)
        .name("trade_button_pressed")
        .build();

    public static final UITexture LIST_TRADE_BUTTON_UNPRESSED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/list_trade_button_unpressed")
        .canApplyTheme()
        .imageSize(154, 14)
        .name("list_trade_button_unpressed")
        .build();

    public static final UITexture LIST_TRADE_BUTTON_PRESSED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/list_trade_button_pressed")
        .canApplyTheme()
        .imageSize(154, 14)
        .name("list_trade_button_pressed")
        .build();

    public static final UITexture OVERLAY_TRADEABLE = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/tile_tradeable")
        .imageSize(47, 25)
        .nonOpaque()
        .name("overlay_tradeable")
        .build();

    public static final UITexture OVERLAY_SELECTED = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/tile_selected")
        .imageSize(47, 25)
        .nonOpaque()
        .name("overlay_selected")
        .build();

    public static final UITexture OVERLAY_COOLDOWN = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/tile_cooldown")
        .imageSize(47, 25)
        .nonOpaque()
        .name("overlay_cooldown")
        .build();

    public static final UITexture MODE_TILE = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/mode_tile")
        .imageSize(32, 32)
        .name("mode_tile")
        .build();

    public static final UITexture MODE_LIST = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/mode_list")
        .imageSize(32, 32)
        .name("mode_list")
        .build();

    public static final UITexture SORT_SMART = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/sort_smart")
        .imageSize(32, 32)
        .name("sort_smart")
        .build();

    public static final UITexture SORT_ALPHABET = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/sort_alphabet")
        .imageSize(32, 32)
        .name("sort_alphabet")
        .build();

    public static final UITexture INPUT_SPRITE = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/input")
        .imageSize(30, 20)
        .name("background_input")
        .build();

    public static final UITexture OUTPUT_SPRITE = UITexture.builder()
        .location(VendingMachine.MODID, "gui/background/output")
        .imageSize(30, 20)
        .name("background_output")
        .build();

    public static final UITexture EJECT_COINS = UITexture.builder()
        .location(VendingMachine.MODID, "gui/overlay/coinEject")
        .imageSize(16, 16)
        .name("coin_eject")
        .build();

    public static final TabTexture TAB_LEFT = TabTexture.of(
        UITexture.fullImage(new ResourceLocation(VendingMachine.MODID, "gui/tabs_left"), ColorType.DEFAULT),
        GuiAxis.X,
        false,
        32,
        28,
        4);

    public static final UITexture FAVOURITE_SPRITE = UITexture.builder()
        .location(VendingMachine.MODID, "gui/icons/favourite_indicator")
        .imageSize(16, 16)
        .fullImage()
        .name("favourite_indicator")
        .build();

}
