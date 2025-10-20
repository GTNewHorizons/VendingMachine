package com.cubefury.vendingmachine.api.enums;

import gregtech.api.enums.Textures.BlockIcons.CustomIcon;
import gregtech.api.interfaces.IIconContainer;

public class Textures {

    public static final CustomIcon VM_MACHINE_FRONT_OFF = new CustomIcon("vendingmachine:vending_machine_front_off"),
        VM_MACHINE_FRONT_ON = new CustomIcon("vendingmachine:vending_machine_front_on"),
        VM_MACHINE_FRONT_ON_GLOW = new CustomIcon("vendingmachine:vending_machine_front_on_glow"),

        VM_OVERLAY_0 = new CustomIcon("vendingmachine:vending_machine_overlay_0"),
        VM_OVERLAY_1 = new CustomIcon("vendingmachine:vending_machine_overlay_1"),
        VM_OVERLAY_2 = new CustomIcon("vendingmachine:vending_machine_overlay_2"),
        VM_OVERLAY_3 = new CustomIcon("vendingmachine:vending_machine_overlay_3"),
        VM_OVERLAY_4 = new CustomIcon("vendingmachine:vending_machine_overlay_4"),

        VM_OVERLAY_ACTIVE_0 = new CustomIcon("vendingmachine:vending_machine_overlay_active_0"),
        VM_OVERLAY_ACTIVE_1 = new CustomIcon("vendingmachine:vending_machine_overlay_active_1"),
        VM_OVERLAY_ACTIVE_2 = new CustomIcon("vendingmachine:vending_machine_overlay_active_2"),
        VM_OVERLAY_ACTIVE_3 = new CustomIcon("vendingmachine:vending_machine_overlay_active_3"),

        VUPLINK_OVERLAY_INACTIVE = new CustomIcon("vendingmachine:vending_uplink_machine_overlay_inactive"),
        VUPLINK_OVERLAY_ACTIVE = new CustomIcon("vendingmachine:vending_uplink_machine_overlay_active");

    public static final IIconContainer[] VM_OVERLAY_ACTIVE = { VM_OVERLAY_ACTIVE_0, VM_OVERLAY_ACTIVE_1,
        VM_OVERLAY_ACTIVE_2, VM_OVERLAY_ACTIVE_3, VM_OVERLAY_4 // bottom right not animated
    };

    public static final IIconContainer[] VM_OVERLAY = { VM_OVERLAY_0, VM_OVERLAY_1, VM_OVERLAY_2, VM_OVERLAY_3,
        VM_OVERLAY_4 };

}
