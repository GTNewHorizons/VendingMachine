package com.cubefury.vendingmachine;

import net.minecraftforge.common.MinecraftForge;

import com.cubefury.vendingmachine.handlers.ClientEventHandler;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.integration.nei.NEIConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    // Override CommonProxy methods here, if you want a different behaviour on the client (e.g. registering renders).
    // Don't forget to call the super methods as well.
    @Override
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new NEIConfig());
        super.init(event);
        SaveLoadHandler.INSTANCE.clientInit();
    }

    public boolean isClient() {
        return true;
    }

    @Override
    public void registerHandlers() {
        super.registerHandlers();
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ClientEventHandler.INSTANCE);
    }

}
