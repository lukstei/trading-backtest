package org.lst.trading.main.strategy;

import org.lst.trading.lib.model.TradingStrategy;

public abstract class AbstractTradingStrategy implements TradingStrategy {
    double mWeight = 1;

    public double getWeight() {
        return mWeight;
    }

    public void setWeight(double weight) {
        mWeight = weight;
    }
}
