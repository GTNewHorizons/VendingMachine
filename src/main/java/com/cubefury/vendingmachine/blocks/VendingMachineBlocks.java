package com.cubefury.vendingmachine.blocks;

public class VendingMachineBlocks {

    public static VendingMachineCasing casingBlock;

    private VendingMachineBlocks() {}

    public static void registerBlocks() {
        casingBlock = new VendingMachineCasing("casing");
    }

}
