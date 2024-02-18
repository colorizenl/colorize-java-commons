//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.Quantiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for working with statistics on numerical data sets with float
 * precision. Unless stated otherwise, methods in this class will return zero
 * instead of throwing an exception when faced with an empty data set.
 */
public final class Statistics {

    private Statistics() {
    }

    public static float sum(Collection<? extends Number> values) {
        float sum = 0f;
        for (Number value : values) {
            sum += value.floatValue();
        }
        return sum;
    }

    public static float min(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return 0f;
        }

        float min = Float.MAX_VALUE;
        for (Number value : values) {
            min = Math.min(value.floatValue(), min);
        }
        return min;
    }

    public static float max(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return 0f;
        }

        float max = Float.MIN_VALUE;
        for (Number value : values) {
            max = Math.max(value.floatValue(), max);
        }
        return max;
    }

    public static float average(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return 0f;
        }
        return sum(values) / values.size();
    }

    public static float median(Collection<? extends Number> values) {
        if (values.isEmpty()) {
            return 0f;
        }

        List<Number> sorted = new ArrayList<>(values);
        sorted.sort((a, b) -> Float.compare(a.floatValue(), b.floatValue()));
        return sorted.get(sorted.size() / 2).floatValue();
    }

    public static float fraction(float value, float total) {
        if (total == 0f) {
            return 0f;
        }
        return value / total;
    }

    public static float percentage(float value, float total) {
        if (total == 0f) {
            return 0f;
        }
        return value * 100f / total;
    }

    /**
     * Calculates the value of the Nth percentile for the specified data set.
     * This method will interpolate between values if none of the values in
     * the data set exactly matches the requested percentile.
     *
     * @throws IllegalArgumentException if {@code n} is outside the range
     *         between 0 and 99 and therefore not a valid percentile.
     */
    public static float percentile(Collection<? extends Number> values, int n) {
        Preconditions.checkArgument(n >= 0 && n <= 99, "Invalid percentile: " + n);

        if (values.size() <= 1) {
            return 0f;
        }

        return (float) Quantiles.percentiles()
            .index(n)
            .compute(values);
    }

    /**
     * Calculates the Pearson correlation for the specified two data sets.
     * A correlation of 1.0 indicates perfect correlation, -1.0 indicates
     * a perfect inverse correlation, 0.0 indicates no correlation.
     *
     * @throws IllegalArgumentException if the two data sets are of different
     *         length, as Pearson correlation is based on pairs.
     */
    public static float correlation(List<? extends Number> a, List<? extends Number> b) {
        Preconditions.checkArgument(a.size() == b.size(),
            "Cannot calculate correlation for data sets of different size: " + a + " versus " + b);

        if (a.size() <= 1) {
            return 0f;
        }

        PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
        for (int i = 0; i < a.size(); i++) {
            accumulator.add(a.get(i).floatValue(), b.get(i).floatValue());
        }

        return (float) accumulator.pearsonsCorrelationCoefficient();
    }
}
