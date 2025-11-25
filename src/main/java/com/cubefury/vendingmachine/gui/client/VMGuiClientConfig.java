package com.cubefury.vendingmachine.gui.client;

import net.minecraft.client.gui.GuiScreen;

import com.cubefury.vendingmachine.VMConfig;
import com.cubefury.vendingmachine.VendingMachine;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

public class VMGuiClientConfig extends SimpleGuiConfig {

    public VMGuiClientConfig(GuiScreen parentScreen) throws ConfigException {
        super(parentScreen, VendingMachine.MODID, VendingMachine.NAME, true, VMConfig.class);
    }
}
