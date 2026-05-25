package com.cubefury.vendingmachine.blocks.gui.fallingitem;

import static com.cubefury.vendingmachine.blocks.MTEVendingMachine.OUTPUT_SLOTS;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.cleanroommc.modularui.animation.Animator;
import com.cleanroommc.modularui.animation.MutableObjectAnimator;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Interpolation;
import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import com.cleanroommc.modularui.widgets.TransformWidget;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;

public class FallingItemSlotFactory {

    private static final int FALL_ANIMATION_DURATION = 1000;
    private static final int MAX_X_POS = 24;
    private static final List<Integer> OUTPUT_SLOT_X_POSITIONS;

    private final ItemStackHandler outputItems;
    private final int fallDistance;

    static {
        Random random = new Random(OUTPUT_SLOTS);
        OUTPUT_SLOT_X_POSITIONS = random.ints(OUTPUT_SLOTS, 0, MAX_X_POS)
            .boxed()
            .collect(Collectors.toList());
    }

    public FallingItemSlotFactory(ItemStackHandler outputItems, int fallDistance) {
        this.outputItems = outputItems;
        this.fallDistance = fallDistance;
    }

    public TransformWidget getFallingItemSlot(int index) {
        final Pos fallingPosition = new Pos(OUTPUT_SLOT_X_POSITIONS.get(index), this.fallDistance);
        Animator fallingPositionAnimation = fallingItemAnimation(fallingPosition);
        IWidget widget = getFallingItemSlot(index, fallingPositionAnimation);
        return new TransformWidget(widget)
            .transform(stack -> stack.translate((float) fallingPosition.getX(), (float) fallingPosition.getY()));
    }

    private IWidget getFallingItemSlot(int index, Animator animator) {
        return new ItemSlotWithDepth(index).slot(
            new ModularSlot(this.outputItems, index).accessibility(false, true)
                .slotGroup("outputSlotGroup")
                .changeListener((newitem, onlyAmountChanged, client, init) -> {
                    if (!init && newitem != null && !onlyAmountChanged) {
                        animator.reset();
                        animator.animate();
                    }
                }))
            .background(IDrawable.EMPTY)
            .disableHoverBackground()
            .setEnabledIf(
                slot -> slot.getSlot()
                    .getHasStack());
    }

    private static Animator fallingItemAnimation(Pos animatedPos) {
        return new MutableObjectAnimator<>(animatedPos, new Pos(animatedPos.getX(), 0), animatedPos.copyOrImmutable())
            .bounds(0, 1)
            .curve(Interpolation.BOUNCE_OUT)
            .duration(FallingItemSlotFactory.FALL_ANIMATION_DURATION);
    }
}
