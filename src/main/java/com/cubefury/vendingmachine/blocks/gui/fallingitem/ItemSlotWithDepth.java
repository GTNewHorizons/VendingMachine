package com.cubefury.vendingmachine.blocks.gui.fallingitem;

import org.lwjgl.opengl.GL11;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;

public class ItemSlotWithDepth extends ItemSlot {

    private final int depth;

    public ItemSlotWithDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        GL11.glTranslatef(0f, 0f, depth);
        super.draw(context, widgetTheme);
        GL11.glTranslatef(0f, 0f, -depth);
    }
}
