package com.cubefury.vendingmachine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cubefury.vendingmachine.gui.WidgetThemes;
import com.cubefury.vendingmachine.items.VMItems;
import com.cubefury.vendingmachine.network.PacketTypeRegistry;
import com.cubefury.vendingmachine.network.SerializedPacket;
import com.cubefury.vendingmachine.util.ItemPlaceholder;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;
import cpw.mods.fml.relauncher.Side;

@Mod(
    modid = VendingMachine.MODID,
    version = Tags.VERSION,
    name = VendingMachine.NAME,
    acceptedMinecraftVersions = "[1.7.10]")
public class VendingMachine {

    public static final String MODID = "vendingmachine";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final String CHANNEL = "VM_NET_CHAN";
    public static final String NAME = "Vending Machine";

    @Mod.Instance(MODID)
    public static VendingMachine instance;

    public static boolean isBqLoaded = false;
    public static boolean isGtLoaded = false;
    public static boolean isAeLoaded = false;

    public static int CONTROLLER_MTE_ID = 2741;
    public static int ME_UPLINK_MTE_ID = 2742;

    @SidedProxy(
        clientSide = "com.cubefury.vendingmachine.ClientProxy",
        serverSide = "com.cubefury.vendingmachine.CommonProxy")
    public static CommonProxy proxy;
    public SimpleNetworkWrapper network;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);

        network = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);

        proxy.registerHandlers();
        PacketTypeRegistry.INSTANCE.init();

        // MUI2
        WidgetThemes.init();

        // Register network handlers
        network.registerMessage(SerializedPacket.HandleClient.class, SerializedPacket.class, 0, Side.CLIENT);
        network.registerMessage(SerializedPacket.HandleServer.class, SerializedPacket.class, 0, Side.SERVER);

    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {

        isBqLoaded = Loader.isModLoaded("betterquesting");
        isGtLoaded = Loader.isModLoaded("gregtech");
        isAeLoaded = Loader.isModLoaded("appliedenergistics2");

        LOG.info("Better Questing Integration enabled: {}", isBqLoaded);
        LOG.info("Gregtech Integration enabled: {}", isGtLoaded);
        LOG.info("AE2 Integration enabled {}", isAeLoaded);

        GameRegistry.registerItem(ItemPlaceholder.placeholder, "placeholder");

        if (isGtLoaded) {
            VMItems.registerMultis();
        }

        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {

        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    @Mod.EventHandler
    public void serverStop(FMLServerStoppedEvent event) {}

    @Mod.EventHandler
    public void missingMapping(FMLMissingMappingsEvent event) {
        for (MissingMapping mapping : event.getAll()) {
            if (mapping.type != Type.BLOCK) {
                continue;
            }
        }
    }
}
