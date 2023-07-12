//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import java.util.Collection;
import java.util.List;

/**
 * Utility class for accessing various aggregation functions that operate on
 * a numerical data set. Unless stated otherwise, all aggregation functions
 * will return zero when providing an empty data set.
 */
public final class Aggregates {

    private Aggregates() {
    }

    public static float sum(Collection<? extends Number> values) {
        float sum = 0f;
        for (Number value : values) {
            sum += value.floatValue();
        }
        return sum;
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

        List<Float> sortedValues = values.stream()
            .map(Number::floatValue)
            .sorted()
            .toList();

        return sortedValues.get(sortedValues.size() / 2);
    }

    public static float percentage(float value, float total) {
        if (total == 0f) {
            return 0f;
        }

        return value * 100f / total;
    }
}
