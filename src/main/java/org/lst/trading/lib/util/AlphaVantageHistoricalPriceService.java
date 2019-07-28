package org.lst.trading.lib.util;

import org.lst.trading.lib.csv.CsvReader;
import org.lst.trading.lib.series.DoubleSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.doubleColumn;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.ofColumn;

public class AlphaVantageHistoricalPriceService implements HistoricalPriceService {
    private final String apikey;

    public static final String SEP = ",";
    public static final CsvReader.ParseFunction<Instant> DATE_COLUMN = ofColumn("timestamp").map(s -> LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).atStartOfDay(ZoneOffset.UTC.normalized()).toInstant());
    public static final CsvReader.ParseFunction<Double> CLOSE_COLUMN = doubleColumn("close");
    public static final CsvReader.ParseFunction<Double> HIGH_COLUMN = doubleColumn("high");
    public static final CsvReader.ParseFunction<Double> LOW_COLUMN = doubleColumn("low");
    public static final CsvReader.ParseFunction<Double> OPEN_COLUMN = doubleColumn("open");
    public static final CsvReader.ParseFunction<Double> VOLUME_COLUMN = doubleColumn("volume");

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageHistoricalPriceService.class);

    public AlphaVantageHistoricalPriceService(String apikey) {
        this.apikey = apikey;
    }

    @Override
    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol) {
        return Http.get(createHistoricalPricesUrl(symbol, apikey))
                .flatMap(Http.asString())
                .map(csv -> csvToDoubleSeries(csv, symbol));
    }

    private static DoubleSeries csvToDoubleSeries(String csv, String symbol) {
        Stream<String> lines = Stream.of(csv.split("\n"));
        DoubleSeries prices = CsvReader.parse(lines, SEP, DATE_COLUMN, CLOSE_COLUMN);
        prices.setName(symbol);
        prices = prices.toAscending();
        return prices;
    }

    private static String createHistoricalPricesUrl(String symbol, String apikey) {
        return format("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&apikey=%s&datatype=csv&outputsize=full",
                symbol, apikey);
    }

}
