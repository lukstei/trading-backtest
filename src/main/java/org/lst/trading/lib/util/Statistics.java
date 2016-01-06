package org.lst.trading.lib.util;

import org.apache.commons.math3.stat.StatUtils;

public class Statistics {
    public static double[] drawdown(double[] series) {
        double max = Double.MIN_VALUE;
        double ddPct = Double.MAX_VALUE;
        double dd = Double.MAX_VALUE;

        for (double x : series) {
            dd = Math.min(x - max, dd);
            ddPct = Math.min(x / max - 1, ddPct);
            max = Math.max(max, x);
        }

        return new double[]{dd, ddPct};
    }

    public static double sharpe(double[] dailyReturns) {
        return StatUtils.mean(dailyReturns) / Math.sqrt(StatUtils.variance(dailyReturns)) * Math.sqrt(250);
    }

    public static double[] returns(double[] series) {
        if (series.length <= 1) {
            return new double[0];
        }

        double[] returns = new double[series.length - 1];
        for (int i = 1; i < series.length; i++) {
            returns[i - 1] = series[i] / series[i - 1] - 1;
        }

        return returns;
    }
}
