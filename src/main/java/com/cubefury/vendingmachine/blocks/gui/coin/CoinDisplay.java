package com.cubefury.vendingmachine.blocks.gui.coin;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cubefury.vendingmachine.blocks.gui.TradeMainPanel;
import com.cubefury.vendingmachine.trade.CurrencyType;
import com.cubefury.vendingmachine.util.Translator;

public class CoinDisplay extends Flow {

    private static final int COIN_ANIM_TIME = 200;
    private static final float DEFAULT_AMOUNT_SCALE = 0.8f;
    private static final float AMOUNT_SCALE_OFFSET = 0.1f;

    private final IntSyncValue coinSyncValue;
    private final TextWidget<?> coinAmount;

    private int textColor = 0;
    private int oldCoinValue = -1;
    private long lastCoinChange = -1;
    private boolean coinIncreased;

    public CoinDisplay(TradeMainPanel panel, CurrencyType type, PanelSyncManager syncManager) {
        super(GuiAxis.X);
        coinSyncValue = syncManager.findSyncHandler("coinAmount_" + type.id, 0, IntSyncValue.class);
        this.child(
            new CoinButton(panel, type).overlay(
                type.texture.asIcon()
                    .size(12))
                .size(12)
                .left(0)
                .syncHandler("ejectCoin_" + type.id)
                .tooltipDynamic((builder) -> {
                    builder.clearText();
                    builder.addLine(coinSyncValue.getValue() + " " + type.getLocalizedName());
                    builder.emptyLine();
                    builder.addLine(
                        IKey.str(Translator.translate("vendingmachine.gui.single_coin_type_eject_hint"))
                            .style(IKey.GRAY, IKey.ITALIC));
                    builder.setAutoUpdate(true);
                }))
            .child(
                coinAmount = IKey.dynamic(() -> getReadableStringFromCoinAmount(coinSyncValue.getValue()))
                    .scale(DEFAULT_AMOUNT_SCALE)
                    .asWidget()
                    .top(3)
                    .left(14)
                    .width(21)
                    .color(() -> textColor))
            .height(14);
    }

    private static String getReadableStringFromCoinAmount(int amount) {
        if (amount < 10000) {
            return "" + amount;
        } else if (amount < 1000000) {
            return amount / 1000 + "K";
        } else {
            return amount / 1000000 + "M";
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int val = coinSyncValue.getValue();
        textColor = widgetTheme.getTheme()
            .getTextColor();
        if (val == 0) {
            textColor = Color.lerp(
                textColor,
                widgetTheme.getTheme()
                    .getColor(),
                0.2f);
        }
        if (val != oldCoinValue) {
            if (oldCoinValue != -1) {
                lastCoinChange = System.currentTimeMillis();
                coinIncreased = val > oldCoinValue;
            }
            oldCoinValue = val;
        }
        long now = System.currentTimeMillis();
        int diff = (int) (now - lastCoinChange);
        float coinAmountScale = DEFAULT_AMOUNT_SCALE;
        if (diff > 0 && diff < COIN_ANIM_TIME) {
            coinAmountScale = serp(
                coinAmountScale,
                coinIncreased ? AMOUNT_SCALE_OFFSET : -AMOUNT_SCALE_OFFSET,
                (float) diff / COIN_ANIM_TIME);
        }
        coinAmount.scale(coinAmountScale);
        super.draw(context, widgetTheme);
    }

    private static float serp(float start, float offset, float t) {
        return offset * (-4 * (t - 0.5f) * (t - 0.5f) + 1) + start;
    }
}
