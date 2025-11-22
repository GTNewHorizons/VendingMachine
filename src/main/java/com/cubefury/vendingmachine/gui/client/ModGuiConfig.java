package com.cubefury.vendingmachine.gui.client;

import static com.cubefury.vendingmachine.Config.CONFIG_CATEGORY_DEVELOPER;
import static com.cubefury.vendingmachine.Config.CONFIG_CATEGORY_VM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;

import com.cubefury.vendingmachine.Config;
import com.cubefury.vendingmachine.VendingMachine;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ModGuiConfig extends GuiConfig {

    public ModGuiConfig(GuiScreen guiScreen) {
        super(guiScreen, getConfigElements(), VendingMachine.MODID, false, false, VendingMachine.NAME);
    }

    private static List<IConfigElement> getConfigElements() {
        List<IConfigElement> configElements = new ArrayList<>();

        List<String> topCategories = Arrays.asList(CONFIG_CATEGORY_VM, CONFIG_CATEGORY_DEVELOPER);

        for (String categoryName : topCategories) {
            ConfigCategory category = Config.configuration.getCategory(categoryName);
            configElements.add(new ConfigElement(category));
        }
        return configElements;
    }
}
