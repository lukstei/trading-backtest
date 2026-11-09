package org.lst.trading.lib.backtest;

import org.junit.Test;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.main.strategy.BuyAndHold;

import java.time.Instant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BacktestTest {
    static final double DELTA = 1e-6;
    static final double DEPOSIT = 10000;

    // 1 share each, 1 + 0.005 per order
    static final double COMMISSION_PER_ORDER = 1.005;

    static Instant day(int n) {
        return Instant.ofEpochSecond(n * 86400L);
    }

    static double[] line(double start, double step, int days) {
        double[] values = new double[days];
        for (int i = 0; i < days; i++) {
            values[i] = start + step * i;
        }
        return values;
    }

    static MultipleDoubleSeries prices(double[] a, double[] b) {
        DoubleSeries x = new DoubleSeries("A");
        DoubleSeries y = new DoubleSeries("B");
        for (int i = 0; i < a.length; i++) {
            x.add(a[i], day(i));
            y.add(b[i], day(i));
        }
        return new MultipleDoubleSeries(x, y);
    }

    static Backtest.Result buyAndHold(double[] a, double[] b) {
        return new Backtest(DEPOSIT, prices(a, b)).run(new BuyAndHold());
    }

    static double lastAccountValue(Backtest.Result result) {
        return result.getAccountValueHistory().getLast();
    }

    @Test public void finalValueIncludesAllCommissions() {
        // a 100 -> 109, b 50 -> 68, gross +27
        Backtest.Result result = buyAndHold(line(100, 1, 10), line(50, 2, 10));

        assertEquals(4 * COMMISSION_PER_ORDER, result.getCommissions(), DELTA);
        assertEquals(27 - 4 * COMMISSION_PER_ORDER, result.getPl(), DELTA);
        assertEquals(DEPOSIT + 27 - 4 * COMMISSION_PER_ORDER, result.getFinalValue(), DELTA);
        assertEquals(result.getInitialFund() + result.getPl(), result.getFinalValue(), DELTA);

        // last history point is before the closing orders
        assertEquals(DEPOSIT + 27 - 2 * COMMISSION_PER_ORDER, lastAccountValue(result), DELTA);
        assertEquals(lastAccountValue(result) - 2 * COMMISSION_PER_ORDER, result.getFinalValue(), DELTA);
    }

    @Test public void buyAndHoldIsOneTrade() {
        Backtest.Result result = buyAndHold(line(100, 1, 10), line(50, 2, 10));
        TradeStats stats = result.getTradeStats();

        assertEquals(1, stats.getTradeCount());
        assertEquals(2, stats.getOrderCount());
        assertEquals(1, stats.getWins());
        assertEquals(27, stats.getGrossProfit(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, stats.getProfitFactor(), 0);
    }

    @Test public void drawdownIsNeverPositive() {
        double[] a = {100, 102, 98, 104, 95, 101, 107, 103, 110, 108};
        double[] b = {50, 49, 52, 48, 53, 51, 47, 55, 54, 56};
        Backtest.Result result = buyAndHold(a, b);

        assertTrue(result.getMaxDrawdown() <= 0);
        assertTrue(result.getMaxDrawdownPercent() <= 0);

        // peak 10000 on day 0, low 10000 - 2 - 2.01 on day 4
        assertEquals(-4.01, result.getMaxDrawdown(), DELTA);
        assertEquals(-4.01 / DEPOSIT, result.getMaxDrawdownPercent(), DELTA);
    }

    @Test public void bigRiseHasNoDrawdown() {
        // +10 a day beats the 2.01 opening commissions
        Backtest.Result result = buyAndHold(line(100, 5, 10), line(50, 5, 10));

        assertEquals(0, result.getMaxDrawdown(), DELTA);
        assertEquals(0, result.getMaxDrawdownPercent(), DELTA);
        assertTrue(Double.isNaN(result.getCalmar()));
    }

    @Test public void smallRiseDipsFromOpeningCommissions() {
        // +1 on day 1 minus 2.01 commissions
        Backtest.Result result = buyAndHold(line(100, 0.5, 10), line(50, 0.5, 10));

        assertEquals(-1.01, result.getMaxDrawdown(), DELTA);
        assertEquals(-1.01 / DEPOSIT, result.getMaxDrawdownPercent(), DELTA);

        // gross +9, net 4.98 over 10 days
        assertEquals(4.98 / DEPOSIT, result.getReturn(), DELTA);
        assertEquals(4.98 / DEPOSIT * 250 / 10, result.getAnnualizedReturn(), DELTA);
        assertEquals(0.01245 / 0.000101, result.getCalmar(), DELTA);
    }
}
