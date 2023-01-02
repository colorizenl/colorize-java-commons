//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import nl.colorize.util.stats.Histogram;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramTest {

    @Test
    void countValues() {
        Histogram<String> hist = new Histogram<>();
        hist.count("a", "x");
        hist.count("a", "x", 3);
        hist.count("a", "y", 2);
        hist.count("b", "y", 7);

        assertEquals(4, hist.getValue("a", "x"));
        assertEquals(0, hist.getValue("b", "x"));
        assertEquals(2, hist.getValue("a", "y"));
        assertEquals(7, hist.getValue("b", "y"));

        assertEquals(6, hist.getTotalSeriesValue("a"));
        assertEquals(7, hist.getTotalSeriesValue("b"));

        assertEquals(4, hist.getTotalBinValue("x"));
        assertEquals(9, hist.getTotalBinValue("y"));
        assertEquals(0, hist.getTotalBinValue("z"));

        assertEquals("[(x, 4), (y, 2)]", hist.getSeriesValues("a").toString());
        assertEquals("[(x, 0), (y, 7)]", hist.getSeriesValues("b").toString());

        assertEquals(ImmutableMap.of("a", 4, "b", 0), hist.getBinValues("x"));
        assertEquals(ImmutableMap.of("a", 2, "b", 7), hist.getBinValues("y"));
    }

    @Test
    void autoSortSeries() {
        Histogram<String> hist = new Histogram<>();
        hist.addBin("b");
        hist.addBin("a");
        hist.addSeries("b");
        hist.addSeries("a");

        assertEquals(ImmutableList.of("b", "a"), hist.getBins());
        assertEquals(ImmutableList.of("a", "b"), hist.getSeriesByName());
    }

    @Test
    void sortSeriesByTotalValue() {
        Histogram<String> hist = new Histogram<>();
        hist.count("a", "x", 10);
        hist.count("a", "y", 7);
        hist.count("b", "x", 6);
        hist.count("b", "y", 12);

        assertEquals(ImmutableList.of("b", "a"), hist.getSeriesByTotalValue());
    }
}
