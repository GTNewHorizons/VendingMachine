package com.cubefury.vendingmachine.trade;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.util.Translator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public enum CurrencyType {

    ADVENTURE("adventure", "dreamcraft:CoinAdventure", "adventure"),
    BEES("bees", "dreamcraft:CoinBees", "bee"),
    BLOOD("blood", "dreamcraft:CoinBlood", "bm"),
    CHEMIST("chemist", "dreamcraft:CoinChemist", "chemist"),
    COOK("cook", "dreamcraft:CoinCook", "cook"),
    DARK_WIZARD("darkWizard", "dreamcraft:CoinDarkWizard", "wizard"),
    FARMER("farmer", "dreamcraft:CoinFarmer", "farmer"),
    FLOWER("flower", "dreamcraft:CoinFlower", "gardener"),
    FORESTRY("forestry", "dreamcraft:CoinForestry", "ranger"),
    SMITH("smith", "dreamcraft:CoinSmith", "builder"),
    SPACE("space", "dreamcraft:CoinSpace", "space"),
    SURVIVOR("survivor", "dreamcraft:CoinSurvivor", "survivor"),
    TECHNICIAN("technician", "dreamcraft:CoinTechnician", "technician"),
    WITCH("witch", "dreamcraft:CoinWitch", "witch"),
    // comment before semicolon to reduce merge conflicts
    ;

    public final String id;
    public final String itemPrefix;

    public final UITexture coinBackground1;
    public final UITexture coinBackground10;
    public final UITexture coinBackground100;
    public final UITexture coinBackground1000;
    public final UITexture coinBackground10000;

    public final UITexture coinIcon1;
    public final UITexture coinIcon10;
    public final UITexture coinIcon100;
    public final UITexture coinIcon1000;
    public final UITexture coinIcon10000;

    CurrencyType(String id, String itemPrefix, String textureName) {
        this.id = id;
        this.itemPrefix = itemPrefix;
        coinBackground1 = createCoinUITexture("background/small0");
        coinBackground10 = createCoinUITexture("background/small1");
        coinBackground100 = createCoinUITexture("background/small2");
        coinBackground1000 = createCoinUITexture("background/small3");
        coinBackground10000 = createCoinUITexture("background/small4");

        coinIcon1 = createCoinUITexture("icons/" + textureName + "0");
        coinIcon10 = createCoinUITexture("icons/" + textureName + "1");
        coinIcon100 = createCoinUITexture("icons/" + textureName + "2");
        coinIcon1000 = createCoinUITexture("icons/" + textureName + "3");
        coinIcon10000 = createCoinUITexture("icons/" + textureName + "4");

        CurrencyItem.typeMap.put(this.id, this);
    }

    private static UITexture createCoinUITexture(String name) {
        return UITexture.builder()
            .location("dreamcraft", "items/coins/" + name)
            .imageSize(16, 16)
            .name("VM_UI_Coin_" + name.replaceAll("/", "_"))
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
