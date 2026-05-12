package com.cubefury.vendingmachine.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // spotless:off
    VENDING_MACHINE_MUSIC(new MixinBuilder("Helps control game music when the vending machine is opened")
        .addClientMixins("SoundManagerMixin", "SoundManagerAccessor", "SoundHandlerAccessor")
        .setPhase(Phase.EARLY))
    ;
    // spotless:on

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return this.builder;
    }
}
