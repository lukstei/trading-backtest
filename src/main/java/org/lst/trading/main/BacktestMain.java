package org.lst.trading.main;

import org.lst.trading.lib.backtest.Backtest;
import org.lst.trading.lib.backtest.TradeStats;
import org.lst.trading.lib.model.ClosedOrder;
import org.lst.trading.lib.model.TradingStrategy;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.util.AlphaVantageHistoricalPriceService;
import org.lst.trading.lib.util.CsvHistoricalPriceService;
import org.lst.trading.lib.util.HistoricalPriceService;
import org.lst.trading.lib.util.Util;
import org.lst.trading.main.strategy.kalman.CointegrationTradingStrategy;

import java.util.Locale;

import static java.lang.String.format;

public class BacktestMain {
    static String alphaVantantageApiKey = ""; // fill API key in here or pass via system property: -Dalphavantantage.apikey=APIKEY

    public static void main(String[] args) throws Exception {
        String x = "GLD";
        String y = "GDX";

        // initialize the trading strategy
        TradingStrategy strategy = new CointegrationTradingStrategy(x, y);

        // load historical prices
        HistoricalPriceService finance = createPriceService();
        MultipleDoubleSeries priceSeries = new MultipleDoubleSeries(finance.getHistoricalAdjustedPrices(x).toBlocking().first(), finance.getHistoricalAdjustedPrices(y).toBlocking().first());

        // initialize the backtesting engine
        int deposit = 15000;
        Backtest backtest = new Backtest(deposit, priceSeries);
        backtest.setLeverage(4);

        // do the backtest
        Backtest.Result result = backtest.run(strategy);

        // show results
        StringBuilder orders = new StringBuilder();
        orders.append("id,amount,side,instrument,from,to,open,close,pl\n");
        for (ClosedOrder order : result.getOrders()) {
            orders.append(format(Locale.US, "%d,%d,%s,%s,%s,%s,%f,%f,%f\n", order.getId(), Math.abs(order.getAmount()), order.isLong() ? "Buy" : "Sell", order.getInstrument(), order.getOpenInstant(), order.getCloseInstant(), order.getOpenPrice(), order.getClosePrice(), order.getPl()));
        }
        System.out.print(orders);

        int days = priceSeries.size();

        System.out.println();
        System.out.println("Backtest result of " + strategy.getClass() + ": " + strategy);
        System.out.println("Prices: " + priceSeries);
        System.out.println(format(Locale.US, "Simulated %d days, Initial deposit %d, Leverage %f", days, deposit, backtest.getLeverage()));
        System.out.println(format(Locale.US, "Commissions = %.2f", result.getCommissions()));
        System.out.println(format(Locale.US, "P/L = %.2f, Final value = %.2f, Result = %.2f%%, Annualized = %.2f%%, Sharpe (rf=0%%) = %.2f", result.getPl(), result.getFinalValue(), result.getReturn() * 100, result.getAnnualizedReturn() * 100, result.getSharpe()));

        // risk and trade stats
        TradeStats stats = result.getTradeStats();
        System.out.println(format(Locale.US, "Max drawdown = %.2f (%.2f%%), Calmar = %s", result.getMaxDrawdown(), result.getMaxDrawdownPercent() * 100, formatRatio(result.getCalmar())));
        System.out.println(format(Locale.US, "Trades = %d round trips (%d orders), Win rate = %s, Profit factor = %s", stats.getTradeCount(), stats.getOrderCount(), formatPercent(stats.getWinRate()), formatRatio(stats.getProfitFactor())));
        System.out.println(format(Locale.US, "Avg win = %s, Avg loss = %s (before commissions)", formatRatio(stats.getAverageWin()), formatRatio(stats.getAverageLoss())));

        System.out.println("Orders: " + Util.writeStringToTempFile(orders.toString()));
        System.out.println("Statistics: " + Util.writeCsv(new MultipleDoubleSeries(result.getPlHistory(), result.getMarginHistory())));
    }

    // bundled csv unless an api key is set
    private static HistoricalPriceService createPriceService() {
        String key = System.getProperty("alphavantantage.apikey");
        if (key != null) {
            alphaVantantageApiKey = key;
        }

        if (alphaVantantageApiKey.isEmpty()) {
            System.out.println("Using price files from data/");
            return new CsvHistoricalPriceService("data");
        }
        System.out.println("Using Alpha Vantage prices");
        return new AlphaVantageHistoricalPriceService(alphaVantantageApiKey);
    }

    // n/a and inf instead of NaN and Infinity
    private static String formatRatio(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        if (Double.isInfinite(value)) {
            return "inf";
        }
        return format(Locale.US, "%.2f", value);
    }

    private static String formatPercent(double value) {
        if (Double.isNaN(value)) {
            return "n/a";
        }
        return format(Locale.US, "%.1f%%", value * 100);
    }
}
