package com.cubefury.vendingmachine;

import java.util.concurrent.Callable;

import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;

import com.cubefury.vendingmachine.command.CommandVending;
import com.cubefury.vendingmachine.handlers.EventHandler;
import com.cubefury.vendingmachine.handlers.SaveLoadHandler;
import com.cubefury.vendingmachine.handlers.ServerTaskScheduler;
import com.google.common.util.concurrent.ListenableFuture;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;

public class CommonProxy {

    private ServerTaskScheduler taskScheduler;

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        VendingMachine.LOG.info("Loading Vending Machine " + Tags.VERSION);
        this.registerHandlers();
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {}

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {}

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ICommandManager command = server.getCommandManager();
        ServerCommandManager manager = (ServerCommandManager) command;

        manager.registerCommand(new CommandVending());

        SaveLoadHandler.INSTANCE.init(server);
        this.taskScheduler = new ServerTaskScheduler(Thread.currentThread());
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        this.taskScheduler = null;
    }

    public boolean isClient() {
        return false;
    }

    protected void registerHandlers() {
        final EventHandler handler = new EventHandler();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
    }

    public <T> ListenableFuture<T> scheduleServerTask(Callable<T> callable) {
        if (this.taskScheduler != null) {
            return this.taskScheduler.scheduleServerTask(callable);
        }
        return null;
    }
}
