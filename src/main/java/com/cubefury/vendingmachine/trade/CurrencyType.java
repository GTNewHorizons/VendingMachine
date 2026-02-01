package com.cubefury.vendingmachine.trade;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.util.Translator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum CurrencyType {

    ADVENTURE("adventure", "dreamcraft:CoinAdventure", "gui/icons/itemCoinAdventure.png"),
    BEES("bees", "dreamcraft:CoinBees", "gui/icons/itemCoinBees.png"),
    BLOOD("blood", "dreamcraft:CoinBlood", "gui/icons/itemCoinBlood.png"),
    CHEMIST("chemist", "dreamcraft:CoinChemist", "gui/icons/itemCoinChemist.png"),
    COOK("cook", "dreamcraft:CoinCook", "gui/icons/itemCoinCook.png"),
    DARK_WIZARD("darkWizard", "dreamcraft:CoinDarkWizard", "gui/icons/itemCoinDarkWizard.png"),
    FARMER("farmer", "dreamcraft:CoinFarmer", "gui/icons/itemCoinFarmer.png"),
    FLOWER("flower", "dreamcraft:CoinFlower", "gui/icons/itemCoinFlower.png"),
    FORESTRY("forestry", "dreamcraft:CoinForestry", "gui/icons/itemCoinForestry.png"),
    SMITH("smith", "dreamcraft:CoinSmith", "gui/icons/itemCoinSmith.png"),
    SPACE("space", "dreamcraft:CoinSpace", "gui/icons/itemCoinSpace.png"),
    SURVIVOR("survivor", "dreamcraft:CoinSurvivor", "gui/icons/itemCoinSurvivor.png"),
    TECHNICIAN("technician", "dreamcraft:CoinTechnician", "gui/icons/itemCoinTechnician.png"),
    WITCH("witch", "dreamcraft:CoinWitch", "gui/icons/itemCoinWitch.png"),
    // comment before semicolon to reduce merge conflicts
    ;

    public final String id;
    public final String itemPrefix;
    public final UITexture texture;

    CurrencyType(String id, String itemPrefix, String texture) {
        this.id = id;
        this.itemPrefix = itemPrefix;
        this.texture = UITexture.builder()
            .location(VendingMachine.MODID, texture)
            .imageSize(32, 32)
            .name("VM_UI_Coin_" + id)
            .build();

        CurrencyItem.typeMap.put(this.id, this);
    }

    public static CurrencyType getTypeFromId(String type) {
        return CurrencyItem.typeMap.get(type);
    }

    @SideOnly(Side.CLIENT)
    public String getLocalizedName() {
        return Translator.translate("vendingmachine.coin." + this.id);
    }
}
