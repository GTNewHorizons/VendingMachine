package com.cubefury.vendingmachine.integration.nei;

import com.cubefury.vendingmachine.Tags;
import com.cubefury.vendingmachine.VendingMachine;
import com.cubefury.vendingmachine.items.VMItems;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;
import codechicken.nei.event.NEIRegisterHandlerInfosEvent;
import codechicken.nei.recipe.HandlerInfo;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class NEIConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        NeiRecipeHandler handler = new NeiRecipeHandler();
        API.addRecipeCatalyst(VMItems.vendingMachine, "vendingmachine", 0);
        handler.addHandler();
    }

    @Override
    public String getName() {
        return VendingMachine.NAME;
    }

    @Override
    public String getVersion() {
        return Tags.VERSION;
    }

    @SubscribeEvent
    public void registerHandlerInfo(NEIRegisterHandlerInfosEvent event) {
        event.registerHandlerInfo(
            new HandlerInfo.Builder("vendingmachine", VendingMachine.NAME, VendingMachine.MODID).setHeight(104)
                .setWidth(166)
                .setMaxRecipesPerPage(2)
                .setDisplayStack(VMItems.vendingMachine)
                .build());
    }
}
