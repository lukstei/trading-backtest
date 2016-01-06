package org.lst.trading.lib.csv;

import org.lst.trading.lib.model.Bar;
import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;
import org.lst.trading.lib.series.TimeSeries;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class CsvReader {
    public interface ParseFunction<T> {
        T parse(String value);

        String getColumn();

        default <F> ParseFunction<F> map(Function<T, F> f) {
            return new ParseFunction<F>() {
                @Override public F parse(String value) {
                    return f.apply(ParseFunction.this.parse(value));
                }

                @Override public String getColumn() {
                    return ParseFunction.this.getColumn();
                }
            };
        }

        static Function<String, String> stripQuotes() {
            return s -> {
                s = s.replaceFirst("^\"", "");
                s = s.replaceFirst("\"$", "");
                return s;
            };
        }

        static ParseFunction<String> ofColumn(String columnName) {
            return new ParseFunction<String>() {
                @Override public String parse(String value) {
                    return value;
                }

                @Override public String getColumn() {
                    return columnName;
                }
            };
        }

        static ParseFunction<Long> longColumn(String column) {
            return ofColumn(column).map(Long::parseLong);
        }

        static ParseFunction<Double> doubleColumn(String column) {
            return ofColumn(column).map(Double::parseDouble);
        }

        static ParseFunction<LocalDateTime> localDateTimeColumn(String column, DateTimeFormatter formatter) {
            return ofColumn(column).map(x -> LocalDateTime.from(formatter.parse(x)));
        }

        static ParseFunction<Instant> instantColumn(String column, DateTimeFormatter formatter) {
            return ofColumn(column).map(x -> Instant.from(formatter.parse(x)));
        }

        static ParseFunction<LocalDate> localDateColumn(String column, DateTimeFormatter formatter) {
            return ofColumn(column).map(x -> LocalDate.from(formatter.parse(x)));
        }

        static ParseFunction<LocalDate> localTimeColumn(String column, DateTimeFormatter formatter) {
            return ofColumn(column).map(x -> LocalDate.from(formatter.parse(x)));
        }
    }

    public interface Function2<T1, T2, F> {
        F apply(T1 t1, T2 t2);
    }

    private static class SeriesConsumer<T> implements Consumer<String[]> {
        int i = 0;
        List<String> mColumns;
        TimeSeries<T> mSeries;
        ParseFunction<Instant> mInstantParseFunction;
        Function2<String[], List<String>, T> mF;

        public SeriesConsumer(TimeSeries<T> series, ParseFunction<Instant> instantParseFunction, Function2<String[], List<String>, T> f) {
            mSeries = series;
            mInstantParseFunction = instantParseFunction;
            mF = f;
        }

        @Override public void accept(String[] parts) {
            if (i++ == 0) {
                mColumns = Stream.of(parts).map(String::trim).collect(toList());
            } else {
                Instant instant = mInstantParseFunction.parse(parts[mColumns.indexOf(mInstantParseFunction.getColumn())]);
                mSeries.add(mF.apply(parts, mColumns), instant);
            }
        }
    }

    private static class ListConsumer<T> implements Consumer<String[]> {
        int i = 0;
        List<T> mEntries = new ArrayList<>();
        Function<String[], T> mF;
        boolean mHasHeader;

        public ListConsumer(Function<String[], T> f, boolean hasHeader) {
            mF = f;
            mHasHeader = hasHeader;
        }

        @Override public void accept(String[] parts) {
            if (i++ > 0 || !mHasHeader) {
                mEntries.add(mF.apply(parts));
            }
        }

        public List<T> getEntries() {
            return mEntries;
        }
    }

    public static <T extends Consumer<String[]>> void parse(Stream<String> lines, String sep, T consumer) {
        lines.map(l -> l.split(sep)).forEach(consumer);
    }

    public static <T> List<T> parse(Stream<String> lines, String sep, boolean hasHeader, Function<String[], T> f) {
        ListConsumer<T> consumer = new ListConsumer<T>(f, hasHeader);
        parse(lines, sep, consumer);
        return consumer.getEntries();
    }

    @SafeVarargs
    public static MultipleDoubleSeries parse(Stream<String> lines, String sep, ParseFunction<Instant> instantF, ParseFunction<Double>... columns) {
        List<String> columnNames = Stream.of(columns).map(ParseFunction::getColumn).collect(toList());
        MultipleDoubleSeries series = new MultipleDoubleSeries(columnNames);
        SeriesConsumer<List<Double>> consumer = new SeriesConsumer<>(series, instantF, (parts, cn) -> Stream.of(columns).map(t -> t.parse(parts[cn.indexOf(t.getColumn())])).collect(toList()));
        parse(lines, sep, consumer);
        return series;
    }

    public static DoubleSeries parse(Stream<String> lines, String sep, ParseFunction<Instant> instantF, ParseFunction<Double> column) {
        DoubleSeries series = new DoubleSeries(column.getColumn());
        SeriesConsumer<Double> consumer = new SeriesConsumer<>(series, instantF, (parts, columnNames) -> column.parse(parts[columnNames.indexOf(column.getColumn())]));
        parse(lines, sep, consumer);
        return series;
    }

    public static Stream<Bar> parse(Stream<String> lines, ParseFunction<Double> open, ParseFunction<Double> high, ParseFunction<Double> low, ParseFunction<Double> close, ParseFunction<Long> volume, ParseFunction<Instant> instant) {
        return lines
            .map(l -> l.split(","))
            .flatMap(new Function<String[], Stream<? extends Bar>>() {
                public List<Object> mColumns;
                int i = 0;

                @Override public Stream<? extends Bar> apply(String[] parts) {
                    if (i++ == 0) {
                        mColumns = Stream.of(parts).map(String::trim).collect(toList());
                        return Stream.empty();
                    } else {
                        return Stream.of(
                            new Bar() {
                                private double mOpen = open.parse(parts[mColumns.indexOf(open.getColumn())]);
                                private double mHigh = high.parse(parts[mColumns.indexOf(high.getColumn())]);
                                private double mLow = low.parse(parts[mColumns.indexOf(low.getColumn())]);
                                private double mClose = close.parse(parts[mColumns.indexOf(close.getColumn())]);
                                private long mVolume = volume.parse(parts[mColumns.indexOf(volume.getColumn())]);

                                @Override public double getOpen() {
                                    return mOpen;
                                }

                                @Override public double getHigh() {
                                    return mHigh;
                                }

                                @Override public double getLow() {
                                    return mLow;
                                }

                                @Override public double getClose() {
                                    return mClose;
                                }

                                @Override public long getVolume() {
                                    return mVolume;
                                }

                                @Override public Instant getStart() {
                                    return instant.parse(parts[mColumns.indexOf(instant.getColumn())]);
                                }

                                @Override public Duration getDuration() {
                                    return null;
                                }

                                @Override public double getWAP() {
                                    return 0;
                                }
                            }
                        );
                    }
                }
            });
    }
}
