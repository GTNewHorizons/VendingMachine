package com.cubefury.vendingmachine.blocks.gui;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cubefury.vendingmachine.trade.CurrencyType;

public class CoinButton extends ToggleButton {

    private final TradeMainPanel panel;
    private final CurrencyType type;

    public CoinButton(TradeMainPanel panel, CurrencyType type) {
        super();
        disableThemeBackground(true);
        disableHoverThemeBackground(true);

        this.panel = panel;
        this.type = type;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (!panel.shiftHeld) {
            return Result.IGNORE;
        }
        if (mouseButton == 0) {
            next();
            Interactable.playButtonClickSound();
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

}
