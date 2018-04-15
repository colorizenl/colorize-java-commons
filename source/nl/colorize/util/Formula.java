//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.math.Stats;
import com.google.common.primitives.Doubles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Formula that calculates a value based on a data set consisting of numerical
 * values.
 * <p>
 * Performing a formula on an empty data set will not throw an exception, but
 * will result in a value of 0.0. This avoids the need for a special path in
 * situations where the data set could be empty.
 */
@FunctionalInterface
public interface Formula {

    public static final Formula COUNT = (values, weights) -> values.size();

    public static final Formula MINIMUM = (values, weights) -> {
        return values.stream()
            .mapToDouble(value -> value)
            .min()
            .orElse(0.0);
    };

    public static final Formula MAXIMUM = (values, weights) -> {
        return values.stream()
            .mapToDouble(value -> value)
            .max()
            .orElse(0.0);
    };

    public static final Formula SUM = (values, weights) -> {
        return values.stream()
            .mapToDouble(value -> value)
            .sum();
    };

    public static final Formula AVERAGE = (values, weights) -> {
        if (values.isEmpty()) {
            return 0.0;
        }
        return Stats.meanOf(values);
    };

    public static final Formula MEDIAN = (values, weights) -> {
        if (values.isEmpty()) {
            return 0.0;
        }

        List<Double> sortedValues = new ArrayList<>();
        sortedValues.addAll(values);
        Collections.sort(sortedValues);
        return sortedValues.get(sortedValues.size() / 2);
    };

    public static final Formula WEIGHTED_AVERAGE = (values, weights) -> {
        Preconditions.checkArgument(values.size() == weights.size(),
            "Should provide the same numbers of values and weights");

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
    };

    /**
     * Performs this formula on the specified values and associated weights.
     * Note that some formulas may only use the values and will ignore the
     * weights.
     */
    public double calculate(List<Double> values, List<Double> weights);

    /**
     * Performs this formula on the specified values, where all values are
     * assigned a weight of 1.0.
     */
    default double calculate(List<Double> values) {
        List<Double> weights = values.stream()
            .map(value -> 1.0)
            .collect(Collectors.toList());

        return calculate(values, weights);
    }

    /**
     * Performs this formula on the specified values, where all values are
     * assigned a weight of 1.0.
     */
    default double calculate(double... values) {
        return calculate(Doubles.asList(values));
    }
}
