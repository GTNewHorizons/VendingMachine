package com.cubefury.vendingmachine.trade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.util.Translator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum CurrencyType {

    ADVENTURE("adventure", "dreamcraft:CoinAdventure", "Adventure"),
    BEES("bees", "dreamcraft:CoinBees", "Bees"),
    BLOOD("blood", "dreamcraft:CoinBlood", "Blood"),
    CHEMIST("chemist", "dreamcraft:CoinChemist", "Chemist"),
    COOK("cook", "dreamcraft:CoinCook", "Cook"),
    DARK_WIZARD("darkWizard", "dreamcraft:CoinDarkWizard", "DarkWizard"),
    FARMER("farmer", "dreamcraft:CoinFarmer", "Farmer"),
    FLOWER("flower", "dreamcraft:CoinFlower", "Flower"),
    FORESTRY("forestry", "dreamcraft:CoinForestry", "Forestry"),
    SMITH("smith", "dreamcraft:CoinSmith", "Smith"),
    SPACE("space", "dreamcraft:CoinSpace", "Space"),
    SURVIVOR("survivor", "dreamcraft:CoinSurvivor", "Survivor"),
    TECHNICIAN("technician", "dreamcraft:CoinTechnician", "Technician"),
    WITCH("witch", "dreamcraft:CoinWitch", "Witch"),
    // comment before semicolon to reduce merge conflicts
    ;

    public final String id;
    public final String itemPrefix;

    public final UITexture coin1;
    public final UITexture coin10;
    public final UITexture coin100;
    public final UITexture coin1000;
    public final UITexture coin10000;

    CurrencyType(String id, String itemPrefix, String textureName) {
        this.id = id;
        this.itemPrefix = itemPrefix;
        coin1 = createCoinUITexture(textureName, "");
        coin10 = createCoinUITexture(textureName, "I");
        coin100 = createCoinUITexture(textureName, "II");
        coin1000 = createCoinUITexture(textureName, "III");
        coin10000 = createCoinUITexture(textureName, "IV");

        CurrencyItem.typeMap.put(this.id, this);
    }

    private static UITexture createCoinUITexture(String name, String suffix) {
        return UITexture.builder()
            .location("dreamcraft", "items/itemCoin" + name + suffix)
            .imageSize(16, 16)
            .name("VM_UI_Coin_" + name + suffix)
            .build();
    }

    public static CurrencyType getTypeFromId(String type) {
        return CurrencyItem.typeMap.get(type);
    }

    public boolean isMatchingType(ItemStack item) {
        return Item.itemRegistry.getNameForObject(item.getItem())
            .startsWith(itemPrefix);
    }

    @SideOnly(Side.CLIENT)
    public String getLocalizedName() {
        return Translator.translate("vendingmachine.coin." + this.id);
    }
}
