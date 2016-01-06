package org.lst.trading.lib.util;


import org.lst.trading.lib.series.MultipleDoubleSeries;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.joining;

public class Util {

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
