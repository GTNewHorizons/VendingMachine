package com.cubefury.vendingmachine.trade;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.cubefury.vendingmachine.blocks.MTEVendingMachine;
import com.cubefury.vendingmachine.blocks.gui.WalletMode;

public class TradeRequest {

    public EntityPlayerMP player;
    public UUID playerID;
    public UUID tradeGroup;
    public int tradeGroupOrder;
    public WalletMode walletMode;
    MTEVendingMachine target;

    public TradeRequest(EntityPlayerMP player, UUID playerID, UUID tradeGroup, int tradeGroupOrder,
        WalletMode walletMode, MTEVendingMachine target) {
        this.player = player;
        this.playerID = playerID;
        this.tradeGroup = tradeGroup;
        this.tradeGroupOrder = tradeGroupOrder;
        this.walletMode = walletMode;
        this.target = target;
    }
}
