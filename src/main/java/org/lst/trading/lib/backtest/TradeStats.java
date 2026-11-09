package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.ClosedOrder;

import java.util.ArrayList;
import java.util.List;

// round trip stats, p/l before commissions
public class TradeStats {
    int mOrderCount;
    List<Double> mTradePls = new ArrayList<>();
    int mWins;
    int mLosses;
    double mGrossProfit;
    double mGrossLoss;

    public TradeStats(List<ClosedOrder> orders) {
        mOrderCount = orders.size();

        // group legs into one trade
        ClosedOrder prev = null;
        for (ClosedOrder order : orders) {
            if (prev != null && isSameTrade(prev, order)) {
                int last = mTradePls.size() - 1;
                mTradePls.set(last, mTradePls.get(last) + order.getPl());
            } else {
                mTradePls.add(order.getPl());
            }
            prev = order;
        }

        // flat trades count as neither
        for (double pl : mTradePls) {
            if (pl > 0) {
                mWins++;
                mGrossProfit += pl;
            } else if (pl < 0) {
                mLosses++;
                mGrossLoss += pl;
            }
        }
    }

    static boolean isSameTrade(ClosedOrder a, ClosedOrder b) {
        return a.getOpenInstant().equals(b.getOpenInstant()) && a.getCloseInstant().equals(b.getCloseInstant());
    }

    public int getTradeCount() {
        return mTradePls.size();
    }

    public int getOrderCount() {
        return mOrderCount;
    }

    public int getWins() {
        return mWins;
    }

    public int getLosses() {
        return mLosses;
    }

    public double getWinRate() {
        if (getTradeCount() == 0) {
            return Double.NaN;
        }
        return (double) mWins / getTradeCount();
    }

    public double getGrossProfit() {
        return mGrossProfit;
    }

    public double getGrossLoss() {
        return mGrossLoss;
    }

    public double getProfitFactor() {
        if (mGrossLoss < 0) {
            return mGrossProfit / Math.abs(mGrossLoss);
        }
        // no losing trades
        if (mGrossProfit > 0) {
            return Double.POSITIVE_INFINITY;
        }
        return Double.NaN;
    }

    public double getAverageWin() {
        if (mWins == 0) {
            return Double.NaN;
        }
        return mGrossProfit / mWins;
    }

    public double getAverageLoss() {
        if (mLosses == 0) {
            return Double.NaN;
        }
        return mGrossLoss / mLosses;
    }
}
