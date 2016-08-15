//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Doubles;

/**
 * Collection of data points that can be used for (statistical) analysis. Each
 * data point consists of a key by which the data point can be identified, and
 * a number of numerical data values and textual metadata stored for that key.
 * In addition to storing and retrieving the data, this class also contains
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
public class DataSet<K, D> implements Serializable {
	
	private Map<K, DataPoint<K, D>> dataPoints;
	
	private static final long serialVersionUID = 1;

	public DataSet() {
		dataPoints = new LinkedHashMap<>();
	}
	
	public void addDataPoint(K key, Map<D, ? extends Number> data, Map<String, String> metadata) {
		addDataPoint(key, data);
		addMetadata(key, metadata);
	}
	
	public void addDataPoint(K key, Map<D, ? extends Number> data) {
		for (Map.Entry<D, ? extends Number> entry : data.entrySet()) {
			addDataPoint(key, entry.getKey(), entry.getValue());
		}
	}
	
	public void addDataPoint(K key, D column, Number value) {
		DataPoint<K, D> dataPoint = lookupDataPoint(key);
		dataPoint.data.put(column, value);
	}
	
	public void addDataPoint(K key) {
		lookupDataPoint(key);
	}
	
	private void addDataPoint(DataPoint<K, D> dataPoint) {
		dataPoints.put(dataPoint.key, dataPoint);
	}
	
	public void addMetadata(K key, Map<String, String> metadata) {
		for (Map.Entry<String, String> entry : metadata.entrySet()) {
			addMetadata(key, entry.getKey(), entry.getValue());
		}
	}
	
	public void addMetadata(K key, String metaKey, String metaValue) {
		DataPoint<K, D> dataPoint = lookupDataPoint(key);
		dataPoint.metadata.put(metaKey, metaValue);
	}
	
	public void removeDataPoint(K key) {
		dataPoints.remove(key);
	}
	
	public void removeDataPoint(K key, D column) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint != null) {
			dataPoint.data.remove(column);
		}
	}
	
	public void removeDataPoints(Collection<K> keys) {
		for (K key : keys) {
			dataPoints.remove(key);
		}
	}
	
	public void clear() {
		dataPoints.clear();
	}
	
	public List<K> getKeys() {
		return ImmutableList.copyOf(dataPoints.keySet());
	}
	
	public Set<D> getColumns() {
		Set<D> columns = new HashSet<>();
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			columns.addAll(dataPoint.data.keySet());
		}
		return columns;
	}
	
	public int getNumDataPoints() {
		return dataPoints.size();
	}
	
	public boolean hasDataPoint(K key) {
		return dataPoints.containsKey(key);
	}
	
	public boolean isEmpty() {
		return dataPoints.isEmpty();
	}
	
	private DataPoint<K, D> lookupDataPoint(K key) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint == null) {
			dataPoint = new DataPoint<K, D>(key);
			dataPoints.put(key, dataPoint);
		}
		return dataPoint;
	}
	
	public Map<D, Number> getDataPoint(K key) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.data;
	}
	
	public Number getData(K key, D column) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.data.get(column);
	}
	
	public Map<String, String> getMetadata(K key) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.metadata;
	}
	
	public String getMetadata(K key, String metaKey) {
		DataPoint<K, D> dataPoint = dataPoints.get(key);
		if (dataPoint == null) {
			return null;
		}
		return dataPoint.metadata.get(metaKey);
	}
	
	private Map<K, Number> select(D column, Predicate<K> keyFilter, Predicate<Number> valueFilter) {
		Map<K, Number> selection = new LinkedHashMap<K, Number>();
		for (DataPoint<K, D> dataPoint : dataPoints.values()) {
			if (keyFilter.apply(dataPoint.key)) {
				Number value = dataPoint.data.get(column);
				if (value != null && valueFilter.apply(value)) {
					selection.put(dataPoint.key, value);
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
		Map<K, Number> selection = select(column);
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
	
	private void checkCalculationPreconditions(Map<K, Number> selection) {
		if (selection.isEmpty()) {
			throw new IllegalStateException("Data set selection is empty");
		}
	}
	
	public Number calculate(D column, Function<Map<K, Number>, Number> func) {
		return calculate(column, noFilter(), func);
	}
	
	public Number calculate(D column, Predicate<K> filter, Function<Map<K, Number>, Number> func) {
		Map<K, Number> selection = select(column, filter);
		checkCalculationPreconditions(selection);
		return func.apply(selection);
	}
	
	public Number calculateSum(D column) {
		return calculateSum(column, noFilter());
	}
	
	public Number calculateSum(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkCalculationPreconditions(selection);
		
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
		checkCalculationPreconditions(selection);
		
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
		checkCalculationPreconditions(selection);
		
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
		checkCalculationPreconditions(selection);
		return calculateMax(selection);
	}
	
	public Number calculateAverage(D column) {
		return calculateAverage(column, noFilter());
	}
	
	public Number calculateAverage(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkCalculationPreconditions(selection);
		
		double total = 0.0;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			total += dataPoint.getValue().doubleValue();
		}
		return total / selection.size();
	}
	
	public Number calculateMedian(D column) {
		return calculateMedian(column, noFilter());
	}
	
	public Number calculateMedian(D column, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkCalculationPreconditions(selection);
		
		double[] selectionValues = toValuesArray(selection);
		Arrays.sort(selectionValues);
		return selectionValues[selection.size() / 2];
	}
	
	public Number calculateWeightedAverage(D column, D weightColumn) {
		return calculateWeightedAverage(column, weightColumn, noFilter());
	}
	
	public Number calculateWeightedAverage(D column, D weightColumn, Predicate<K> filter) {
		Map<K, Number> selection = select(column, filter);
		checkCalculationPreconditions(selection);
		
		double weightedSum = 0.0;
		double weightedCount = 0.0;
		for (Map.Entry<K, Number> dataPoint : selection.entrySet()) {
			double weight = getData(dataPoint.getKey(), weightColumn).doubleValue();
			weightedSum += dataPoint.getValue().doubleValue() * weight;
			weightedCount += weight;
		}
		return weightedSum / weightedCount;
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
			DataPoint<K, D> dataPointCopy = new DataPoint<K, D>(dataPoint.key);
			dataPointCopy.data.putAll(dataPoint.data);
			dataPointCopy.metadata.putAll(dataPoint.metadata);
			copy.addDataPoint(dataPointCopy);
		}
		return copy;
	}
	
	/**
	 * Stores all data, both numerical data and metadata, for a data point.
	 */
	private static class DataPoint<K, D> implements Serializable {
		
		private K key;
		private Map<D, Number> data;
		private Map<String, String> metadata;

		private static final long serialVersionUID = 1;
		
		public DataPoint(K key) {
			this.key = key;
			this.data = new HashMap<>();
			this.metadata = new HashMap<>();
		}
	}
}
