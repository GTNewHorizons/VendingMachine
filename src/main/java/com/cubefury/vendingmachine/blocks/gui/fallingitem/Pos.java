package com.cubefury.vendingmachine.blocks.gui.fallingitem;

import com.cleanroommc.modularui.animation.IAnimatable;
import com.cleanroommc.modularui.utils.Interpolations;

public class Pos implements IAnimatable<Pos> {

    private int x, y;

    public Pos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public Pos interpolate(Pos start, Pos end, float t) {
        this.x = Interpolations.lerp(start.x, end.x, t);
        this.y = Interpolations.lerp(start.y, end.y, t);
        return this;
    }

    @Override
    public Pos copyOrImmutable() {
        return new Pos(x, y);
    }

    public void set(Pos other) {
        this.x = other.x;
        this.y = other.y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
