package com.cubefury.vendingmachine.trade;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import com.cleanroommc.modularui.drawable.UITexture;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.util.Translator;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CurrencyItem {

    public CurrencyType type;
    public int value;
    private static final Map<String, CurrencyType> typeMap = new HashMap<>();

    public static CurrencyItem fromNBT(NBTTagCompound nbt) {
        CurrencyType type = CurrencyType.getTypeFromId(nbt.getString("type"));
        if (type == null) {
            return null;
        }
        return new CurrencyItem(type, nbt.getInteger("value"));
    }

    public void writeToNBT(NBTTagCompound payload) {
        payload.setString("type", this.type.id);
        payload.setInteger("value", this.value);
    }

    public enum CurrencyType {

        ADVENTURE("adventure", "dreamcraft:item.CoinAdventure", "gui/icons/itemCoinAdventure.png"),
        BEES("bees", "dreamcraft:item.CoinBees", "gui/icons/itemCoinBees.png"),
        BLOOD("blood", "dreamcraft:item.CoinBlood", "gui/icons/itemCoinBlood.png"),
        CHEMIST("chemist", "dreamcraft:item.CoinChemist", "gui/icons/itemCoinChemist.png"),
        COOK("cook", "dreamcraft:item.CoinCook", "gui/icons/itemCoinCook.png"),
        DARK_WIZARD("darkWizard", "dreamcraft:item.CoinDarkWizard", "gui/icons/itemCoinDarkWizard.png"),
        FARMER("farmer", "dreamcraft:item.CoinFarmer", "gui/icons/itemCoinFarmer.png"),
        FLOWER("flower", "dreamcraft:item.CoinFlower", "gui/icons/itemCoinFlower.png"),
        FORESTRY("forestry", "dreamcraft:item.CoinForestry", "gui/icons/itemCoinForestry.png"),
        SMITH("smith", "dreamcraft:item.CoinSmith", "gui/icons/itemCoinSmith.png"),
        SPACE("space", "dreamcraft:item.CoinSpace", "gui/icons/itemCoinSpace.png"),
        SURVIVOR("survivor", "dreamcraft:item.CoinSurvivor", "gui/icons/itemCoinSurvivor.png"),
        TECHNICIAN("technician", "dreamcraft:item.CoinTechnician", "gui/icons/itemCoinTechnician.png"),
        WITCH("witch", "dreamcraft:item.CoinWitch", "gui/icons/itemCoinWitch.png"),
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

            typeMap.put(this.id, this);
        }

        public static CurrencyType getTypeFromId(String type) {
            return typeMap.get(type);
        }

        @SideOnly(Side.CLIENT)
        public String getLocalizedName() {
            return Translator.translate("vendingmachine.coin." + this.id);
        }
    }

    public CurrencyItem(CurrencyType type, int value) {
        this.type = type;
        this.value = value;
    }

    public static CurrencyItem fromItemStack(ItemStack newItem) {
        String itemName = Item.itemRegistry.getNameForObject(newItem.getItem());
        for (CurrencyType type : CurrencyType.values()) {
            if (itemName.startsWith(type.itemPrefix)) {
                int currencyValue = mapSuffixToValue(itemName.substring(type.itemPrefix.length()));
                if (currencyValue < 0) {
                    return null;
                }
                return new CurrencyItem(type, currencyValue * newItem.stackSize);
            }
        }
        return null;
    }

    private static int mapSuffixToValue(String suffix) {
        return switch (suffix) {
            case "" -> 1;
            case "I" -> 10;
            case "II" -> 100;
            case "III" -> 1000;
            case "IV" -> 10000;
            default -> -1;
        };
    }
}
