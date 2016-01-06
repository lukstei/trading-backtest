package org.lst.trading.lib.backtest;

import org.lst.trading.lib.model.ClosedOrder;
import org.lst.trading.lib.model.TradingStrategy;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.series.TimeSeries;
import org.lst.trading.lib.util.Statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.lst.trading.lib.util.Util.check;

public class Backtest {

    public static class Result {
        DoubleSeries mPlHistory;
        DoubleSeries mMarginHistory;
        double mPl;
        List<ClosedOrder> mOrders;
        double mInitialFund;
        double mFinalValue;
        double mCommissions;

        public Result(double pl, DoubleSeries plHistory, DoubleSeries marginHistory, List<ClosedOrder> orders, double initialFund, double finalValue, double commisions) {
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

        public List<ClosedOrder> getOrders() {
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

        List<ClosedOrder> orders = Collections.unmodifiableList(mContext.mClosedOrders);
        mResult = new Result(mContext.mClosedPl, mContext.mPl, mContext.mFundsHistory, orders, mDeposit, mDeposit + mContext.mClosedPl, mContext.mCommissions);
    }
}
