//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.math.Stats;
import com.google.common.primitives.Doubles;

/**
 * Collection of data points that can be used for (statistical) analysis. Each
 * data point consists of a row key by which the data point can be identified, 
 * and a number of numerical data values and textual metadata stored for that 
 * row. In addition to storing and retrieving the data, this class also contains
 * methods to select data points based on certain conditions, and to perform 
 * basic statistical calculations on the entire data set or on those selections.
 * The data set can also be sorted based on a certain column, the default order
 * is based on the order in which the data points were added. 
 * <p>
 * The design of this class attempts to be similar to concepts from statistical
 * environments, such as data frames in <a href="https://www.r-project.org">R</a>.
 * <p>
 * This class is not thread safe. The data set should not be accessed or modified
 * from multiple threads, unless access is synchronized externally.
 * @param <R> The type of keys by which data points can be identified, i.e. the
 *            key used for rows in the data set.
 * @param <C> The type of keys by which values for a data point can be identified,
 *            i.e. the key used for columns in the data set.
 */
public final class DataSet<R, C> implements Serializable {
    
    private Map<R, DataPoint<R, C>> dataPoints;
    
    private static final Joiner NAME_JOINER = Joiner.on(", ");
    private static final long serialVersionUID = 1;

    public DataSet() {
        dataPoints = new LinkedHashMap<>();
    }
    
    public void add(R row, Map<C, ? extends Number> data, Map<String, String> metadata) {
        add(row, data);
        addMetadata(row, metadata);
    }
    
    public void add(R row, Map<C, ? extends Number> data) {
        for (Map.Entry<C, ? extends Number> entry : data.entrySet()) {
            add(row, entry.getKey(), entry.getValue());
        }
    }
    
    public void add(R row, C column, Number value) {
        DataPoint<R, C> dataPoint = lookupDataPoint(row);
        dataPoint.data.put(column, value);
    }
    
    public void add(R row) {
        lookupDataPoint(row);
    }
    
    private void add(DataPoint<R, C> dataPoint) {
        dataPoints.put(dataPoint.rowKey, dataPoint);
    }
    
    public void addMetadata(R row, Map<String, String> metadata) {
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            addMetadata(row, entry.getKey(), entry.getValue());
        }
    }
    
    public void addMetadata(R row, String metaKey, String metaValue) {
        DataPoint<R, C> dataPoint = lookupDataPoint(row);
        dataPoint.metadata.put(metaKey, metaValue);
    }
    
    /**
     * Inserts a value for the specified column. Any value already existing will
     * be replaced.
     */
    public void fill(C column, Number value) {
        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            dataPoint.data.put(column, value);
        }
    }
    
    /**
     * Inserts a value for the specified column, but only for the rows that do
     * not yet have a value for this column.
     */
    public void fillMissing(C column, Number value) {
        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            if (dataPoint.data.get(column) == null) {
                dataPoint.data.put(column, value);
            }
        }
    }
    
    public void remove(R row) {
        dataPoints.remove(row);
    }
    
    public void remove(R row, C column) {
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint != null) {
            dataPoint.data.remove(column);
        }
    }
    
    public void remove(Collection<R> rows) {
        for (R row : rows) {
            dataPoints.remove(row);
        }
    }
    
    public void clear() {
        dataPoints.clear();
    }
    
    public List<R> getRows() {
        return ImmutableList.copyOf(dataPoints.keySet());
    }
    
    public List<C> getColumns() {
        Set<C> columns = new LinkedHashSet<>();
        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            columns.addAll(dataPoint.data.keySet());
        }
        return ImmutableList.copyOf(columns);
    }
    
    public boolean contains(R row, C column) {
        return get(row, column) != null;
    }
    
    public boolean containsRow(R row) {
        return dataPoints.containsKey(row);
    }
    
    public boolean containsColumn(C column) {
        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            if (dataPoint.data.containsKey(column)) {
                return true;
            }
        }
        return false;
    }
    
    public int getNumDataPoints() {
        return dataPoints.size();
    }
    
    public boolean isEmpty() {
        return dataPoints.isEmpty();
    }
    
    private DataPoint<R, C> lookupDataPoint(R row) {
        Preconditions.checkArgument(row != null, "null rows key is not allowed");
        
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint == null) {
            dataPoint = new DataPoint<R, C>(row);
            dataPoints.put(row, dataPoint);
        }
        return dataPoint;
    }
    
    public Number get(R row, C column) {
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint == null) {
            return null;
        }
        return dataPoint.data.get(column);
    }
    
    public Number get(R row, C column, Number defaultValue) {
        Number value = get(row, column);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    
    public Map<C, Number> getRow(R row) {
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint == null) {
            return null;
        }
        return dataPoint.data;
    }
    
    public Map<R, Number> getColumn(C column) {
        Map<R, Number> values = new LinkedHashMap<>();
        for (R row : getRows()) {
            Number value = dataPoints.get(row).data.get(column);
            if (value != null) {
                values.put(row, value);
            }
        }
        return values;
    }
    
    public Map<String, String> getMetadata(R row) {
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint == null) {
            return null;
        }
        return dataPoint.metadata;
    }
    
    public String getMetadata(R row, String metaKey) {
        DataPoint<R, C> dataPoint = dataPoints.get(row);
        if (dataPoint == null) {
            return null;
        }
        return dataPoint.metadata.get(metaKey);
    }
    
    private Map<R, Number> select(C column, Predicate<R> keyFilter, Predicate<Number> valueFilter) {
        Map<R, Number> selection = new LinkedHashMap<>();

        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            if (keyFilter.test(dataPoint.rowKey)) {
                Number value = dataPoint.data.get(column);
                if (value != null && valueFilter.test(value)) {
                    selection.put(dataPoint.rowKey, value);
                }
            }
        }

        return selection;
    }
    
    public Map<R, Number> select(C column) {
        return select(column, noFilter());
    }
    
    public Map<R, Number> select(C column, Predicate<R> filter) {
        return select(column, filter, hasValueFilter());
    }
    
    public Tuple<R, Number> selectFirst(C column) {
        return selectFirst(column, noFilter());
    }
    
    public Tuple<R, Number> selectFirst(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        if (selection.isEmpty()) {
            return null;
        }
        List<R> keys = ImmutableList.copyOf(selection.keySet());
        return Tuple.of(keys.get(0), selection.get(keys.get(0)));
    }
    
    public Tuple<R, Number> selectLast(C column) {
        return selectLast(column, noFilter());
    }
    
    public Tuple<R, Number> selectLast(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        if (selection.isEmpty()) {
            return null;
        }
        List<R> keys = ImmutableList.copyOf(selection.keySet());
        return Tuple.of(keys.get(keys.size() - 1), selection.get(keys.get(keys.size() - 1)));
    }
    
    public Map<R, Number> selectExactly(C column, double value, double epsilon) {
        return selectExactly(column, noFilter(), value, epsilon);
    }
    
    public Map<R, Number> selectExactly(C column, Predicate<R> filter, final double value,
                                        final double epsilon) {
        return select(column, filter,
            input -> input.doubleValue() >= value - epsilon && input.doubleValue() <= value + epsilon);
    }
    
    public Map<R, Number> selectAtLeast(C column, double minValue) {
        return selectAtLeast(column, noFilter(), minValue);
    }
    
    public Map<R, Number> selectAtLeast(C column, Predicate<R> filter, final double minValue) {
        return select(column, filter, input ->input.doubleValue() >= minValue);
    }
    
    public Map<R, Number> selectAtMost(C column, double maxValue) {
        return selectAtMost(column, noFilter(), maxValue);
    }
    
    public Map<R, Number> selectAtMost(C column, Predicate<R> filter, final double maxValue) {
        return select(column, filter, input ->input.doubleValue() <= maxValue);
    }
    
    private void checkSelectionSize(Map<R, Number> selection, int minSize) {
        if (selection.size() < minSize) {
            throw new IllegalStateException("Data set selection is too small, " + 
                    "need at least " + minSize + " elements but got " + selection.size());
        }
    }

    public double calculateSum(C column) {
        return calculateSum(column, noFilter());
    }
    
    public double calculateSum(C column, Predicate<R> filter) {
        return select(column, filter).values().stream()
            .mapToDouble(value -> value.doubleValue())
            .sum();
    }
    
    public Tuple<R, Number> calculateMin(C column) {
        return calculateMin(column, noFilter());
    }
    
    public Tuple<R, Number> calculateMin(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        checkSelectionSize(selection, 1);
        
        double min = Double.MAX_VALUE;
        R minKey = null;

        for (Map.Entry<R, Number> dataPoint : selection.entrySet()) {
            if (dataPoint.getValue().doubleValue() < min) {
                min = dataPoint.getValue().doubleValue();
                minKey = dataPoint.getKey();
            }
        }

        return Tuple.of(minKey, min);
    }
    
    private Tuple<R, Number> calculateMax(Map<R, Number> selection) {
        checkSelectionSize(selection, 1);
        
        double max = Double.MIN_VALUE;
        R maxKey = null;

        for (Map.Entry<R, Number> dataPoint : selection.entrySet()) {
            if (dataPoint.getValue().doubleValue() > max) {
                max = dataPoint.getValue().doubleValue();
                maxKey = dataPoint.getKey();
            }
        }

        return Tuple.of(maxKey, max);
    }
    
    public Tuple<R, Number> calculateMax(C column) {
        return calculateMax(column, noFilter());
    }
    
    public Tuple<R, Number> calculateMax(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        checkSelectionSize(selection, 1);
        return calculateMax(selection);
    }
    
    public double calculateAverage(C column) {
        return calculateAverage(column, noFilter());
    }
    
    public double calculateAverage(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        if (selection.isEmpty()) {
            return 0.0;
        }
        return Stats.meanOf(selection.values());
    }
    
    public double calculateMedian(C column) {
        return calculateMedian(column, noFilter());
    }
    
    public double calculateMedian(C column, Predicate<R> filter) {
        List<Double> sortedValues = select(column, filter).values().stream()
            .map(value -> value.doubleValue())
            .sorted()
            .collect(Collectors.toList());

        if (sortedValues.isEmpty()) {
            return 0.0;
        }

        return sortedValues.get(sortedValues.size() / 2);
    }
    
    public double calculateWeightedAverage(C column, C weightColumn) {
        return calculateWeightedAverage(column, weightColumn, noFilter());
    }
    
    public double calculateWeightedAverage(C column, C weightColumn, Predicate<R> filter) {
        List<Double> values = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        for (Map.Entry<R, Number> entry : select(column, filter).entrySet()) {
            values.add(get(entry.getKey(), column).doubleValue());
            weights.add(get(entry.getKey(), weightColumn).doubleValue());
        }

        double weightedSum = 0.0;
        double weightedCount = 0.0;
        for (int i = 0; i < values.size(); i++) {
            weightedSum += values.get(i) * weights.get(i);
            weightedCount += weights.get(i);
        }

        if (weightedSum == 0.0) {
            return 0.0;
        }

        return weightedSum / weightedCount;
    }
    
    public Map<R, Number> calculatePercentiles(C column) {
        return calculatePercentiles(column, noFilter());
    }
    
    public Map<R, Number> calculatePercentiles(C column, Predicate<R> filter) {
        Map<R, Number> selection = select(column, filter);
        if (selection.isEmpty()) {
            return Collections.emptyMap();
        } else if (selection.size() == 1) {
            return ImmutableMap.of(selection.keySet().iterator().next(), (Number) 100f);
        }
        
        List<R> sortedRows = sortSelectionRows(selection);
        double interval = 100.0 / (sortedRows.size() - 1);
        Map<R, Number> percentiles = new LinkedHashMap<>();
        
        for (int i = 0; i < sortedRows.size(); i++) {
            percentiles.put(sortedRows.get(i), i * interval);
        }
        
        return percentiles;
    }
    
    public double calculatePercentile(R row, C column) {
        return calculatePercentile(row, column, noFilter());
    }
    
    public double calculatePercentile(R row, C column, Predicate<R> filter) {
        return calculatePercentiles(column, filter).get(row).doubleValue();
    }
    
    /**
     * Calculates the percentage of rows that matches the specified filter.
     */
    public double calculatePercentage(Predicate<R> filter) {
        if (dataPoints.isEmpty()) {
            return 0.0;
        }
        
        int matches = 0;
        for (R row : dataPoints.keySet()) {
            if (filter.test(row)) {
                matches++;
            }
        }
        
        return matches * 100.0 / dataPoints.size();
    }
    
    /**
     * Takes a selection from the data set, and normalizes its values so that
     * the highest value in the selection is 1.0 and all other values are
     * proportional to that in the range 0.0 - 1.0. If the selection contains
     * negative values, those values are normalized in the range -1.0 - 0.0.
     * The returned value is a normalized version of the selection, the original
     * selection itself is left untouched.
     * @throws IllegalArgumentException if the selection is empty.
     */
    public Map<R, Number> calculateNormalized(C column) {
        Map<R, Number> selection = select(column, noFilter());
        double max = calculateMax(selection).getRight().doubleValue();
        
        if (max == 0.0) {
            return ImmutableMap.copyOf(selection);
        }
        
        Map<R, Number> normalized = new LinkedHashMap<>();
        for (Map.Entry<R, Number> entry : selection.entrySet()) {
            double normalizedValue = entry.getValue().doubleValue() / max;
            normalized.put(entry.getKey(), normalizedValue);
        }
        return normalized;
    }
    
    public void sort(final C column, final Comparator<Number> sortFunction) {
        List<R> sortedKeys = new ArrayList<>(dataPoints.keySet());
        Collections.sort(sortedKeys, (a, b) -> {
            return sortFunction.compare(dataPoints.get(a).data.get(column), dataPoints.get(b).data.get(column));
        });
        
        Map<R, DataPoint<R, C>> copy = new LinkedHashMap<>(dataPoints);
        dataPoints.clear();
        for (R sortedKey : sortedKeys) {
            dataPoints.put(sortedKey, copy.get(sortedKey));
        }
    }
    
    public void sortAscending(C column) {
        sort(column, numericalComparator(false));
    }
    
    public void sortDescending(C column) {
        sort(column, numericalComparator(true));
    }
    
    private List<R> sortSelectionRows(final Map<R, Number> selection) {
        List<R> sortedRows = new ArrayList<>(selection.keySet());
        Collections.sort(sortedRows, (a, b) -> {
            if (selection.get(a).doubleValue() < selection.get(b).doubleValue()) {
                return -1;
            } else if (selection.get(a).doubleValue() > selection.get(b).doubleValue()) {
                return 1;
            } else {
                return 0;
            }
        });
        return sortedRows;
    }
    
    private Predicate<R> noFilter() {
        return Predicates.alwaysTrue();
    }
    
    private Predicate<Number> hasValueFilter() {
        return Predicates.notNull();
    }
    
    private Comparator<Number> numericalComparator(final boolean reverse) {
        // This method is only required because all subclasses of 
        // Number  (e.g. Integer, Float, Double, etc.) all implement 
        // {@link Comparable}, but Number itself does not.
        Comparator<Number> numericalComparator = (a, b) -> {
            double result = a.doubleValue() - b.doubleValue();
            if (reverse) {
                result = -result;
            }
            return (int) Math.round(result);
        };
        return Ordering.from(numericalComparator).nullsLast();
    }
    
    /**
     * Takes a selection from the data set, and converts its values to an array
     * of doubles.
     */
    public double[] toValuesArray(Map<R, Number> selection) {
        return Doubles.toArray(selection.values());
    }
    
    /**
     * Creates a shallow copy of this dataset.
     */
    public DataSet<R, C> copy() {
        DataSet<R, C> copy = new DataSet<R, C>();
        for (DataPoint<R, C> dataPoint : dataPoints.values()) {
            DataPoint<R, C> dataPointCopy = new DataPoint<R, C>(dataPoint.rowKey);
            dataPointCopy.data.putAll(dataPoint.data);
            dataPointCopy.metadata.putAll(dataPoint.metadata);
            copy.add(dataPointCopy);
        }
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("DataSet");
        buffer.append("\n    Rows: ").append(NAME_JOINER.join(getRows()));
        buffer.append("\n    Columns: ").append(NAME_JOINER.join(getColumns()));
        buffer.append("\n");
        return buffer.toString();
    }
    
    /**
     * Stores all data, both numerical data and metadata, for a data point.
     */
    private static class DataPoint<K, D> implements Serializable {
        
        private K rowKey;
        private Map<D, Number> data;
        private Map<String, String> metadata;

        private static final long serialVersionUID = 1;
        
        public DataPoint(K rowKey) {
            this.rowKey = rowKey;
            this.data = new HashMap<>();
            this.metadata = new HashMap<>();
        }
        
        @Override
        public String toString() {
            return rowKey + " = " + data;
        }
    }
}
