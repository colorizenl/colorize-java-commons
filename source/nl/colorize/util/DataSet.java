//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
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
 * @param <K> The type of keys by which data points can be identified, i.e. the
 *            key used for rows in the data set.
 * @param <D> The type of keys by which values for a data point can be identified,
 *            i.e. the key used for columns in the data set.
 */
public final class DataSet<K, D> implements Serializable {
	
	private Map<K, DataPoint<K, D>> dataPoints;
	
	private static final Joiner NAME_JOINER = Joiner.on(", ");
	private static final long serialVersionUID = 1;

	public DataSet() {
		dataPoints = new LinkedHashMap<>();
	}
	
	public void add(K row, Map<D, ? extends Number> data, Map<String, String> metadata) {
		add(row, data);
		addMetadata(row, metadata);
	}
	
	public void add(K row, Map<D, ? extends Number> data) {
		for (Map.Entry<D, ? extends Number> entry : data.entrySet()) {
			add(row, entry.getKey(), entry.getValue());
		}
	}
	
	public void add(K row, D column, Number value) {
		DataPoint<K, D> dataPoint = lookupDataPoint(row);
		dataPoint.data.put(column, value);
	}
	
	public void add(K row) {
		lookupDataPoint(row);
	}
	
	private void add(DataPoint<K, D> dataPoint) {
		dataPoints.put(dataPoint.rowKey, dataPoint);
	}
	
	public void addMetadata(K row, Map<String, String> metadata) {
		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			addMetadata(row, entry.getKey(), entry.getValue());
		}
	}
	
	public void addMetadata(K row, String metaKey, String metaValue) {
		DataPoint<K, D> dataPoint = lookupDataPoint(row);
		dataPoint.metadata.put(metaKey, metaValue);
	}
	
	public void remove(K row) {
		dataPoints.remove(row);
	}
	
	public void remove(K row, D column) {
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint != null) {
			dataPoint.data.remove(column);
		}
	}
	
	public void remove(Collection<K> rows) {
		for (K row : rows) {
			dataPoints.remove(row);
		}
	}
	
	public void clear() {
		dataPoints.clear();
	}
	
	public List<K> getRows() {
		return ImmutableList.copyOf(dataPoints.keySet());
	}
	
	public List<D> getColumns() {
		Set<D> columns = new LinkedHashSet<>();
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			columns.addAll(dataPoint.data.keySet());
		}
		return ImmutableList.copyOf(columns);
	}
	
	public boolean contains(K row, D column) {
		return get(row, column) != null;
	}
	
	public boolean containsRow(K row) {
		return dataPoints.containsKey(row);
	}
	
	public boolean containsColumn(D column) {
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
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
	
	private DataPoint<K, D> lookupDataPoint(K row) {
		Preconditions.checkArgument(row != null, "null rows key is not allowed");
		
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint == null) {
			dataPoint = new DataPoint<K, D>(row);
			dataPoints.put(row, dataPoint);
		}
		return dataPoint;
	}
	
	public Number get(K row, D column) {
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.data.get(column);
	}
	
	public Number get(K row, D column, Number defaultValue) {
		Number value = get(row, column);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}
	
	public Map<D, Number> getRow(K row) {
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.data;
	}
	
	public Map<K, Number> getColumn(D column) {
		Map<K, Number> values = new LinkedHashMap<>();
		for (K row : getRows()) {
			Number value = dataPoints.get(row).data.get(column);
			if (value != null) {
				values.put(row, value);
			}
		}
		return values;
	}
	
	public Map<String, String> getMetadata(K row) {
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.metadata;
	}
	
	public String getMetadata(K row, String metaKey) {
		DataPoint<K, D> dataPoint = dataPoints.get(row);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.metadata.get(metaKey);
	}
	
	private Map<K, Number> select(D column, Predicate<K> keyFilter, Predicate<Number> valueFilter) {
		Map<K, Number> selection = new LinkedHashMap<K, Number>();
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			if (keyFilter.apply(dataPoint.rowKey)) {
				Number value = dataPoint.data.get(column);
				if (value != null && valueFilter.apply(value)) {
					selection.put(dataPoint.rowKey, value);
				}
			}
		}
		return selection;
	}
	
	public Map<K, Number> select(D column) {
		return select(column, noFilter());
	}
	
	public Map<K, Number> select(D column, Predicate<K> filter) {
		return select(column, filter, hasValueFilter());
	}
	
	public Tuple<K, Number> selectFirst(D column) {
		return selectFirst(column, noFilter());
	}
	
	public Tuple<K, Number> selectFirst(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		if (selection.isEmpty()) {
			return null;
		}
		List<K> keys = ImmutableList.copyOf(selection.keySet());
		return Tuple.of(keys.get(0), selection.get(keys.get(0)));
	}
	
	public Tuple<K, Number> selectLast(D column) {
		return selectLast(column, noFilter());
	}
	
	public Tuple<K, Number> selectLast(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		if (selection.isEmpty()) {
			return null;
		}
		List<K> keys = ImmutableList.copyOf(selection.keySet());
		return Tuple.of(keys.get(keys.size() - 1), selection.get(keys.get(keys.size() - 1)));
	}
	
	public Map<K, Number> selectExactly(D column, double value, double epsilon) {
		return selectExactly(column, noFilter(), value, epsilon);
	}
	
	public Map<K, Number> selectExactly(D column, Predicate<K> filter, final double value, 
			final double epsilon) {
		return select(column, filter, new Predicate<Number>() {
			public boolean apply(Number input) {
				return input.doubleValue() >= value - epsilon && input.doubleValue() <= value + epsilon;
			}
		});
	}
	
	public Map<K, Number> selectAtLeast(D column, double minValue) {
		return selectAtLeast(column, noFilter(), minValue);
	}
	
	public Map<K, Number> selectAtLeast(D column, Predicate<K> filter, final double minValue) {
		return select(column, filter, new Predicate<Number>() {
			public boolean apply(Number input) {
				return input.doubleValue() >= minValue;
			}
		});
	}
	
	public Map<K, Number> selectAtMost(D column, double maxValue) {
		return selectAtMost(column, noFilter(), maxValue);
	}
	
	public Map<K, Number> selectAtMost(D column, Predicate<K> filter, final double maxValue) {
		return select(column, filter, new Predicate<Number>() {
			public boolean apply(Number input) {
				return input.doubleValue() <= maxValue;
			}
		});
	}
	
	private void checkSelectionSize(Map<K, Number> selection, int minSize) {
		if (selection.size() < minSize) {
			throw new IllegalStateException("Data set selection is too small, " + 
					"need at least " + minSize + " elements but got " + selection.size());
		}
	}
	
	public Number calculate(D column, Function<Map<K, Number>, Number> func) {
		return calculate(column, noFilter(), func);
	}
	
	public Number calculate(D column, Predicate<K> filter, Function<Map<K, Number>, Number> func) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 1);
		return func.apply(selection);
	}
	
	public Number calculateSum(D column) {
		return calculateSum(column, noFilter());
	}
	
	public Number calculateSum(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		
		double sum = 0.0;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			sum += dataPoint.getValue().doubleValue();
		}
		return sum;
	}
	
	public Tuple<K, Number> calculateMin(D column) {
		return calculateMin(column, noFilter());
	}
	
	public Tuple<K, Number> calculateMin(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 1);
		
		double min = Double.MAX_VALUE;
		K minKey = null;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			if (dataPoint.getValue().doubleValue() < min) {
				min = dataPoint.getValue().doubleValue();
				minKey = dataPoint.getKey();
			}
		}
		return Tuple.of(minKey, (Number) min);
	}
	
	private Tuple<K, Number> calculateMax(Map<K, Number> selection) {
		checkSelectionSize(selection, 1);
		
		double max = Double.MIN_VALUE;
		K maxKey = null;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			if (dataPoint.getValue().doubleValue() > max) {
				max = dataPoint.getValue().doubleValue();
				maxKey = dataPoint.getKey();
			}
		}
		return Tuple.of(maxKey, (Number) max);
	}
	
	public Tuple<K, Number> calculateMax(D column) {
		return calculateMax(column, noFilter());
	}
	
	public Tuple<K, Number> calculateMax(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 1);
		return calculateMax(selection);
	}
	
	public Number calculateAverage(D column) {
		return calculateAverage(column, noFilter());
	}
	
	public Number calculateAverage(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 1);
		return Stats.meanOf(selection.values());
	}
	
	public Number calculateMedian(D column) {
		return calculateMedian(column, noFilter());
	}
	
	public Number calculateMedian(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 1);
		
		double[] selectionValues = toValuesArray(selection);
		Arrays.sort(selectionValues);
		return selectionValues[selection.size() / 2];
	}
	
	public Number calculateWeightedAverage(D column, D weightColumn) {
		return calculateWeightedAverage(column, weightColumn, noFilter());
	}
	
	public Number calculateWeightedAverage(D column, D weightColumn, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkSelectionSize(selection, 2);
		
		double weightedSum = 0.0;
		double weightedCount = 0.0;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			double weight = get(dataPoint.getKey(), weightColumn).doubleValue();
			weightedSum += dataPoint.getValue().doubleValue() * weight;
			weightedCount += weight;
		}
		
		if (weightedCount > 0.0) {
			return weightedSum / weightedCount;
		} else {
			return Stats.meanOf(selection.values());
		}
	}
	
	public Map<K, Number> calculatePercentiles(D column) {
		return calculatePercentiles(column, noFilter());
	}
	
	public Map<K, Number> calculatePercentiles(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		if (selection.isEmpty()) {
			return Collections.emptyMap();
		} else if (selection.size() == 1) {
			return ImmutableMap.of(selection.keySet().iterator().next(), (Number) 100f);
		}
		
		List<K> sortedRows = sortSelectionRows(selection);
		double lowest = selection.get(sortedRows.get(0)).doubleValue();
		double highest = selection.get(sortedRows.get(sortedRows.size() - 1)).doubleValue();
		
		Map<K, Number> percentiles = new LinkedHashMap<>();
		percentiles.put(sortedRows.get(0), 1.0);
		for (int i = 1; i < sortedRows.size() - 1; i++) {
			double percentile = (selection.get(sortedRows.get(i)).doubleValue() - lowest) / 
					(highest - lowest) * 100.0;
			percentiles.put(sortedRows.get(i), percentile);
		}
		percentiles.put(sortedRows.get(sortedRows.size() - 1), 100.0);
		return percentiles;
	}
	
	public Number calculatePercentile(K row, D column) {
		return calculatePercentile(row, column, noFilter());
	}
	
	public Number calculatePercentile(K row, D column, Predicate<K> filter) {
		return calculatePercentiles(column, filter).get(row);
	}
	
	public void sort(final D column, final Comparator<Number> sortFunction) {
		List<K> sortedKeys = new ArrayList<>(dataPoints.keySet());
		Collections.sort(sortedKeys, new Comparator<K>() {
			public int compare(K a, K b) {
				return sortFunction.compare(dataPoints.get(a).data.get(column), 
						dataPoints.get(b).data.get(column));
			}
		});
		
		Map<K, DataPoint<K, D>> copy = new LinkedHashMap<>(dataPoints);
		dataPoints.clear();
		for (K sortedKey : sortedKeys) {
			dataPoints.put(sortedKey, copy.get(sortedKey));
		}
	}
	
	public void sortAscending(D column) {
		sort(column, numericalComparator(false));
	}
	
	public void sortDescending(D column) {
		sort(column, numericalComparator(true));
	}
	
	private List<K> sortSelectionRows(final Map<K, Number> selection) {
		List<K> sortedRows = new ArrayList<>(selection.keySet());
		Collections.sort(sortedRows, new Comparator<K>() {
			public int compare(K a, K b) {
				if (selection.get(a).doubleValue() < selection.get(b).doubleValue()) {
					return -1;
				} else if (selection.get(a).doubleValue() > selection.get(b).doubleValue()) {
					return 1;
				} else {
					return 0;
				}
			}
		});
		return sortedRows;
	}
	
	/**
	 * Inserts a value for the specified column. Any value already existing will
	 * be replaced.
	 */
	public void fill(D column, Number value) {
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			dataPoint.data.put(column, value);
		}
	}
	
	/**
	 * Inserts a value for the specified column, but only for the rows that do
	 * not yet have a value for this column.
	 */
	public void fillMissing(D column, Number value) {
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			if (dataPoint.data.get(column) == null) {
				dataPoint.data.put(column, value);
			}
		}
	}
	
	private Predicate<K> noFilter() {
		return Predicates.alwaysTrue();
	}
	
	private Predicate<Number> hasValueFilter() {
		return Predicates.notNull();
	}
	
	private Comparator<Number> numericalComparator(final boolean reverse) {
		// This method is only required because all subclasses of 
		// Number  (e.g. Integer, Float, Double, etc.) all implement 
		// {@link Comparable}, but Number itself does not.
		Comparator<Number> numericalComparator = new Comparator<Number>() {
			public int compare(Number a, Number b) {
				double result = a.doubleValue() - b.doubleValue();
				if (reverse) {
					result = -result;
				}
				return (int) Math.round(result);
			}
		};
		return Ordering.from(numericalComparator).nullsLast();
	}
	
	/**
	 * Takes a selection from the data set, and converts its values to an array
	 * of doubles.
	 */
	public double[] toValuesArray(Map<K, Number> selection) {
		return Doubles.toArray(selection.values());
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
	public Map<K, Number> normalize(Map<K, Number> selection) {
		double max = calculateMax(selection).getRight().doubleValue();
		
		if (max == 0.0) {
			return ImmutableMap.copyOf(selection);
		}
		
		Map<K, Number> normalized = new LinkedHashMap<>();
		for (Map.Entry<K, Number> entry : selection.entrySet()) {
			double normalizedValue = entry.getValue().doubleValue() / max;
			normalized.put(entry.getKey(), normalizedValue);
		}
		return normalized;
	}
	
	/**
	 * Creates a shallow copy of this dataset.
	 */
	public DataSet<K, D> copy() {
		DataSet<K, D> copy = new DataSet<K, D>();
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			DataPoint<K, D> dataPointCopy = new DataPoint<K, D>(dataPoint.rowKey);
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
