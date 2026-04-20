package com.cubefury.vendingmachine.blocks.gui;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

public class TradeRow extends Flow {

    public TradeRow() {
        super(GuiAxis.X);
        this.collapseDisabledChild(true)
            .setEnabledIf(
                r -> r.getChildren()
                    .stream()
                    .anyMatch(IWidget::isEnabled));
    }
}
