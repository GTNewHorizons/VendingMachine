package com.cubefury.vendingmachine.integration.materiallib;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.api.StackResolver;

/// Sole holder of MaterialLib API references, so that no other class can pull them in while MaterialLib is absent.
/// Every caller checks [com.cubefury.vendingmachine.VendingMachine#isMaterialLibLoaded] before entering a method that
/// reaches this class.
public final class MaterialLibStacks {

    private MaterialLibStacks() {}

    /// The stack an `ml:<material>:<shape>` reference names, or null when the reference is malformed or names nothing
    /// MaterialLib registers. MaterialLib logs the reason for a miss.
    public static ItemStack resolve(String reference, int amount) {
        String[] parts = reference.split(":");
        if (parts.length != 3) {
            return null;
        }
        return StackResolver.getStack(parts[1], parts[2], amount);
    }
}
