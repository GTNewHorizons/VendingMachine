package com.cubefury.vendingmachine.integration.betterquesting.gui;

import java.util.UUID;

import com.cubefury.vendingmachine.trade.Trade;
import com.cubefury.vendingmachine.trade.TradeDatabase;
import com.cubefury.vendingmachine.trade.TradeGroup;

import betterquesting.api2.client.gui.misc.GuiAlign;
import betterquesting.api2.client.gui.misc.GuiTransform;
import betterquesting.api2.client.gui.misc.IGuiRect;
import betterquesting.api2.client.gui.panels.CanvasEmpty;
import betterquesting.api2.client.gui.panels.IGuiPanel;
import betterquesting.api2.client.gui.panels.lists.CanvasScrolling;

public class BqTradeGroup {

    public static int addTradePanel(CanvasScrolling csReward, IGuiRect rectReward, UUID tradeGroup, int yOffset) {
        TradeGroup tg = TradeDatabase.INSTANCE.getTradeGroups()
            .get(tradeGroup);
        if (tg == null || tg.getTrades() == null) {
            return yOffset;
        }
        for (Trade trade : tg.getTrades()) {
            IGuiPanel tradeGui = trade.getTradeGui(
                new GuiTransform(GuiAlign.FULL_BOX, 0, 0, rectReward.getWidth(), rectReward.getHeight(), 111));
            if (tradeGui != null) {
                tradeGui.initPanel();
                CanvasEmpty tempCanvas = new CanvasEmpty(
                    new GuiTransform(
                        GuiAlign.TOP_LEFT,
                        0,
                        yOffset,
                        rectReward.getWidth(),
                        tradeGui.getTransform()
                            .getHeight()
                            - tradeGui.getTransform()
                                .getY(),
                        1));
                csReward.addPanel(tempCanvas);
                tempCanvas.addPanel(tradeGui);
                yOffset += tempCanvas.getTransform()
                    .getHeight();
            }
        }
        return yOffset;
    }
}
