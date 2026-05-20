package com.cubefury.vendingmachine.blocks.gui;

import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.PageButton;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cubefury.vendingmachine.gui.GuiTextures;
import com.cubefury.vendingmachine.trade.TradeCategory;

public class VendingPageButton extends PageButton {

    private final static int ICON_MARGIN = 6;

    private final int index;
    private final Icon tabIcon;

    public VendingPageButton(int index, PagedWidget.Controller controller, List<TradeCategory> tradeCategories,
        Set<TradeCategory> highlightedTabs) {
        super(index, controller);

        this.index = index;

        overlay(new DynamicDrawable(() -> {
            if (highlightedTabs.contains(tradeCategories.get(index))) {
                return GuiTextures.TAB_HIGHLIGHT.asIcon()
                    .size(20, 20);
            }
            return IDrawable.EMPTY;
        }),
            tabIcon = tradeCategories.get(index)
                .getTexture()
                .asIcon()
                .margin(ICON_MARGIN)
                .center());
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        MTEVendingMachineGui.lastPage = this.index;
        return super.onMousePressed(mouseButton);
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        if (isHovering()) {
            tabIcon.marginLeft(ICON_MARGIN - 1)
                .marginRight(ICON_MARGIN + 1);
        } else {
            tabIcon.marginLeft(ICON_MARGIN)
                .marginRight(ICON_MARGIN);
        }
        super.draw(context, widgetTheme);
    }
}
