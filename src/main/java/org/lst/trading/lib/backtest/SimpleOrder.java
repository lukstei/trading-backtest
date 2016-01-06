package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.Order;

import java.time.Instant;

class SimpleOrder implements Order {
    int mId;
    int mAmount;
    double mOpenPrice;
    Instant mOpenInstant;
    String mInstrument;

    public SimpleOrder(int id, String instrument, Instant openInstant, double openPrice, int amount) {
        mId = id;
        mInstrument = instrument;
        mOpenInstant = openInstant;
        mOpenPrice = openPrice;
        mAmount = amount;
    }

    @Override public int getId() {
        return mId;
    }

    @Override public int getAmount() {
        return mAmount;
    }

    @Override public double getOpenPrice() {
        return mOpenPrice;
    }

    @Override public Instant getOpenInstant() {
        return mOpenInstant;
    }

    @Override public String getInstrument() {
        return mInstrument;
    }
}
