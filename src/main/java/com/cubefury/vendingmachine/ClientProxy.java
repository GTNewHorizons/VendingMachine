package com.cubefury.vendingmachine;

import net.minecraftforge.common.MinecraftForge;

import com.cubefury.vendingmachine.handlers.ClientEventHandler;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.integration.nei.NEIConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

public final class ClientProxy extends CommonProxy {

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.
    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new NEIConfig());
        super.init(event);
        SaveLoadHandler.INSTANCE.clientInit();
    }

    @Override
    public boolean isClient() {
        return true;
    }

    @Override
    protected void registerHandlers() {
        super.registerHandlers();
        final ClientEventHandler handler = new ClientEventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }

}
