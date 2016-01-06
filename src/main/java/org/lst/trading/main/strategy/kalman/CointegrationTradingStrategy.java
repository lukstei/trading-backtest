package org.lst.trading.main.strategy.kalman;

import org.apache.commons.math3.stat.StatUtils;
import org.lst.trading.lib.model.Order;
import org.lst.trading.lib.model.TradingContext;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.series.TimeSeries;
import org.lst.trading.lib.util.Util;
import org.lst.trading.main.strategy.AbstractTradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CointegrationTradingStrategy extends AbstractTradingStrategy {
    private static Logger log = LoggerFactory.getLogger(CointegrationTradingStrategy.class);

    boolean mReinvest = false;

    String mX, mY;
    TradingContext mContext;
    Cointegration mCoint;

    DoubleSeries mAlpha;
    DoubleSeries mBeta;
    DoubleSeries mXs;
    DoubleSeries mYs;
    DoubleSeries mError;
    DoubleSeries mVariance;
    DoubleSeries mModel;

    Order mXOrder;
    Order mYOrder;

    public CointegrationTradingStrategy(String x, String y) {
        this(1, x, y);
    }

    public CointegrationTradingStrategy(double weight, String x, String y) {
        setWeight(weight);
        mX = x;
        mY = y;
    }

    @Override public void onStart(TradingContext context) {
        mContext = context;
        mCoint = new Cointegration(1e-10, 1e-7);
        mAlpha = new DoubleSeries("alpha");
        mBeta = new DoubleSeries("beta");
        mXs = new DoubleSeries("x");
        mYs = new DoubleSeries("y");
        mError = new DoubleSeries("error");
        mVariance = new DoubleSeries("variance");
        mModel = new DoubleSeries("model");
    }

    @Override public void onTick() {
        double x = mContext.getLastPrice(mX);
        double y = mContext.getLastPrice(mY);
        double alpha = mCoint.getAlpha();
        double beta = mCoint.getBeta();

        mCoint.step(x, y);
        mAlpha.add(alpha, mContext.getTime());
        mBeta.add(beta, mContext.getTime());
        mXs.add(x, mContext.getTime());
        mYs.add(y, mContext.getTime());
        mError.add(mCoint.getError(), mContext.getTime());
        mVariance.add(mCoint.getVariance(), mContext.getTime());

        double error = mCoint.getError();

        mModel.add(beta * x + alpha, mContext.getTime());

        if (mError.size() > 30) {
            double[] lastValues = mError.reversedStream().mapToDouble(TimeSeries.Entry::getItem).limit(15).toArray();
            double sd = Math.sqrt(StatUtils.variance(lastValues));

            if (mYOrder == null && Math.abs(error) > sd) {
                double value = mReinvest ? mContext.getNetValue() : mContext.getInitialFunds();
                double baseAmount = (value * getWeight() * 0.5 * Math.min(4, mContext.getLeverage())) / (y + beta * x);

                if (beta > 0 && baseAmount * beta >= 1) {
                    mYOrder = mContext.order(mY, error < 0, (int) baseAmount);
                    mXOrder = mContext.order(mX, error > 0, (int) (baseAmount * beta));
                }
                //log.debug("Order: baseAmount={}, residual={}, sd={}, beta={}", baseAmount, residual, sd, beta);
            } else if (mYOrder != null) {
                if (mYOrder.isLong() && error > 0 || !mYOrder.isLong() && error < 0) {
                    mContext.close(mYOrder);
                    mContext.close(mXOrder);

                    mYOrder = null;
                    mXOrder = null;
                }
            }
        }
    }

    @Override public void onEnd() {
        log.debug("Kalman filter statistics: " + Util.writeCsv(new MultipleDoubleSeries(mXs, mYs, mAlpha, mBeta, mError, mVariance, mModel)));
    }

    @Override public String toString() {
        return "CointegrationStrategy{" +
            "mY='" + mY + '\'' +
            ", mX='" + mX + '\'' +
            '}';
    }

    public DoubleSeries getAlpha() {
        return mAlpha;
    }

    public DoubleSeries getBeta() {
        return mBeta;
    }

    public DoubleSeries getXs() {
        return mXs;
    }

    public DoubleSeries getYs() {
        return mYs;
    }

    public DoubleSeries getError() {
        return mError;
    }

    public DoubleSeries getVariance() {
        return mVariance;
    }

    public DoubleSeries getModel() {
        return mModel;
    }
}
