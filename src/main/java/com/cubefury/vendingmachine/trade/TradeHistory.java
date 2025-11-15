package com.cubefury.vendingmachine.trade;

public class TradeHistory {

    public long lastTrade = -1;
    public int tradeCount = 0;

    public static TradeHistory DEFAULT = new TradeHistory();

    public TradeHistory() {}

    public TradeHistory(long lastTrade, int tradeCount) {
        this.lastTrade = lastTrade;
        this.tradeCount = tradeCount;
    }

    public void executeTrade() {
        lastTrade = System.currentTimeMillis();
        tradeCount += 1;
    }

    public void resetData() {
        lastTrade = -1;
        tradeCount = 0;
    }

    public void resetTradeAvailability() {
        lastTrade = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TradeHistory that)) return false;
        return lastTrade == that.lastTrade && tradeCount == that.tradeCount;
    }
}
