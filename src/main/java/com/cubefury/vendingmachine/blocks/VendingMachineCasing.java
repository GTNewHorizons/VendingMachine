package com.cubefury.vendingmachine.blocks;

import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.common.blocks.BlockCasingsAbstract;
import gregtech.common.blocks.ItemCasings;
import gregtech.common.blocks.MaterialCasings;

public class VendingMachineCasing extends BlockCasingsAbstract {

    public VendingMachineCasing(String aName) {
        super(ItemCasings.class, aName, MaterialCasings.INSTANCE, 1);
    }

    @Override
    public int getTextureIndex(int aMeta) {
        return (16 << 7) | (aMeta + 64);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int meta) {
        return Textures.BlockIcons.MACHINE_CASING_ITEM_PIPE_TIN.getIcon();
    }
}
