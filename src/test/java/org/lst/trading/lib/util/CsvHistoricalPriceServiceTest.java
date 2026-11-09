package org.lst.trading.lib.util;

import org.junit.Test;
import org.lst.trading.lib.series.DoubleSeries;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class CsvHistoricalPriceServiceTest {

    @Test
    public void stooqFormatUsesClose() throws Exception {
        DoubleSeries series = load("stooq", "GLD");

        assertEquals(3, series.size());
        assertEntry(series, 0, "2024-01-02", 190.80);
        assertEntry(series, 1, "2024-01-03", 189.20);
        assertEntry(series, 2, "2024-01-04", 190.10);
    }

    @Test
    public void yahooFormatPrefersAdjClose() throws Exception {
        DoubleSeries series = load("yahoo", "GLD");

        assertEquals(3, series.size());
        assertEntry(series, 0, "2024-01-02", 185.80);
        assertEntry(series, 1, "2024-01-03", 184.24);
        assertEntry(series, 2, "2024-01-04", 185.12);
    }

    @Test
    public void nasdaqFormatIsSortedAscending() throws Exception {
        DoubleSeries series = load("nasdaq", "GLD");

        assertEquals(3, series.size());
        assertEntry(series, 0, "2024-01-02", 185.80);
        assertEntry(series, 1, "2024-01-03", 184.20);
        assertEntry(series, 2, "2024-01-04", 186.04);
    }

    @Test
    public void badRowsAreSkipped() throws Exception {
        DoubleSeries series = load("messy", "GDX");

        assertEquals(3, series.size());
        assertEntry(series, 0, "2024-01-02", 30.50);
        assertEntry(series, 1, "2024-01-05", 31.25);
        assertEntry(series, 2, "2024-01-10", 1030.75);
    }

    @Test
    public void seriesNameIsSymbol() throws Exception {
        CsvHistoricalPriceService service = new CsvHistoricalPriceService(fixtureFolder("stooq").toString());
        DoubleSeries series = service.getHistoricalAdjustedPrices("GLD").toBlocking().first();

        assertEquals("GLD", series.getName());
    }

    @Test
    public void missingFileNamesExpectedPath() throws Exception {
        Path folder = fixtureFolder("stooq");
        CsvHistoricalPriceService service = new CsvHistoricalPriceService(folder);

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.getHistoricalAdjustedPrices("XYZ"));

        String expected = folder.resolve("XYZ.csv").toAbsolutePath().toString();
        assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }

    @Test
    public void missingRelativeFolderNamesAbsolutePath() {
        CsvHistoricalPriceService service = new CsvHistoricalPriceService("no-such-folder");

        RuntimeException e = assertThrows(RuntimeException.class, () -> service.getHistoricalAdjustedPrices("GLD"));

        String expected = Paths.get("no-such-folder", "GLD.csv").toAbsolutePath().toString();
        assertTrue(e.getMessage(), e.getMessage().contains(expected));
    }

    @Test
    public void timestampColumnNewestFirst() {
        DoubleSeries series = parse(
            "timestamp,open,high,low,close,volume",
            "2024-01-04,1,1,1,12.5,100",
            "2024-01-03,1,1,1,11.5,100",
            "2024-01-02,1,1,1,10.5,100");

        assertEquals(3, series.size());
        assertEntry(series, 0, "2024-01-02", 10.5);
        assertEntry(series, 2, "2024-01-04", 12.5);
    }

    @Test
    public void adjCloseVariantWinsOverEarlierClose() {
        DoubleSeries series = parse(
            "date,close,adj_close",
            "2024-01-02,100,90");

        assertEntry(series, 0, "2024-01-02", 90);
    }

    @Test
    public void headerIsCaseInsensitiveAndBomIsIgnored() {
        DoubleSeries series = parse(
            "﻿ DATE , ADJ CLOSE ",
            "2024-01-02,42");

        assertEntry(series, 0, "2024-01-02", 42);
    }

    @Test
    public void timePartOfDateIsDropped() {
        DoubleSeries series = parse(
            "Date,Close",
            "2024-01-02 00:00:00-05:00,42");

        assertEntry(series, 0, "2024-01-02", 42);
    }

    @Test
    public void noPriceColumnThrows() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> parse("date,open,volume", "2024-01-02,1,100"));

        assertTrue(e.getMessage(), e.getMessage().contains("No price column"));
        assertTrue(e.getMessage(), e.getMessage().contains("adj close"));
    }

    @Test
    public void noDateColumnThrows() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> parse("day,close", "2024-01-02,1"));

        assertTrue(e.getMessage(), e.getMessage().contains("No date column"));
    }

    @Test
    public void badDateThrowsWithLineNumber() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> parse("date,close", "2024-01-02,1", "02.01.2024,2"));

        assertTrue(e.getMessage(), e.getMessage().contains("02.01.2024"));
        assertTrue(e.getMessage(), e.getMessage().contains("line 3"));
    }

    @Test
    public void headerOnlyThrows() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> parse("date,close"));

        assertTrue(e.getMessage(), e.getMessage().contains("No prices found"));
    }

    // ---- helpers ----

    private static DoubleSeries load(String folder, String symbol) throws Exception {
        CsvHistoricalPriceService service = new CsvHistoricalPriceService(fixtureFolder(folder));
        return service.getHistoricalAdjustedPrices(symbol).toBlocking().first();
    }

    private static DoubleSeries parse(String... lines) {
        List<String> list = Arrays.asList(lines);
        return CsvHistoricalPriceService.parseLines(list, "TEST", "test.csv");
    }

    // fixture folders on the test classpath
    private static Path fixtureFolder(String name) throws Exception {
        URL url = CsvHistoricalPriceServiceTest.class.getResource("/csv/" + name + "/");
        return Paths.get(url.toURI());
    }

    private static void assertEntry(DoubleSeries series, int index, String date, double price) {
        assertEquals(Instant.parse(date + "T00:00:00Z"), series.get(index).getInstant());
        assertEquals(price, series.get(index).getItem(), 1e-9);
    }
}
