//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Calculates the percentiles for a numerical data set with float precision.
 * This class uses the <em>inclusive</em> definition of percentiles, meaning
 * a percentile indicates the part of the dataset that is lower than or equal
 * to a value.
 */
public class Percentiles {

    private List<PercentileValue> percentileValues;

    public Percentiles(Collection<? extends Number> dataSet) {
        List<Float> orderedDataSet = dataSet.stream()
            .map(Number::floatValue)
            .sorted()
            .toList();

        if (orderedDataSet.isEmpty()) {
            percentileValues = Collections.emptyList();
        } else if (orderedDataSet.size() == 1) {
            percentileValues = List.of(new PercentileValue(0f, orderedDataSet.get(0)));
        } else {
            percentileValues = new ArrayList<>();

            for (int i = 0; i < orderedDataSet.size(); i++) {
                float percentile = i * (100f / (orderedDataSet.size() - 1));
                float value = orderedDataSet.get(i);
                percentileValues.add(new PercentileValue(percentile, value));
            }
        }
    }

    /**
     * Returns the percentile for the specified value. This method will always
     * return a number in the range between 0 and 100 (inclusive). Values
     * outside the data set are clamped to this range. Always returns zero if
     * the data set contains zero or one data points.
     */
    public float getPercentile(float value) {
        if (percentileValues.size() <= 1) {
            return 0f;
        }

        for (int i = 1; i < percentileValues.size(); i++) {
            PercentileValue previous = percentileValues.get(i - 1);
            PercentileValue next = percentileValues.get(i);

            if (value < next.value) {
                return previous.percentile;
            }
        }

        return 100f;
    }

    /**
     * Returns the value corresponding to the specified percentile in the data
     * set. No interpolation is performed, this will only return values that
     * are included in the data set. Use {@link #interpolateValue(float)} if
     * you want to interpolate <em>between</em> values in the data set. Always
     * returns 0 if the data set is empty.
     *
     * @throws IllegalArgumentException if the percentile is outside the range
     *         between 0 and 100 (inclusive).
     */
    public float getValueForPercentile(float percentile) {
        return getValue(percentile, false);
    }

    /**
     * Returns a value corresponding to the specified percentile in the data
     * set, using linear interpolation between values if there is no exact
     * match. Always returns 0 if the data set is empty.
     *
     * @throws IllegalArgumentException if the percentile is outside the range
     *         between 0 and 100 (inclusive).
     */
    public float interpolateValue(float percentile) {
        return getValue(percentile, true);
    }

    private float getValue(float percentile, boolean interpolate) {
        validatePercentile(percentile);

        if (percentileValues.isEmpty()) {
            return 0f;
        }

        for (int i = 1; i < percentileValues.size(); i++) {
            PercentileValue previous = percentileValues.get(i - 1);
            PercentileValue next = percentileValues.get(i);

            if (percentile < next.percentile) {
                if (interpolate) {
                    return interpolate(percentile, previous, next);
                } else {
                    return previous.value;
                }
            }
        }

        return percentileValues.get(percentileValues.size() - 1).value;
    }

    private float interpolate(float percentile, PercentileValue previous, PercentileValue next) {
        float delta = (percentile - previous.percentile) / (next.percentile - previous.percentile);
        return previous.value + delta * (next.value - previous.value);
    }

    private static void validatePercentile(float percentile) {
        Preconditions.checkArgument(percentile >= 0f && percentile <= 100f,
            "Invalid percentile: " + percentile);
    }

    /**
     * Simple data structure that is used to describe at which percentile a
     * certain value in the data set is located.
     */
    private record PercentileValue(float percentile, float value) {

        public PercentileValue {
            validatePercentile(percentile);
        }
    }
}
