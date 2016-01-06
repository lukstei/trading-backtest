package org.lst.trading.lib.util;

import org.lst.trading.lib.series.DoubleSeries;
import rx.Observable;

public interface HistoricalPriceService {
    Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol);
}
