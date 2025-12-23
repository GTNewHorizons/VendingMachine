package com.cubefury.vendingmachine.trade;

public class TradeHistory {

    public long lastTrade = -1;
    public int tradeCount = 0;
    public boolean notificationQueued = false;

    public static TradeHistory DEFAULT = new TradeHistory();

    public TradeHistory() {}

    public TradeHistory(long lastTrade, int tradeCount, boolean notificationQueued) {
        this.lastTrade = lastTrade;
        this.tradeCount = tradeCount;
        this.notificationQueued = notificationQueued;
    }

    public void executeTrade(int maxTrades, boolean hasCooldown) {
        lastTrade = System.currentTimeMillis();
        tradeCount += 1;
        notificationQueued = hasCooldown && (maxTrades == -1 || tradeCount < maxTrades);
    }

    public void setNotified() {
        notificationQueued = false;
    }

    public void resetData() {
        lastTrade = -1;
        tradeCount = 0;
        notificationQueued = false;
    }

    public void resetTradeAvailability(boolean notifyPlayer) {
        lastTrade = -1;
        notificationQueued = notifyPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeHistory that)) return false;
        return lastTrade == that.lastTrade && tradeCount == that.tradeCount;
    }
}
