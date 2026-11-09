package org.lst.trading.lib.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatisticsTest {
    static final double DELTA = 1e-9;

    static void assertDrawdown(double dd, double ddPct, double[] series) {
        double[] result = Statistics.drawdown(series);
        assertEquals(2, result.length);
        assertEquals(dd, result[0], DELTA);
        assertEquals(ddPct, result[1], DELTA);
    }

    @Test public void risingOnlyHasNoDrawdown() {
        assertDrawdown(0, 0, new double[]{100, 105, 105, 130, 200});
    }

    @Test public void rising100To120IsZeroNotPlusTen() {
        assertDrawdown(0, 0, new double[]{100, 110, 120});
    }

    @Test public void fallingSeries() {
        assertDrawdown(-20, -0.2, new double[]{100, 90, 80});
    }

    @Test public void peakThenPartialRecovery() {
        assertDrawdown(-30, -0.25, new double[]{100, 120, 90, 110});
    }

    @Test public void laterDeeperDrawdownWins() {
        assertDrawdown(-60, -0.4, new double[]{100, 80, 150, 90, 140});
    }

    @Test public void singleValueHasNoDrawdown() {
        assertDrawdown(0, 0, new double[]{100});
    }

    @Test public void emptyHasNoDrawdown() {
        assertDrawdown(0, 0, new double[0]);
    }
}
