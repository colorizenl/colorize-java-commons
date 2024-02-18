//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Data structure to describe histograms, which can be used to describe
 * the distribution of a numerical data set. The histogram consists of
 * <em>bins</em> and <em>series</em>.
 * <p>
 * Bins act as intervals or “buckets” for categorizing values. Bins can
 * be of any type, but must implement the {@link Comparable} interface,
 * which is used to determine the order of the bins within the histogram.
 * It is possible to add bins with a frequency of zero, for cases where
 * the histogram needs to depict the entire range of bins.
 * <p>
 * Series can be used to provide more information on different categories
 * of data that contribute towards the overall frequency within each bin.
 * Series are purely descriptive text label, and are therefore always of
 * type string. Also unlike bins, series do not have an explicit order,
 * with series being sorted based on their overall frequency within the
 * data set.
 * <p>
 * The following example shows a histogram with multiple bins and multiple
 * series, presented in glorious ASCII art:
 * <p>
 * <pre>
 *   [2]
 *   [1]     [2]
 *   [1]     [1]      [1]
 *   -------------------------------
 *   0-10    11-20    21-30    31-40
 * </pre>
 * <p>
 * This class is not thread-safe, {@link Histogram} instances should therefore
 * not be used concurrently from multiple threads.
 *
 * @param <B> Type of the bins within the histogram.
 */
public class Histogram<B extends Comparable<B>> {

    private SortedSet<B> bins;
    private Map<B, Multiset<String>> frequency;
    private Map<String, Integer> seriesTotals;

    private static final Multiset<String> EMPTY = ImmutableMultiset.of();

    /**
     * Creates a new histogram that is initially empty. Bins will be added
     * on-the-fly as data is added to the histogram.
     */
    public Histogram() {
        this.bins = new TreeSet<>();
        this.frequency = new HashMap<>();
        this.seriesTotals = new HashMap<>();
    }

    /**
     * Creates a new histogram that consists of the specified bins. This can
     * be used in situations where all bins are known up front, or when it
     * is needed to always depict all possible bins in the histogram.
     */
    public Histogram(List<B> initialBins) {
        this();
        for (B bin : initialBins) {
            prepareBin(bin);
        }
    }

    private void prepareBin(B bin) {
        if (!bins.contains(bin)) {
            bins.add(bin);
            frequency.put(bin, HashMultiset.create());
        }
    }

    /**
     * Adds the specified frequency to this histogram. The requested bin
     * and/or series are added to this histogram if they do not yet exist.
     */
    public void count(B bin, String series) {
        count(bin, series, 1);
    }

    /**
     * Adds the specified frequency to this histogram. The requested bin
     * and/or series are added to this histogram if they do not yet exist.
     *
     * @throws IllegalArgumentException when trying to add a negative
     *         frequency. Note that adding zero is in fact allowed, this
     *         will add the bin if it does not exist yet without adding
     *         a frequency to the bin.
     */
    public void count(B bin, String series, int value) {
        Preconditions.checkArgument(value >= 0, "Invalid frequency: " + value);
        Preconditions.checkArgument(!series.trim().isEmpty(), "Empty series name");

        prepareBin(bin);

        if (value > 0) {
            frequency.get(bin).add(series, value);
            seriesTotals.put(series, seriesTotals.getOrDefault(series, 0) + value);
        }
    }

    /**
     * Adds all data from the specified other histogram to this histogram.
     * This includes any bins and/or series that are not yet present in this
     * histogram.
     */
    public void merge(Histogram<B> other) {
        for (B bin : other.bins) {
            for (String series : other.frequency.get(bin)) {
                count(bin, series);
            }
        }
    }

    /**
     * Returns a list of all bins in this histogram. The bins are sorted based
     * on their natural order, i.e. based on the {@link Comparable} interface.
     */
    public List<B> getBins() {
        return List.copyOf(bins);
    }

    /**
     * Returns a list of all series in this histogram. The series are ordered
     * based on overall frequency, with the largest series becoming the first
     * element in the list.
     */
    public List<String> getSeries() {
        return List.copyOf(sortFrequencyMap(seriesTotals).keySet());
    }

    /**
     * Returns the frequency count for the specified bin and series. Returns
     * zero if the bin and/or series do not exist in this histogram.
     */
    public int getFrequency(B bin, String series) {
        return frequency.getOrDefault(bin, EMPTY).count(series);
    }

    /**
     * Returns a map containing all series and corresponding frequency that
     * exist in the specified bin. The iteration order of the map is based
     * on series frequency, with the most common series first. Returns an
     * empty map if no such bin exists.
     */
    public Map<String, Integer> getBinFrequency(B bin) {
        Map<String, Integer> binFrequency = new HashMap<>();
        for (String series : frequency.getOrDefault(bin, EMPTY)) {
            binFrequency.put(series, binFrequency.getOrDefault(series, 0) + 1);
        }
        return sortFrequencyMap(binFrequency);
    }

    /**
     * Returns a list of tuples for all bins in this histogram, with each
     * tuple consisting of the bin and the corresponding frequency for the
     * specified series. The frequency will be zero if no such series exists.
     */
    public TupleList<B, Integer> getSeriesFrequency(String series) {
        TupleList<B, Integer> seriesFrequency = new TupleList<>();
        for (B bin : bins) {
            int binFrequency = frequency.getOrDefault(bin, EMPTY).count(series);
            seriesFrequency.add(bin, binFrequency);
        }
        return seriesFrequency;
    }

    /**
     * Returns the total frequency for the specified bin, combining the
     * frequencies of all series that are included in that bin. Returns zero
     * if no such bin exists in this histogram.
     */
    public int getBinTotal(B bin) {
        return frequency.getOrDefault(bin, EMPTY).size();
    }

    /**
     * Returns the total frequency for the specified series, combining all
     * bins in which the series might exist. Returns zero if no such series
     * exists in this histogram.
     */
    public int getSeriesTotal(String series) {
        return bins.stream()
            .mapToInt(bin -> frequency.getOrDefault(bin, EMPTY).count(series))
            .sum();
    }

    /**
     * Returns map containing the total frequency for all series in this
     * histogram. The iteration order of the map will match
     * {@link #getSeries()}.
     */
    public Map<String, Integer> getSeriesTotals() {
        return sortFrequencyMap(seriesTotals);
    }

    /**
     * Returns a map containing the total frequency for all series in this
     * histogram, but normalized to percentages instead of the absolute
     * numbers. The iteration order of the map will match
     * {@link #getSeries()}. Use {@link #getSeriesTotals()} if you need the
     * absolute numbers.
     */
    public Map<String, Float> getSeriesPercentages() {
        return normalizeFrequencyMap(sortFrequencyMap(seriesTotals));
    }

    /**
     * Returns the combined total frequency of all data in this histogram.
     * This number will match both the sum of all bins and the sum of all
     * series.
     */
    public int getTotal() {
        return bins.stream()
            .mapToInt(bin -> frequency.getOrDefault(bin, EMPTY).size())
            .sum();
    }

    /**
     * Returns a new frequency map that contains the same entries as the
     * original, but sorted by value so that the most common entry comes
     * first in the map's iteration order.
     */
    private Map<String, Integer> sortFrequencyMap(Map<String, Integer> original) {
        List<String> sortedKeys = original.keySet().stream()
            .sorted((a, b) -> original.get(b) - original.get(a))
            .toList();

        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        for (String key : sortedKeys) {
            sortedMap.put(key, original.get(key));
        }
        return sortedMap;
    }

    /**
     * Returns a new frequency map that contains the same entries in the same
     * order as the original, but expressed as percentages instead of absolute
     * numbers.
     */
    private Map<String, Float> normalizeFrequencyMap(Map<String, Integer> original) {
        int total = original.values().stream()
            .mapToInt(value -> value)
            .sum();

        Map<String, Float> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : original.entrySet()) {
            float percentage = Statistics.percentage(entry.getValue(), total);
            normalized.put(entry.getKey(), percentage);
        }
        return normalized;
    }
}
