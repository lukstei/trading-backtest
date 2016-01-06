package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.ClosedOrder;
import org.lst.trading.lib.model.Order;
import org.lst.trading.lib.model.TradingStrategy;
import org.lst.trading.lib.model.TradingContext;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.series.TimeSeries;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static org.lst.trading.lib.util.Util.check;

public class Backtest {

    static class BacktestTradingContext implements TradingContext {
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

        @Override public Order order(String instrument, boolean isLong, int amount) {
            check(amount > 0);

            double price = getLastPrice(instrument);
            SimpleOrder order = new SimpleOrder(mOrderId++, instrument, getTime(), price, amount, isLong);
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
            return mClosedPl + mOrders.stream().mapToDouble(t -> Order.getPl(t, getLastPrice(t.getInstrument()))).sum() - mCommissions;
        }

        @Override public List<String> getInstruments() {
            return mInstruments;
        }

        @Override public double getAvailableFunds() {
            return getNetValue() - mOrders.stream().mapToDouble(t -> t.getAmount() * t.getOpenPrice() / mLeverage).sum();
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
            return 1 + order.getAmount() * 0.005;
        }
    }

    public static class Result {
        DoubleSeries mPlHistory;
        DoubleSeries mMarginHistory;
        double mPl;
        List<SimpleClosedOrder> mOrders;
        double mInitialFund;
        double mFinalValue;
        double mCommissions;

        public Result(double pl, DoubleSeries plHistory, DoubleSeries marginHistory, List<SimpleClosedOrder> orders, double initialFund, double finalValue, double commisions) {
            mPl = pl;
            mPlHistory = plHistory;
            mMarginHistory = marginHistory;
            mOrders = orders;
            mInitialFund = initialFund;
            mFinalValue = finalValue;
            mCommissions = commisions;
        }

        public DoubleSeries getMarginHistory() {
            return mMarginHistory;
        }

        public double getInitialFund() {
            return mInitialFund;
        }

        public DoubleSeries getAccountValueHistory() {
            return getPlHistory().plus(mInitialFund);
        }

        public double getFinalValue() {
            return mFinalValue;
        }

        public double getReturn() {
            return mFinalValue / mInitialFund - 1;
        }

        public double getAnnualizedReturn() {
            return getReturn() * 250 / getDaysCount();
        }

        public double getSharpe() {
            return Statistics.sharpe(Statistics.returns(getAccountValueHistory().toArray()));
        }

        public double getMaxDrawdown() {
            return Statistics.drawdown(getAccountValueHistory().toArray())[0];
        }

        public double getMaxDrawdownPercent() {
            return Statistics.drawdown(getAccountValueHistory().toArray())[1];
        }

        public int getDaysCount() {
            return mPlHistory.size();
        }

        public DoubleSeries getPlHistory() {
            return mPlHistory;
        }

        public double getPl() {
            return mPl;
        }

        public double getCommissions() {
            return mCommissions;
        }

        public List<SimpleClosedOrder> getOrders() {
            return mOrders;
        }
    }

    MultipleDoubleSeries mPriceSeries;
    double mDeposit;
    double mLeverage = 1;

    TradingStrategy mStrategy;
    BacktestTradingContext mContext;

    Iterator<TimeSeries.Entry<List<Double>>> mPriceIterator;
    Result mResult;

    public Backtest(double deposit, MultipleDoubleSeries priceSeries) {
        check(priceSeries.isAscending());
        mDeposit = deposit;
        mPriceSeries = priceSeries;
    }

    public void setLeverage(double leverage) {
        mLeverage = leverage;
    }

    public double getLeverage() {
        return mLeverage;
    }

    public Result run(TradingStrategy strategy) {
        initialize(strategy);
        while (nextStep()) ;
        return mResult;
    }

    public void initialize(TradingStrategy strategy) {
        mStrategy = strategy;
        mContext = new BacktestTradingContext();

        mContext.mInstruments = mPriceSeries.getNames();
        mContext.mHistory = new MultipleDoubleSeries(mContext.mInstruments);
        mContext.mInitialFunds = mDeposit;
        mContext.mLeverage = mLeverage;
        strategy.onStart(mContext);
        mPriceIterator = mPriceSeries.iterator();
        nextStep();
    }

    public boolean nextStep() {
        if (!mPriceIterator.hasNext()) {
            finish();
            return false;
        }

        TimeSeries.Entry<List<Double>> entry = mPriceIterator.next();

        mContext.mPrices = entry.getItem();
        mContext.mInstant = entry.getInstant();
        mContext.mPl.add(mContext.getPl(), entry.getInstant());
        mContext.mFundsHistory.add(mContext.getAvailableFunds(), entry.getInstant());
        if (mContext.getAvailableFunds() < 0) {
            finish();
            return false;
        }

        mStrategy.onTick();

        mContext.mHistory.add(entry);

        return true;
    }

    public Result getResult() {
        return mResult;
    }

    private void finish() {
        for (SimpleOrder order : new ArrayList<>(mContext.mOrders)) {
            mContext.close(order);
        }

        mStrategy.onEnd();

        mResult = new Result(mContext.mClosedPl, mContext.mPl, mContext.mFundsHistory, mContext.mClosedOrders, mDeposit, mDeposit + mContext.mClosedPl, mContext.mCommissions);
    }
}
