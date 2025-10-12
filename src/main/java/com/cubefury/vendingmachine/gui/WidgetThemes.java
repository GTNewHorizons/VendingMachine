package com.cubefury.vendingmachine.gui;

import com.cleanroommc.modularui.api.IThemeApi;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Color;

public final class WidgetThemes {

    public static final String BACKGROUND_SIDEPANEL = "background_side_panel";

    public static void register() {
        IThemeApi themeApi = IThemeApi.get();
        registerThemedTexture(themeApi, BACKGROUND_SIDEPANEL, GuiTextures.SIDE_PANEL_BACKGROUND, 0, 0);
    }

    private static void registerThemedTexture(IThemeApi themeApi, String textureThemeId, UITexture background,
        int defaultWidth, int defaultHeight) {
        themeApi.registerWidgetTheme(
            textureThemeId,
            new WidgetTheme(defaultWidth, defaultHeight, background, Color.WHITE.main, 0xFF404040, false, 0),
            new WidgetTheme(defaultWidth, defaultHeight, background, Color.WHITE.main, 0xFF404040, false, 0),
            WidgetTheme::new);
    }
}
