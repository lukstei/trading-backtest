package org.lst.trading.lib.util;

import org.lst.trading.lib.series.DoubleSeries;

public interface HistoricalPriceService {
    DoubleSeries getHistoricalAdjustedPrices(String symbol);
}
