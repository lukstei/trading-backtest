package org.lst.trading.lib.strategy;

import org.lst.trading.lib.model.TradingStrategy;
import org.lst.trading.lib.model.TradingContext;

import java.util.ArrayList;
import java.util.List;

public class MultipleTradingStrategy implements TradingStrategy {
    public static MultipleTradingStrategy of(TradingStrategy... strategies) {
        MultipleTradingStrategy strategy = new MultipleTradingStrategy();
        for (TradingStrategy s : strategies) {
            strategy.add(s);
        }
        return strategy;
    }

    List<TradingStrategy> mStrategies = new ArrayList<>();

    public boolean add(TradingStrategy strategy) {
        return mStrategies.add(strategy);
    }

    public int size() {
        return mStrategies.size();
    }

    @Override public void onStart(TradingContext context) {
        mStrategies.forEach(t -> t.onStart(context));
    }

    @Override public void onTick() {
        mStrategies.forEach(TradingStrategy::onTick);
    }

    @Override public void onEnd() {
        mStrategies.forEach(TradingStrategy::onEnd);
    }

    @Override public String toString() {
        return "MultipleStrategy{" +
                "mStrategies=" + mStrategies +
                '}';
    }
}
