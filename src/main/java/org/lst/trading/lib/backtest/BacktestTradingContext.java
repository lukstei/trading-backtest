package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.ClosedOrder;
import org.lst.trading.lib.model.Order;
import org.lst.trading.lib.model.TradingContext;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.series.TimeSeries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.lst.trading.lib.util.Util.check;

class BacktestTradingContext implements TradingContext {
    Instant mInstant;
    List<Double> mPrices;
    List<String> mInstruments;
    DoubleSeries mPl = new DoubleSeries("pl");
    DoubleSeries mFundsHistory = new DoubleSeries("funds");
    MultipleDoubleSeries mHistory;
    double mInitialFunds;
    double mCommissions;

    int mOrderId = 1;

    List<SimpleOrder> mOrders = new ArrayList<>();

    double mClosedPl = 0;
    List<SimpleClosedOrder> mClosedOrders = new ArrayList<>();
    double mLeverage;

    @Override public Instant getTime() {
        return mInstant;
    }

    @Override public double getLastPrice(String instrument) {
        return mPrices.get(mInstruments.indexOf(instrument));
    }

    @Override public Stream<TimeSeries.Entry<Double>> getHistory(String instrument) {
        int index = mInstruments.indexOf(instrument);
        return mHistory.reversedStream().map(t -> new TimeSeries.Entry<>(t.getItem().get(index), t.getInstant()));
    }

    @Override public Order order(String instrument, boolean buy, int amount) {
        check(amount > 0);

        double price = getLastPrice(instrument);
        SimpleOrder order = new SimpleOrder(mOrderId++, instrument, getTime(), price, amount * (buy ? 1 : -1));
        mOrders.add(order);

        mCommissions += calculateCommission(order);

        return order;
    }

    @Override public ClosedOrder close(Order order) {
        SimpleOrder simpleOrder = (SimpleOrder) order;
        mOrders.remove(simpleOrder);
        double price = getLastPrice(order.getInstrument());
        SimpleClosedOrder closedOrder = new SimpleClosedOrder(simpleOrder, price, getTime());
        mClosedOrders.add(closedOrder);
        mClosedPl += closedOrder.getPl();
        mCommissions += calculateCommission(order);

        return closedOrder;
    }

    @Override public double getPl() {
        return mClosedPl + mOrders.stream().mapToDouble(t -> t.calculatePl(getLastPrice(t.getInstrument()))).sum() - mCommissions;
    }

    @Override public List<String> getInstruments() {
        return mInstruments;
    }

    @Override public double getAvailableFunds() {
        return getNetValue() - mOrders.stream().mapToDouble(t -> Math.abs(t.getAmount()) * t.getOpenPrice() / mLeverage).sum();
    }

    @Override public double getInitialFunds() {
        return mInitialFunds;
    }

    @Override public double getNetValue() {
        return mInitialFunds + getPl();
    }

    @Override public double getLeverage() {
        return mLeverage;
    }

    double calculateCommission(Order order) {
        return 1 + Math.abs(order.getAmount()) * 0.005;
    }
}

