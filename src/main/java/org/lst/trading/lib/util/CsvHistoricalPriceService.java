package org.lst.trading.lib.util;

import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.TimeSeries;
import rx.Observable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CsvHistoricalPriceService implements HistoricalPriceService {
    // header names, first match wins
    static final String[] DATE_COLUMNS = {"date", "timestamp"};
    static final String[] PRICE_COLUMNS = {"adj close", "adj_close", "adjclose", "close", "close/last"};

    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("M/d/yyyy");

    private final Path mFolder;

    public CsvHistoricalPriceService(String folder) {
        this(Paths.get(folder));
    }

    public CsvHistoricalPriceService(Path folder) {
        mFolder = folder;
    }

    @Override
    public Observable<DoubleSeries> getHistoricalAdjustedPrices(String symbol) {
        return Observable.just(readFile(mFolder.resolve(symbol + ".csv"), symbol));
    }

    public static DoubleSeries readFile(Path file, String symbol) {
        if (!Files.isRegularFile(file)) {
            throw new RuntimeException("No price file for " + symbol + ", expected " + file.toAbsolutePath());
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + file.toAbsolutePath(), e);
        }
        return parseLines(lines, symbol, file.toAbsolutePath().toString());
    }

    static DoubleSeries parseLines(List<String> lines, String symbol, String source) {
        int headerLine = firstNonBlankLine(lines);
        if (headerLine < 0) {
            throw new RuntimeException("Empty price file: " + source);
        }

        String[] header = splitRow(stripBom(lines.get(headerLine)));
        int dateCol = findColumn(header, DATE_COLUMNS);
        int priceCol = findColumn(header, PRICE_COLUMNS);
        if (dateCol < 0) {
            throw new RuntimeException("No date column in " + source + ", expected one of: " + String.join(", ", DATE_COLUMNS));
        }
        if (priceCol < 0) {
            throw new RuntimeException("No price column in " + source + ", expected one of: " + String.join(", ", PRICE_COLUMNS));
        }

        List<TimeSeries.Entry<Double>> entries = new ArrayList<>();
        for (int i = headerLine + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) {
                continue;
            }

            String[] row = splitRow(line);
            if (row.length <= Math.max(dateCol, priceCol)) {
                continue;
            }

            Double price = parsePrice(row[priceCol]);
            if (price == null) {
                continue;
            }

            Instant instant = parseDate(row[dateCol], i + 1, source);
            entries.add(new TimeSeries.Entry<>(price, instant));
        }

        if (entries.isEmpty()) {
            throw new RuntimeException("No prices found in " + source);
        }

        // newest first files
        entries.sort((a, b) -> a.getInstant().compareTo(b.getInstant()));

        DoubleSeries series = new DoubleSeries(symbol);
        for (TimeSeries.Entry<Double> entry : entries) {
            series.add(entry);
        }
        return series;
    }

    // split on commas outside quotes, drop the quotes
    static String[] splitRow(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());

        return parts.toArray(new String[0]);
    }

    // index of first name found, or -1
    private static int findColumn(String[] header, String[] names) {
        for (String name : names) {
            for (int i = 0; i < header.length; i++) {
                if (header[i].equalsIgnoreCase(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    // null when missing or not a number
    private static Double parsePrice(String value) {
        String s = value.startsWith("$") ? value.substring(1) : value;
        s = s.replace(",", "");

        double price;
        try {
            price = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }

        if (Double.isNaN(price) || Double.isInfinite(price)) {
            return null;
        }
        return price;
    }

    // start of day utc, same as alpha vantage loader
    private static Instant parseDate(String value, int lineNumber, String source) {
        try {
            LocalDate date;
            if (value.contains("/")) {
                date = LocalDate.parse(value, US_DATE);
            } else {
                // drop any time part
                String day = value.length() > 10 ? value.substring(0, 10) : value;
                date = LocalDate.parse(day);
            }
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (DateTimeParseException e) {
            throw new RuntimeException("Bad date '" + value + "' on line " + lineNumber + " of " + source
                + ", expected yyyy-MM-dd or MM/dd/yyyy");
        }
    }

    private static int firstNonBlankLine(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (!lines.get(i).trim().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // utf-8 bom from excel exports
    private static String stripBom(String line) {
        return line.startsWith("\uFEFF") ? line.substring(1) : line;
    }
}
