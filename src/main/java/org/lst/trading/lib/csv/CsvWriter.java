package org.lst.trading.lib.csv;

import rx.functions.Func1;

import java.io.*;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;

public class CsvWriter<T> {
    public static <T> CsvWriter<T> create(List<Column<T>> columns) {
        return new CsvWriter<>(columns);
    }

    public static <T> Column<T> column(String name, Func1<T, Object> f) {
        return new Column<T>() {
            @Override public String getName() {
                return name;
            }

            @Override public Func1<T, Object> getF() {
                return f;
            }
        };
    }

    public interface Column<T> {
        String getName();

        Func1<T, Object> getF();
    }

    String mSeparator = ",";
    List<Column<T>> mColumns;

    public CsvWriter(List<Column<T>> columns) {
        mColumns = columns;
    }

    public Stream<String> write(Stream<T> values) {
        return concat(
            of(mColumns.stream().map(Column::getName).collect(joining(mSeparator))),
            values.map(x -> mColumns.stream().map(f -> {
                Object o = f.getF().call(x);
                return o == null ? "" : o.toString();
            }).collect(joining(mSeparator)))
        );
    }

    public void writeToStream(Stream<T> values, OutputStream outputStream) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        write(values).forEach(x -> {
            try {
                writer.write(x);
                writer.write("\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writer.flush();
    }
}
