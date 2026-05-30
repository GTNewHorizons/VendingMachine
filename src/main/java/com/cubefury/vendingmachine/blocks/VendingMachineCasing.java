package com.cubefury.vendingmachine.blocks;

import net.minecraft.util.IIcon;

import com.cubefury.vendingmachine.api.enums.Textures;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.common.blocks.BlockCasingsAbstract;
import gregtech.common.blocks.ItemCasings;
import gregtech.common.blocks.MaterialCasings;

public class VendingMachineCasing extends BlockCasingsAbstract {

    public VendingMachineCasing(String aName) {
        super(ItemCasings.class, aName, MaterialCasings.INSTANCE, 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Textures.VM_CASING.getIcon();
    }
}
