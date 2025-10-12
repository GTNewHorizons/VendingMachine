package com.cubefury.vendingmachine.gui;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.theme.WidgetThemeKey;
import com.cleanroommc.modularui.utils.Color;

public final class WidgetThemes {

    public static void init() {}

    private static final IThemeApi themeApi = IThemeApi.get();

    public static final WidgetThemeKey<WidgetTheme> BACKGROUND_SIDEPANEL = themeApi
        .widgetThemeKeyBuilder("background_side_panel", WidgetTheme.class)
        .defaultTheme(new WidgetTheme(0, 0, GuiTextures.SIDE_PANEL_BACKGROUND, Color.WHITE.main, 0xFF404040, false, 0))
        .defaultHoverTheme(null)
        .register();

    public static final WidgetThemeKey<TextFieldTheme> BACKGROUND_SEARCH_BAR = themeApi
        .widgetThemeKeyBuilder("background_search_bar", TextFieldTheme.class)
        .defaultTheme(
            new TextFieldTheme(
                0,
                0,
                GuiTextures.TEXT_FIELD_BACKGROUND,
                Color.WHITE.main,
                0xFF404040,
                false,
                0,
                0,
                0xFF404040))
        .defaultHoverTheme(null)
        .register();

    public static final WidgetThemeKey<WidgetTheme> THEME_TRADE_BUTTON = themeApi
        .widgetThemeKeyBuilder("background_tile_trade_button", WidgetTheme.class)
        .defaultTheme(new WidgetTheme(0, 0, null, Color.WHITE.main, 0xFF404040, false, 0))
        .defaultHoverTheme(null)
        .register();
}
