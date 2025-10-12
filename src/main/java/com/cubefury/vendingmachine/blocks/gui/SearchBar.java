package com.cubefury.vendingmachine.blocks.gui;

import java.awt.Point;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.TextFieldTheme;
import com.cleanroommc.modularui.widgets.textfield.BaseTextFieldWidget;
import com.cubefury.vendingmachine.gui.GuiTextures;
import com.cubefury.vendingmachine.util.Translator;

public class SearchBar extends BaseTextFieldWidget<SearchBar> {

    private MTEVendingMachineGui gui;
    private String previousText;

    public SearchBar(MTEVendingMachineGui gui) {
        super();

        this.gui = gui;

        background(GuiTextures.TEXT_FIELD_BACKGROUND);
        hoverBackground(GuiTextures.TEXT_FIELD_BACKGROUND);
        setText("");
        this.previousText = "";
        hintText(Translator.translate("vendingmachine.gui.search"));
    }

    @Override
    public void onFocus(ModularGuiContext context) {
        super.onFocus(context);
        Point main = this.handler.getMainCursor();
        if (main.x == 0) {
            this.handler.setCursor(main.y, getText().length(), true, true);
        }
    }

    @Override
    public boolean canHover() {
        return true;
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        if (
            hasTooltip() && getScrollData().isScrollBarActive(getScrollArea())
                && isHoveringFor(getTooltip().getShowUpTimer())
        ) {
            getTooltip().draw(getContext());
        }
    }

    @NotNull
    public String getText() {
        if (
            this.handler.getText()
                .isEmpty()
        ) {
            return "";
        }
        if (
            this.handler.getText()
                .size() > 1
        ) {
            throw new IllegalStateException("TextFieldWidget can only have one line!");
        }
        return this.handler.getText()
            .get(0);
    }

    @Override
    protected void setupDrawText(ModularGuiContext context, TextFieldTheme widgetTheme) {
        this.renderer.setSimulate(false);
        this.renderer.setPos(
            getArea().getPadding()
                .getLeft(),
            0);
        this.renderer.setScale(this.scale);
        this.renderer.setAlignment(this.textAlignment, -1, getArea().height);
    }

    public void setText(@NotNull String text) {
        if (
            this.handler.getText()
                .isEmpty()
        ) {
            this.handler.getText()
                .add(text);
        } else {
            this.handler.getText()
                .set(0, text);
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        String curText = getText();
        if (!curText.equals(previousText)) {
            MTEVendingMachineGui.lastSearch = curText;
            gui.setForceRefresh();
        }
        previousText = curText;
    }
}
