package org.lst.trading.main.strategy;

import org.lst.trading.lib.model.Order;
import org.lst.trading.lib.model.TradingContext;
import org.lst.trading.lib.model.TradingStrategy;

import java.util.HashMap;
import java.util.Map;

public class BuyAndHold implements TradingStrategy {
    Map<String, Order> mOrders;
    TradingContext mContext;

    @Override public void onStart(TradingContext context) {
        mContext = context;
    }

    @Override public void onTick() {
        if (mOrders == null) {
            mOrders = new HashMap<>();
            mContext.getInstruments().stream().forEach(instrument -> mOrders.put(instrument, mContext.order(instrument, true, 1)));
        }
    }
}
