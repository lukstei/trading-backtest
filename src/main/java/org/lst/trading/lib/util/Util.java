package org.lst.trading.lib.util;


import org.lst.trading.lib.series.DoubleSeries;
import org.lst.trading.lib.series.MultipleDoubleSeries;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.DoubleStream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class Util {

    public static Path writeCsv(DoubleSeries series) {
        return writeCsv(new MultipleDoubleSeries(series));
    }

    public static Path writeCsv(double[][] series, Collection<String> names) {
        StringBuilder csv = new StringBuilder();
        csv.append(names.stream().collect(joining(","))).append("\n");

        for (int i = 0; i < series[0].length; i++) {
            for (int j = 0; j < series.length; j++) {
                if (j > 0) {
                    csv.append(",");
                }
                csv.append(series[j][i]);
            }
            csv.append("\n");
        }
        return writeStringToTempFile(csv.toString());
    }

    public static Path writeCsv(MultipleDoubleSeries series) {
        String data =
            "date," + series.getNames().stream().collect(joining(",")) + "\n" +
                series.stream().map(
                    e -> e.getInstant() + "," + e.getItem().stream().map(Object::toString).collect(joining(","))
                ).collect(joining("\n"));

        return writeStringToTempFile(data);
    }

    public static Path writeStringToTempFile(String content) {
        try {
            return writeString(content, Paths.get(File.createTempFile("out-", ".csv").getAbsolutePath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static Path writeString(String content, Path path) {
        try {
            Files.write(path, content.getBytes());
            return path;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new RuntimeException();
        }
    }


    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
