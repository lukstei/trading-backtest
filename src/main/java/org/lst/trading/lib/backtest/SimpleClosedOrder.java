package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.ClosedOrder;

import java.time.Instant;

class SimpleClosedOrder implements ClosedOrder {
    SimpleOrder mOrder;
    double mClosePrice;
    Instant mCloseInstant;
    double mPl;

    public SimpleClosedOrder(SimpleOrder order, double closePrice, Instant closeInstant) {
        mOrder = order;
        mClosePrice = closePrice;
        mCloseInstant = closeInstant;
        mPl = calculatePl(mClosePrice);
    }

    @Override public int getId() {
        return mOrder.getId();
    }

    @Override public double getClosePrice() {
        return mClosePrice;
    }

    @Override public Instant getCloseInstant() {
        return mCloseInstant;
    }

    @Override public double getPl() {
        return mPl;
    }

    @Override public boolean isLong() {
        return mOrder.isLong();
    }

    @Override public int getAmount() {
        return mOrder.getAmount();
    }

    @Override public double getOpenPrice() {
        return mOrder.getOpenPrice();
    }

    @Override public Instant getOpenInstant() {
        return mOrder.getOpenInstant();
    }

    @Override public String getInstrument() {
        return mOrder.getInstrument();
    }
}
