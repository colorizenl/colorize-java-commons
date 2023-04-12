//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import nl.colorize.util.CSVRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Categorizes the frequency of data points within different bins. The histogram
 * has two ways to categorize values: the <em>bins</em> are typically depicted on
 * the x-axis, and used to categorize values. The <em>series</em> are typically
 * depicted using different colors, and are used to differentiate categories
 * within the same bin.
 *
 * @param <B> Type of the bins within the histogram.
 */
public class Histogram<B> {

    private List<B> bins;
    private List<String> series;
    private Map<Tuple<String, B>, Integer> values;

    public Histogram() {
        this.bins = new ArrayList<>();
        this.series = new ArrayList<>();
        this.values = new HashMap<>();
    }

    public void addBin(B bin) {
        Preconditions.checkState(!bins.contains(bin), "Bin already exists: " + bin);
        bins.add(bin);
    }

    public void addBins(Iterable<B> bins) {
        for (B bin : bins) {
            addBin(bin);
        }
    }

    public void addSeries(String name) {
        Preconditions.checkState(!series.contains(name), "Series already exists: " + name);
        series.add(name);
        Collections.sort(series);
    }

    public void addSeries(Iterable<String> names) {
        for (String name : names) {
            addSeries(name);
        }
    }

    public void count(String seriesName, B bin) {
        count(seriesName, bin, 1);
    }

    public void count(String seriesName, B bin, int amount) {
        Preconditions.checkArgument(amount >= 0, "Invalid amount: " + amount);

        if (!series.contains(seriesName)) {
            addSeries(seriesName);
        }

        if (!bins.contains(bin)) {
            addBin(bin);
        }

        Tuple<String, B> key = Tuple.of(seriesName, bin);
        values.put(key, values.getOrDefault(key, 0) + amount);
    }

    /**
     * Returns all bins in this histogram, sorted in their natural order.
     */
    public List<B> getBins() {
        return ImmutableList.copyOf(bins);
    }

    /**
     * Returns all series in this histogram, sorted alphabetically.
     */
    public List<String> getSeriesByName() {
        return ImmutableList.copyOf(series);
    }

    /**
     * Returns all series in this histogram, sorted by total frequency so that
     * the highest-scoring series is first in the list.
     */
    public List<String> getSeriesByTotalValue() {
        return series.stream()
            .sorted((a, b) -> getTotalSeriesValue(b) - getTotalSeriesValue(a))
            .toList();
    }

    public int getValue(String seriesName, B bin) {
        Tuple<String, B> key = Tuple.of(seriesName, bin);
        return values.getOrDefault(key, 0);
    }

    public int getTotalSeriesValue(String seriesName) {
        Preconditions.checkArgument(series.contains(seriesName),
            "Unknown series: " + seriesName);

        return values.entrySet().stream()
            .filter(entry -> entry.getKey().left().equals(seriesName))
            .mapToInt(Map.Entry::getValue)
            .sum();
    }

    public int getTotalBinValue(B bin) {
        return values.entrySet().stream()
            .filter(entry -> entry.getKey().right().equals(bin))
            .mapToInt(Map.Entry::getValue)
            .sum();
    }

    public TupleList<B, Integer> getSeriesValues(String seriesName) {
        Preconditions.checkArgument(series.contains(seriesName),
            "Unknown series: " + seriesName);

        TupleList<B, Integer> tuples = TupleList.create();
        for (B bin : bins) {
            tuples.add(Tuple.of(bin, getValue(seriesName, bin)));
        }
        return tuples;
    }

    public Map<String, Integer> getBinValues(B bin) {
        Map<String, Integer> binValues = new LinkedHashMap<>();
        for (String seriesName : series) {
            binValues.put(seriesName, getValue(seriesName, bin));
        }
        return binValues;
    }

    /**
     * Returns the contents of this histogram as CSV records. The histogram's
     * bins will act as columns in the CSV, though they will be prefixed with
     * a column named "series" which will be used to store the series name. If
     * the histogram uses non-string bins, {@code toString()} will be called
     * on the bin objects to obtain the column names. The series in the
     * histogram will be used as rows, with the name of each series acting as
     * the first cell.
     */
    public List<CSVRecord> toCSV() {
        List<String> columns = new ArrayList<>();
        columns.add("series");
        bins.forEach(bin -> columns.add(bin.toString()));

        List<CSVRecord> records = new ArrayList<>();

        for (String seriesName : series) {
            List<String> cells = new ArrayList<>();
            cells.add(seriesName);
            bins.forEach(bin -> cells.add(String.valueOf(getValue(seriesName, bin))));
            records.add(CSVRecord.create(columns, cells));
        }

        return records;
    }

    /**
     * Factory method to create a histogram with the specified bins and series.
     * All bins will be initially set to zero.
     */
    public static <B> Histogram<B> withBins(Iterable<B> bins, Iterable<String> series) {
        Histogram<B> hist = new Histogram<>();
        hist.addBins(bins);
        hist.addSeries(series);
        return hist;
    }

    /**
     * Factory method to create a histogram with the specified bins. All bins
     * will be initially set to zero.
     */
    public static <B> Histogram<B> withBins(Iterable<B> bins) {
        Histogram<B> hist = new Histogram<>();
        hist.addBins(bins);
        return hist;
    }
}
