package com.cubefury.vendingmachine.blocks.gui;

import static com.cubefury.vendingmachine.gui.GuiTextures.MODE_LIST;
import static com.cubefury.vendingmachine.gui.GuiTextures.MODE_TILE;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.value.IValue;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.DynamicDrawable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.Icon;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.value.sync.GenericSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.widgets.ItemDisplayWidget;
import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.gui.GuiTextures;
import com.cubefury.vendingmachine.gui.WidgetThemes;
import com.cubefury.vendingmachine.util.Translator;

public class TradeItemDisplayWidget extends ItemDisplayWidget implements Interactable {

    public enum DisplayType {

        TILE("tile", MODE_TILE),
        LIST("list", MODE_LIST);

        private final String type;
        private final Icon texture;

        DisplayType(String type, UITexture texture) {
            this.type = type;
            this.texture = texture.asIcon();
        }

        public String getLocalizedName() {
            return IKey.lang("vendingmachine.gui.display_mode_" + this.type)
                .toString();
        }

        public Icon getTexture() {
            return this.texture;
        }
    }

    private MTEVendingMachine vm;
    private TradeMainPanel rootPanel;
    private boolean pressed = false;
    private IValue<ItemStack> value;
    public final DisplayType displayType;

    private TradeItemDisplay display;

    public TradeItemDisplayWidget(TradeItemDisplay display, MTEVendingMachine base, DisplayType displayType) {
        this.vm = base;
        this.displayType = displayType;
        widgetTheme(WidgetThemes.THEME_TRADE_BUTTON);
        if (displayType == DisplayType.TILE) {
            height(MTEVendingMachineGui.TILE_ITEM_HEIGHT);
            width(MTEVendingMachineGui.TILE_ITEM_WIDTH);
            background(
                new DynamicDrawable(
                    () -> pressed ? GuiTextures.TILE_TRADE_BUTTON_PRESSED : GuiTextures.TILE_TRADE_BUTTON_UNPRESSED));
        } else if (displayType == DisplayType.LIST) {
            height(MTEVendingMachineGui.LIST_ITEM_HEIGHT);
            width(MTEVendingMachineGui.LIST_ITEM_WIDTH);
            background(
                new DynamicDrawable(
                    () -> pressed ? GuiTextures.LIST_TRADE_BUTTON_PRESSED : GuiTextures.LIST_TRADE_BUTTON_UNPRESSED));
        }

        this.display = display;
        this.item((ItemStack) null);
    }

    public void setDisplay(TradeItemDisplay display) {
        this.display = display;
        this.item(display == null ? null : display.display);
    }

    private boolean checkVmActive() {
        return this.vm != null && this.vm.getActive();
    }

    public TradeItemDisplay getDisplay() {
        return this.display;
    }

    public @NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (rootPanel.shiftHeld) {
            rootPanel.attemptPurchase(this.display);
            pressed = true;
            return Result.SUCCESS;
        }
        return Result.IGNORE;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        int textColor = Translator.getColor("vendingmachine.gui.display_text_color");
        ItemStack item = value.getValue();
        if (!Platform.isStackEmpty(item)) {
            if (this.displayType == DisplayType.TILE) {
                GuiDraw.drawText(" " + this.display.display.stackSize, 4, 9, 1.0f, textColor, false);
                GuiDraw.drawItem(item, 26, 4, 16, 16, context.getCurrentDrawingZ());
                if (this.display.tradeableNow) {
                    GuiDraw.drawOutline(1, 1, 45, 23, 0x883CFF00, 2);
                }
                if (!this.checkVmActive() || this.display.hasCooldown || !this.display.enabled) {
                    GuiDraw.drawRoundedRect(
                        1,
                        1,
                        MTEVendingMachineGui.TILE_ITEM_WIDTH - 2,
                        MTEVendingMachineGui.TILE_ITEM_HEIGHT - 2,
                        0xBB000000,
                        1,
                        1);
                }
                this.overlay(
                    IKey.str(display.hasCooldown ? this.display.cooldownText : "")
                        .style(IKey.WHITE));
            } else if (this.displayType == DisplayType.LIST) {
                GuiDraw.drawText("" + this.display.display.stackSize, 6, 4, 0.9f, textColor, false);
                GuiDraw.drawItem(item, 24, 2, 9, 9, context.getCurrentDrawingZ());
                GuiDraw.drawText(
                    this.display.display.getDisplayName()
                        .length() > 21
                            ? this.display.display.getDisplayName()
                                .substring(0, 21) + "..."
                            : this.display.display.getDisplayName(),
                    36,
                    4,
                    0.9f,
                    textColor,
                    false);
                GuiDraw.drawRect(
                    1,
                    1,
                    3,
                    MTEVendingMachineGui.LIST_ITEM_HEIGHT - 3,
                    this.display.tradeableNow ? 0x883CFF00 : 0x88333333);
                if (!this.checkVmActive() || this.display.hasCooldown || !this.display.enabled) {
                    GuiDraw.drawRect(
                        1,
                        1,
                        MTEVendingMachineGui.LIST_ITEM_WIDTH - 2,
                        MTEVendingMachineGui.LIST_ITEM_HEIGHT - 2,
                        0xBB000000);
                }
                this.overlay(
                    IKey.str(display.hasCooldown && this.display.enabled ? this.display.cooldownText : "")
                        .style(IKey.WHITE)
                        .scale(0.9f));
            }
        }
    }

    @Override
    public boolean isValidSyncHandler(SyncHandler syncHandler) {
        if (syncHandler instanceof GenericSyncValue<?>genericSyncValue && genericSyncValue.isOfType(ItemStack.class)) {
            this.value = genericSyncValue.cast();
            return true;
        }
        return false;
    }

    public ItemDisplayWidget item(IValue<ItemStack> itemSupplier) {
        this.value = itemSupplier;
        setValue(itemSupplier);
        return this;
    }

    @Override
    public void onMouseEndHover() {
        pressed = false;
        super.onMouseEndHover();
    }

    @Override
    public boolean onMouseRelease(int mouseButton) {
        pressed = false;
        return true;
    }

    public void setRootPanel(TradeMainPanel rootPanel) {
        this.rootPanel = rootPanel;
    }
}
