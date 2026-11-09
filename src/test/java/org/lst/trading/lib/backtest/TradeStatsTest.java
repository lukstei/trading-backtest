package org.lst.trading.lib.backtest;

import org.junit.Test;
import org.lst.trading.lib.model.ClosedOrder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TradeStatsTest {
    static final double DELTA = 1e-9;

    static Instant day(int n) {
        return Instant.ofEpochSecond(n * 86400L);
    }

    // p/l = amount * (close - open)
    static ClosedOrder order(int amount, double open, double close, int openDay, int closeDay) {
        SimpleOrder order = new SimpleOrder(1, "X", day(openDay), open, amount);
        return new SimpleClosedOrder(order, close, day(closeDay));
    }

    @Test public void twoLegsWithSameInstantsAreOneTrade() {
        List<ClosedOrder> orders = new ArrayList<>();
        orders.add(order(10, 100, 105, 1, 5));
        orders.add(order(-5, 50, 52, 1, 5));

        TradeStats stats = new TradeStats(orders);

        assertEquals(1, stats.getTradeCount());
        assertEquals(2, stats.getOrderCount());
        assertEquals(1, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(40, stats.getGrossProfit(), DELTA);
        assertEquals(0, stats.getGrossLoss(), DELTA);
        assertEquals(40, stats.getAverageWin(), DELTA);
    }

    @Test public void sameOpenDifferentCloseAreSeparateTrades() {
        List<ClosedOrder> orders = new ArrayList<>();
        orders.add(order(10, 100, 105, 1, 5));
        orders.add(order(10, 100, 99, 1, 6));

        TradeStats stats = new TradeStats(orders);

        assertEquals(2, stats.getTradeCount());
        assertEquals(1, stats.getWins());
        assertEquals(1, stats.getLosses());
    }

    @Test public void mixedWinsAndLosses() {
        List<ClosedOrder> orders = new ArrayList<>();
        // +40 over two legs
        orders.add(order(10, 100, 105, 1, 3));
        orders.add(order(-5, 50, 52, 1, 3));
        // -20
        orders.add(order(10, 100, 98, 4, 6));
        // +10
        orders.add(order(-5, 50, 48, 7, 9));
        // +30
        orders.add(order(3, 20, 30, 10, 12));
        // -5
        orders.add(order(1, 40, 35, 13, 15));

        TradeStats stats = new TradeStats(orders);

        assertEquals(5, stats.getTradeCount());
        assertEquals(6, stats.getOrderCount());
        assertEquals(3, stats.getWins());
        assertEquals(2, stats.getLosses());
        assertEquals(0.6, stats.getWinRate(), DELTA);
        assertEquals(80, stats.getGrossProfit(), DELTA);
        assertEquals(-25, stats.getGrossLoss(), DELTA);
        assertEquals(3.2, stats.getProfitFactor(), DELTA);
        assertEquals(80 / 3.0, stats.getAverageWin(), DELTA);
        assertEquals(-12.5, stats.getAverageLoss(), DELTA);
    }

    @Test public void flatTradeIsNeitherWinNorLoss() {
        List<ClosedOrder> orders = new ArrayList<>();
        orders.add(order(10, 100, 105, 1, 3));
        orders.add(order(10, 100, 100, 4, 6));
        orders.add(order(10, 100, 99, 7, 9));

        TradeStats stats = new TradeStats(orders);

        assertEquals(3, stats.getTradeCount());
        assertEquals(1, stats.getWins());
        assertEquals(1, stats.getLosses());
        assertEquals(1 / 3.0, stats.getWinRate(), DELTA);
        assertEquals(5, stats.getProfitFactor(), DELTA);
    }

    @Test public void allWinsHaveInfiniteProfitFactor() {
        List<ClosedOrder> orders = new ArrayList<>();
        orders.add(order(10, 100, 105, 1, 3));
        orders.add(order(-5, 50, 48, 4, 6));

        TradeStats stats = new TradeStats(orders);

        assertEquals(2, stats.getTradeCount());
        assertEquals(2, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(1.0, stats.getWinRate(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, stats.getProfitFactor(), 0);
        assertEquals(30, stats.getAverageWin(), DELTA);
        assertTrue(Double.isNaN(stats.getAverageLoss()));
    }

    @Test public void allLossesHaveZeroProfitFactor() {
        List<ClosedOrder> orders = new ArrayList<>();
        orders.add(order(10, 100, 95, 1, 3));

        TradeStats stats = new TradeStats(orders);

        assertEquals(0, stats.getProfitFactor(), DELTA);
        assertEquals(0, stats.getWinRate(), DELTA);
        assertTrue(Double.isNaN(stats.getAverageWin()));
        assertEquals(-50, stats.getAverageLoss(), DELTA);
    }

    @Test public void noTrades() {
        TradeStats stats = new TradeStats(new ArrayList<>());

        assertEquals(0, stats.getTradeCount());
        assertEquals(0, stats.getOrderCount());
        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0, stats.getGrossProfit(), DELTA);
        assertEquals(0, stats.getGrossLoss(), DELTA);
        assertTrue(Double.isNaN(stats.getWinRate()));
        assertTrue(Double.isNaN(stats.getProfitFactor()));
        assertTrue(Double.isNaN(stats.getAverageWin()));
        assertTrue(Double.isNaN(stats.getAverageLoss()));
    }
}
