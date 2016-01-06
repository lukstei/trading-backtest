package org.lst.trading.lib.util.yahoo;

import org.lst.trading.lib.csv.CsvReader;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.util.HistoricalPriceService;
import org.lst.trading.lib.util.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.doubleColumn;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.ofColumn;

public class YahooFinance implements HistoricalPriceService {
    public static final String SEP = ",";
    public static final CsvReader.ParseFunction<Instant> DATE_COLUMN = ofColumn("Date").map(s -> LocalDate.from(DateTimeFormatter.ISO_DATE.parse(s)).atStartOfDay(ZoneOffset.UTC.normalized()).toInstant());
    public static final CsvReader.ParseFunction<Double> CLOSE_COLUMN = doubleColumn("Close");
    public static final CsvReader.ParseFunction<Double> HIGH_COLUMN = doubleColumn("High");
    public static final CsvReader.ParseFunction<Double> LOW_COLUMN = doubleColumn("Low");
    public static final CsvReader.ParseFunction<Double> OPEN_COLUMN = doubleColumn("Open");
    public static final CsvReader.ParseFunction<Double> ADJ_COLUMN = doubleColumn("Adj Close");
    public static final CsvReader.ParseFunction<Double> VOLUME_COLUMN = doubleColumn("Volume");
    public static final OffsetDateTime DEFAULT_FROM = OffsetDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final Logger log = LoggerFactory.getLogger(YahooFinance.class);

    @Override public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol) {
        return getHistoricalAdjustedPrices(symbol, DEFAULT_FROM.toInstant());
    }

    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol, Instant from) {
        return getHistoricalAdjustedPrices(symbol, from, Instant.now());
    }

    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol, Instant from, Instant to) {
        return getHistoricalPricesCsv(symbol, from, to).map(csv -> csvToDoubleSeries(csv, symbol));
    }

    private static Observable<String> getHistoricalPricesCsv(String symbol, Instant from, Instant to) {
        return Http.get(createHistoricalPricesUrl(symbol, from, to))
            .flatMap(Http.asString());
    }

    private static DoubleSeries csvToDoubleSeries(String csv, String symbol) {
        Stream<String> lines = Stream.of(csv.split("\n"));
        DoubleSeries prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN);
        prices.setName(symbol);
        prices = prices.toAscending();
        return prices;
    }

    private static String createHistoricalPricesUrl(String symbol, Instant from, Instant to) {
        return format("http://ichart.yahoo.com/table.csv?s=%s&%s&%s&g=d&ignore=.csv", symbol, toYahooQueryDate(from, "abc"), toYahooQueryDate(to, "def"));
    }

    private static String toYahooQueryDate(Instant instant, String names) {
        OffsetDateTime time = instant.atOffset(ZoneOffset.UTC);
        String[] strings = names.split("");
        return format("%s=%d&%s=%d&%s=%d", strings[0], time.getMonthValue() - 1, strings[1], time.getDayOfMonth(), strings[2], time.getYear());
    }
}
