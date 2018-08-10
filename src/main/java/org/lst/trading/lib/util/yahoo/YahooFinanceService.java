package org.lst.trading.lib.util.yahoo;

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.TimeSeries.Entry;
import org.lst.trading.lib.util.HistoricalPriceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;

public class YahooFinanceService implements HistoricalPriceService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceService.class);
    
    private static final int DAYS_OF_HISTORY = 2520;

    @Override 
    public DoubleSeries getHistoricalAdjustedPrices(String symbol) {
        return getHistory(symbol, DAYS_OF_HISTORY);
    }

	public DoubleSeries getHistory(String symbol, int daysBack) {
		DoubleSeries doubleSeries = new DoubleSeries(symbol);
		
		try {
			Calendar from = Calendar.getInstance();
			from.add(Calendar.DAY_OF_MONTH, -daysBack);

			Stock stock = YahooFinance.get(symbol);
			//[n] is the most current, [0] is the last in history
			List<HistoricalQuote> historicalQuotes = stock.getHistory(from, Interval.DAILY);

			for (HistoricalQuote historicalQuote : historicalQuotes) {
				BigDecimal open = historicalQuote.getOpen();
				BigDecimal high = historicalQuote.getHigh();
				BigDecimal low = historicalQuote.getLow();
				BigDecimal close = historicalQuote.getClose();
				BigDecimal adjClose = historicalQuote.getAdjClose();
				Date time = historicalQuote.getDate().getTime();
				
				if (open == null || high == null || low == null || close == null || adjClose == null) {
					log.error(symbol + " " + time + " has missing data: " + open + ", " + high + ", " + low + ", " + close + ", " + adjClose);
					continue;
				}
				
				doubleSeries.add(new Entry<Double>(adjClose.doubleValue(), time.toInstant()));
			}
		} catch (FileNotFoundException e) {
			log.error("no such symbol: " + symbol);
		} catch (SocketTimeoutException e) {
			log.error("network error: " + symbol);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return doubleSeries;
	}
}
