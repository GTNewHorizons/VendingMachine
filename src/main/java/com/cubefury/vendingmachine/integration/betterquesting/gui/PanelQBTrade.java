package com.cubefury.vendingmachine.integration.betterquesting.gui;

import com.cubefury.vendingmachine.trade.CurrencyItem;
import com.cubefury.vendingmachine.trade.Trade;

import betterquesting.api.utils.BigItemStack;
import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiRectangle;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.CanvasMinimum;
import betterquesting.api2.client.gui.panels.content.PanelGeneric;
import betterquesting.api2.client.gui.panels.content.PanelItemSlot;
import betterquesting.api2.client.gui.resources.textures.IGuiTexture;
import betterquesting.api2.client.gui.themes.presets.PresetIcon;
import bq_standard.client.gui.panels.content.PanelItemSlotBuilder;

public class PanelQBTrade extends CanvasMinimum {

    private static final IGuiTexture RIGHT_ARROW_TEX = PresetIcon.ICON_RIGHT.getTexture();
    private final Trade trade;

    public PanelQBTrade(IGuiRect rect, Trade trade) {
        super(rect);
        this.trade = trade;
    }

    @Override
    public void initPanel() {
        super.initPanel();

        int x_offset = 0;

        for (int i = 0; i < trade.fromItems.size(); i++) {
            BigItemStack stack = trade.fromItems.get(i)
                .toBQBigItemStack();
            GuiRectangle rectangle = new GuiRectangle(x_offset, 0, 18, 18, 0);
            PanelItemSlot is = PanelItemSlotBuilder.forValue(stack, rectangle)
                .showCount(true)
                .build();
            this.addPanel(is);
            x_offset += 20;
        }

        for (int i = 0; i < trade.nonConsumedItems.size(); i++) {
            BigItemStack stack = trade.nonConsumedItems.get(i)
                .toBQBigItemStack();
            GuiRectangle rectangle = new GuiRectangle(x_offset, 0, 18, 18, 0);
            PanelItemSlot is = PanelItemSlotBuilder.forValue(stack, rectangle)
                .showCount(true)
                .build();
            this.addPanel(is);
            x_offset += 20;
        }

        for (int i = 0; i < trade.fromCurrency.size(); i++) {
            CurrencyItem currencyItem = trade.fromCurrency.get(i);
            BigItemStack stack = new BigItemStack(currencyItem.getItemRepresentation());
            GuiRectangle rectangle = new GuiRectangle(x_offset, 0, 18, 18, 0);
            PanelItemSlot is = PanelItemSlotBuilder.forValue(stack, rectangle)
                .showCount(true)
                .build();
            this.addPanel(is);
            x_offset += 20;
        }

        // +1px buffer on each side to regularize slightly smaller icon size (16px vs 18px of items)
        this.addPanel(
            new PanelGeneric(
                new GuiTransform(GuiAlign.TOP_LEFT, x_offset + 1, 1, 16, 16, 0),
                PresetIcon.ICON_RIGHT.getTexture()));
        x_offset += 20;

        for (int i = 0; i < trade.toItems.size(); i++) {
            BigItemStack stack = trade.toItems.get(i)
                .toBQBigItemStack();
            GuiRectangle rectangle = new GuiRectangle(x_offset, 0, 18, 18, 0);
            PanelItemSlot is = PanelItemSlotBuilder.forValue(stack, rectangle)
                .showCount(true)
                .build();
            this.addPanel(is);
            x_offset += 20;
        }

        recalcSizes();
    }
}
