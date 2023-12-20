//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramTest {

    private static final DateRange JANUARY = new DateRange("2023-01-01", "2023-02-01");
    private static final DateRange FEBRUARY = new DateRange("2023-02-01", "2023-03-01");
    private static final DateRange MARCH = new DateRange("2023-03-01", "2023-04-01");
    private static final DateRange APRIL = new DateRange("2023-04-01", "2023-05-01");

    @Test
    void addBinsOnTheFly() {
        Histogram<String> histogram = new Histogram<>();
        histogram.count("a", "1");
        histogram.count("a", "1");
        histogram.count("b", "1");

        assertEquals(List.of("a", "b"), histogram.getBins());
    }

    @Test
    void addBinsUpFront() {
        Histogram<String> histogram = new Histogram<>(List.of("a", "b"));

        assertEquals(List.of("a", "b"), histogram.getBins());
    }

    @Test
    void autoSortBins() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(MARCH, "1");
        histogram.count(JANUARY, "1");

        assertEquals(List.of(JANUARY, MARCH), histogram.getBins());
    }

    @Test
    void sortSeriesByFrequency() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(MARCH, "1");
        histogram.count(APRIL, "2");
        histogram.count(APRIL, "2");

        assertEquals(List.of("2", "1"), histogram.getSeries());
    }

    @Test
    void calculateTotals() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(JANUARY, "1");
        histogram.count(JANUARY, "2");
        histogram.count(JANUARY, "2", 10);
        histogram.count(MARCH, "1");
        histogram.count(APRIL, "2");

        assertEquals(1, histogram.getFrequency(JANUARY, "1"));
        assertEquals(11, histogram.getFrequency(JANUARY, "2"));

        assertEquals(12, histogram.getBinTotal(JANUARY));
        assertEquals(0, histogram.getBinTotal(FEBRUARY));
        assertEquals(1, histogram.getBinTotal(MARCH));
        assertEquals(1, histogram.getBinTotal(APRIL));

        assertEquals(2, histogram.getSeriesTotal("1"));
        assertEquals(12, histogram.getSeriesTotal("2"));
        assertEquals(0, histogram.getSeriesTotal("3"));

        assertEquals(14, histogram.getTotal());
    }

    @Test
    void getSeriesTotals() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(JANUARY, "1");
        histogram.count(JANUARY, "2");
        histogram.count(FEBRUARY, "2");

        Map<String, Integer> totals = histogram.getSeriesTotals();

        assertEquals(2, totals.size());
        assertEquals("[2, 1]", totals.keySet().toString());
        assertEquals(2, totals.get("2"));
        assertEquals(1, totals.get("1"));
    }

    @Test
    void getSeriesPercentages() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(JANUARY, "1");
        histogram.count(JANUARY, "2", 2);
        histogram.count(FEBRUARY, "2");

        Map<String, Float> percentages = histogram.getSeriesPercentages();

        assertEquals("{2=75.0, 1=25.0}", percentages.toString());
    }

    @Test
    void getSeriesTuples() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(JANUARY, "1");
        histogram.count(JANUARY, "2");
        histogram.count(FEBRUARY, "2");
        histogram.count(FEBRUARY, "2");

        assertEquals("[(2023-01-01 - 2023-02-01, 1), (2023-02-01 - 2023-03-01, 0)]",
            histogram.getSeriesFrequency("1").toString());

        assertEquals("[(2023-01-01 - 2023-02-01, 1), (2023-02-01 - 2023-03-01, 2)]",
            histogram.getSeriesFrequency("2").toString());
    }

    @Test
    void getBinMap() {
        Histogram<DateRange> histogram = new Histogram<>();
        histogram.count(JANUARY, "1");
        histogram.count(JANUARY, "2");
        histogram.count(JANUARY, "2", 3);
        histogram.count(FEBRUARY, "2");

        assertEquals("{2=4, 1=1}", histogram.getBinFrequency(JANUARY).toString());
        assertEquals("{2=1}", histogram.getBinFrequency(FEBRUARY).toString());
    }
}
