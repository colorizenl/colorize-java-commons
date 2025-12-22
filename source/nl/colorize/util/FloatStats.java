//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.math.PairedStatsAccumulator;
import com.google.common.math.Quantiles;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Immutable data set that contains numerical values with {@code float}
 * precision. All operations in this class allow the data set to be empty,
 * and will return zero rather than throwing an exception.
 */
public final class FloatStats {

    private float[] values;

    private FloatStats(float[] values) {
        this.values = values;
    }

    public float sum() {
        float sum = 0f;
        for (float value : values) {
            sum += value;
        }
        return sum;
    }

    public float min() {
        if (values.length == 0) {
            return 0f;
        }

        float min = Float.MAX_VALUE;
        for (float value : values) {
            min = Math.min(value, min);
        }
        return min;
    }

    public float max() {
        if (values.length == 0) {
            return 0f;
        }

        float max = Float.MIN_VALUE;
        for (float value : values) {
            max = Math.max(value, max);
        }
        return max;
    }

    public float average() {
        if (values.length == 0) {
            return 0f;
        }
        return sum() / values.length;
    }

    public float median() {
        if (values.length == 0) {
            return 0f;
        }

        float[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    /**
     * Calculates the specified value as a percentage of this data set's total.
     * For example, if this data set consists of [2, 2], then calculating the
     * percentage for 1 would result in 25%.
     */
    public float percentage(float value) {
        if (values.length == 0) {
            return 0f;
        }
        return value * 100f / sum();
    }

    /**
     * Calculates the value of the Nth percentile within this data set.
     * This method will interpolate between values if none of the values in
     * the data set exactly matches the requested percentile.
     *
     * @throws IllegalArgumentException if {@code n} is outside the range
     *         between 0 and 99 and therefore not a valid percentile.
     */
    public float percentile(int n) {
        Preconditions.checkArgument(n >= 0 && n <= 99, "Invalid percentile: " + n);

        if (values.length <= 1) {
            return 0f;
        }

        double[] doubleValues = IntStream.range(0, values.length)
            .mapToDouble(i -> values[i])
            .toArray();

        return (float) Quantiles.percentiles()
            .index(n)
            .compute(doubleValues);
    }

    /**
     * Calculates the Pearson correlation between this data set and the
     * specified other data set. A correlation of 1.0 indicates perfect
     * correlation, -1.0 indicates a perfect inverse correlation, and
     * 0.0 indicates no correlation.
     *
     * @throws IllegalArgumentException if the two data sets are of different
     *         length, as Pearson correlation is based on pairs.
     */
    public float pearsonCorrelation(FloatStats other) {
        Preconditions.checkArgument(values.length == other.values.length,
            "Mismatched data sets: " + values.length + " versus " + other.values.length);

        if (values.length <= 1) {
            return 0f;
        }

        PairedStatsAccumulator accumulator = new PairedStatsAccumulator();
        for (int i = 0; i < values.length; i++) {
            accumulator.add(values[i], other.values[i]);
        }
        return (float) accumulator.pearsonsCorrelationCoefficient();
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }

    /**
     * Creates a {@link FloatStats} data set that consists of the specified
     * values.
     */
    public static FloatStats of(float... values) {
        return new FloatStats(values);
    }

    /**
     * Creates a {@link FloatStats} data set that consists of the specified
     * values. Each value will be converted to a {@code float} by calling
     * {@link Number#floatValue()}.
     */
    public static FloatStats of(Collection<? extends Number> values) {
        float[] floatValues = new float[values.size()];
        int index = 0;

        for (Number number : values) {
            floatValues[index] = number.floatValue();
            index++;
        }

        return new FloatStats(floatValues);
    }

    /**
     * Utility method that returns a percentage for the specified value.
     * Returns zero if {@code total} is zero.
     */
    public static float percentage(float value, float total) {
        if (total == 0f) {
            return 0f;
        }
        return value * 100f / total;
    }

    /**
     * Utility method that multiplies two percentages and returns the
     * resulting percentage. For example, multiplying 50% with 50% will
     * return 25%.
     */
    public static float multiplyPercentage(float percentageA, float percentageB) {
        return ((percentageA / 100f) * (percentageB / 100f)) * 100f;
    }
}
