package com.cubefury.vendingmachine.blocks.gui.coin;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
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
    private final ToggleButton coinButton;

    private final Icon coinIcon1, coinIcon10, coinIcon100, coinIcon1000, coinIcon10000;

    private int textColor = 0;
    private int oldCoinValue = -1;
    private long lastCoinChange = -1;
    private boolean coinIncreased;

    public CoinDisplay(TradeMainPanel panel, CurrencyType type, PanelSyncManager syncManager) {
        super(GuiAxis.X);

        coinIcon1 = type.coin1.asIcon()
            .size(12);
        coinIcon10 = type.coin10.asIcon()
            .size(12);
        coinIcon100 = type.coin100.asIcon()
            .size(12);
        coinIcon1000 = type.coin1000.asIcon()
            .size(12);
        coinIcon10000 = type.coin10000.asIcon()
            .size(12);

        coinSyncValue = syncManager.findSyncHandler("coinAmount_" + type.id, 0, IntSyncValue.class);
        this.child(
            coinButton = new CoinButton(panel, type).overlay(coinIcon1)
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

    private Icon getCoinButtonIcon() {
        if (oldCoinValue < 10) return coinIcon1;
        if (oldCoinValue < 100) return coinIcon10;
        if (oldCoinValue < 1000) return coinIcon100;
        if (oldCoinValue < 10000) return coinIcon1000;
        return coinIcon10000;
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
            coinButton.overlay(getCoinButtonIcon());
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
