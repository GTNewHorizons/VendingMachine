package com.cubefury.vendingmachine.trade;

public class TradeHistory {

    public long lastTrade = -1;
    public int tradeCount = 0;
    public int cooldownTradeCount = 0;
    public boolean notificationQueued = false;

    public static final TradeHistory DEFAULT = new TradeHistory();

    public TradeHistory() {}

    public TradeHistory(long lastTrade, int tradeCount, int cooldownTradeCount, boolean notificationQueued) {
        this.lastTrade = lastTrade;
        this.tradeCount = tradeCount;
        this.cooldownTradeCount = cooldownTradeCount;
        this.notificationQueued = notificationQueued;
    }

    public void executeTrade(int maxTrades, int cooldown) {
        boolean hasCooldown = cooldown != -1;
        boolean cooldownOver = (System.currentTimeMillis() - lastTrade) / 1000 >= cooldown;
        lastTrade = System.currentTimeMillis();
        tradeCount += 1;
        cooldownTradeCount = cooldownOver ? 1 : cooldownTradeCount + 1;
        notificationQueued = hasCooldown && (maxTrades == -1 || tradeCount < maxTrades);
    }

    public void setNotified() {
        notificationQueued = false;
    }

    public void resetData() {
        lastTrade = -1;
        tradeCount = 0;
        cooldownTradeCount = 0;
        notificationQueued = false;
    }

    public void resetTradeAvailability(boolean notifyPlayer) {
        lastTrade = -1;
        cooldownTradeCount = 0;
        notificationQueued = notifyPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeHistory that)) return false;
        return lastTrade == that.lastTrade && tradeCount == that.tradeCount
            && cooldownTradeCount == that.cooldownTradeCount;
    }

    public TradeHistory copy() {
        return new TradeHistory(lastTrade, tradeCount, cooldownTradeCount, notificationQueued);
    }

    public static TradeHistory merge(TradeHistory th1, TradeHistory th2) {
        return new TradeHistory(
            Math.max(th1.lastTrade, th2.lastTrade),
            th1.tradeCount + th2.tradeCount,
            th1.cooldownTradeCount + th2.cooldownTradeCount,
            false);
    }
}
