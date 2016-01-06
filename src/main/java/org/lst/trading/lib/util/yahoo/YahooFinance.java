package org.lst.trading.lib.util.yahoo;

import org.lst.trading.lib.csv.CsvReader;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.util.HistoricalPriceService;
import org.lst.trading.lib.util.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.doubleColumn;
import static org.lst.trading.lib.csv.CsvReader.ParseFunction.ofColumn;
import static org.lst.trading.lib.util.Util.check;

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

    public Observable<List<Quote>> getQuotes(String... symbols) throws IOException, URISyntaxException {
        String symbolsJoined = Stream.of(symbols).collect(joining(","));

        return Http.get(format("http://download.finance.yahoo.com/d/quotes.csv?s=%s&f=sabt1d1&e=.csv", symbolsJoined))
            .flatMap(Http.asString())
            .map(csv -> {
                String[] lines = csv.split("\n");
                List<Quote> quotes = new ArrayList<>();

                for (int i = 0; i < lines.length; i++) {
                    String[] s = lines[i].trim().replace("\"", "").split(",");
                    check(s[0].equalsIgnoreCase(symbols[i]));

                    double avgPrice = (Double.parseDouble(s[1]) + Double.parseDouble(s[2])) / 2;
                    LocalTime time = LocalTime.parse(s[3].toUpperCase(), DateTimeFormatter.ofPattern("h:ma"));
                    LocalDate date = LocalDate.parse(s[4], DateTimeFormatter.ofPattern("M/d/yyyy"));
                    Instant instant = date.atTime(time).atZone(ZoneId.of("America/New_York")).toInstant();
                    quotes.add(new Quote(s[0], avgPrice, instant));
                }

                return quotes;
            });
    }

    public static Observable<Double> getAdv(String symbol) {
        return Http.get(format("http://download.finance.yahoo.com/d/quotes.csv?s=%s&f=a2&e=.csv", symbol))
            .flatMap(Http.asString())
            .map(Double::parseDouble);
    }

    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol, Instant from) {
        return getHistoricalAdjustedPrices(symbol, from, Instant.now());
    }

    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol, Instant from, Instant to) {
        return getHistoricalPricesCsv(symbol, from, to).map(csv -> csvToDoubleSeries(csv, symbol));
    }

    public DoubleSeries csvToDoubleSeries(String csv, String symbol) {
        Stream<String> lines = Stream.of(csv.split("\n"));
        DoubleSeries prices = CsvReader.parse(lines, SEP, DATE_COLUMN, ADJ_COLUMN);
        prices.setName(symbol);
        prices = prices.toAscending();
        return prices;
    }


    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol) {
        return getHistoricalAdjustedPrices(symbol, DEFAULT_FROM.toInstant());
    }

    public Observable<String> getHistoricalPricesCsv(String symbol, Instant from, Instant to) {
        return Http.get(createHistoricalPricesUrl(symbol, from, to))
            .flatMap(Http.asString());
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
