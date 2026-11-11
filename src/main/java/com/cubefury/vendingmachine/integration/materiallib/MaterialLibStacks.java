package com.cubefury.vendingmachine.integration.materiallib;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.api.StackResolver;

/// Resolves the `ml:<material>:<shape>` references trade entries carry. Such a reference names its item by material
/// and shape instead of by the id and metadata MaterialLib assigns afresh each session.
///
/// Sole holder of MaterialLib API references, keeping them out of classes that load while MaterialLib is absent.
/// Callers check [com.cubefury.vendingmachine.VendingMachine#isMaterialLibLoaded] first.
public final class MaterialLibStacks {

    private MaterialLibStacks() {}

    /// A single-item stack of what an `ml:<material>:<shape>` reference names, or null when the reference is malformed
    /// or names nothing MaterialLib registers.
    public static ItemStack resolve(String reference) {
        String[] parts = reference.split(":");
        if (parts.length != 3) {
            return null;
        }
        return StackResolver.getStack(parts[1], parts[2], 1);
    }
}
