//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Aggregation functions that operate on a numerical data set and return the
 * aggregated value with float precision.
 */
@FunctionalInterface
public interface Aggregate {

    public static Aggregate COUNT = values -> values.size();

    public static Aggregate SUM = values -> {
        float sum = 0f;
        for (Number value : values) {
            sum += value.floatValue();
        }
        return sum;
    };

    public static Aggregate MIN = values -> {
        if (values.isEmpty()) {
            return 0f;
        }

        float min = values.iterator().next().floatValue();
        for (Number value : values) {
            min = Math.min(value.floatValue(), min);
        }
        return min;
    };

    public static Aggregate MAX = values -> {
        if (values.isEmpty()) {
            return 0f;
        }

        float max = values.iterator().next().floatValue();
        for (Number value : values) {
            max = Math.max(value.floatValue(), max);
        }
        return max;
    };

    public static Aggregate AVERAGE = values -> {
        if (values.isEmpty()) {
            return 0f;
        }

        float sum = SUM.calc(values);
        float count = values.size();
        return sum / count;
    };

    public static Aggregate MEDIAN = values -> {
        if (values.isEmpty()) {
            return 0f;
        }

        List<Number> sorted = new ArrayList<>(values);
        sorted.sort((a, b) -> Float.compare(a.floatValue(), b.floatValue()));
        return sorted.get(sorted.size() / 2).floatValue();
    };

    /**
     * Aggregates the specified values, and returns the result with float
     * precision. Returns zero if the data set is empty.
     */
    public float calc(Collection<? extends Number> values);

    /**
     * Aggregates the specified values, and returns the result with float
     * precision. Returns zero if the data set is empty.
     */
    default float calc(Number... values) {
        //TODO use List.copyOf once TeaVM supports it.
        return calc(ImmutableList.copyOf(values));
    }

    /**
     * Utility method that calculates a percentage. If {@code total} is zero,
     * this will return 0% and not throw an exception.
     */
    public static float toPercentage(float value, float total) {
        if (total == 0f) {
            return 0f;
        }

        return value * 100f / total;
    }
}
