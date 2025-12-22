//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static nl.colorize.util.DateParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void contains() {
        DateRange range = new DateRange("2018-10-10", "2019-01-07");

        assertFalse(range.contains(parse("2018-10-09")));
        assertTrue(range.contains(parse("2018-10-10")));
        assertTrue(range.contains(parse("2018-11-10")));
        assertFalse(range.contains(parse("2019-01-07")));
        assertFalse(range.contains(parse("2019-01-08")));
    }

    @Test
    void splitDays() {
        List<DateRange> intervals = DateRange.daily(parse("2018-10-01"), parse("2018-10-03"));

        assertEquals(2, intervals.size());
        assertEquals("2018-10-01..2018-10-02", intervals.get(0).toString());
        assertEquals("2018-10-02..2018-10-03", intervals.get(1).toString());
    }

    @Test
    void splitWeeks() {
        DateRange range = new DateRange("2018-10-10", "2018-10-21");
        List<DateRange> intervals = range.splitWeeks();

        assertEquals(2, intervals.size());
        assertEquals("2018-10-10..2018-10-15", intervals.get(0).toString());
        assertEquals("2018-10-15..2018-10-21", intervals.get(1).toString());
    }

    @Test
    void splitMonths() {
        DateRange range = new DateRange("2018-10-05", "2019-01-15");
        List<DateRange> intervals = range.splitMonths();

        assertEquals(4, intervals.size());
        assertEquals("2018-10-05..2018-11-01", intervals.get(0).toString());
        assertEquals("2018-11-01..2018-12-01", intervals.get(1).toString());
        assertEquals("2018-12-01..2019-01-01", intervals.get(2).toString());
        assertEquals("2019-01-01..2019-01-15", intervals.get(3).toString());
    }

    @Test
    void splitYears() {
        DateRange range = new DateRange("2018-10-01", "2018-12-15");
        List<DateRange> intervals = range.splitYears();

        assertEquals(1, intervals.size());
        assertEquals("2018-10-01..2018-12-15", intervals.get(0).toString());
    }

    @Test
    void span() {
        DateRange a = new DateRange("2018-06-12", "2018-10-10");
        DateRange b = new DateRange("2019-01-01", "2019-04-11");
        DateRange c = new DateRange("2019-01-01", "2019-03-01");

        assertEquals("2018-06-12..2019-04-11", a.span(b).toString());
        assertEquals("2019-01-01..2019-04-11", b.span(c).toString());
    }

    @Test
    void sortDateRanges() {
        DateRange a = new DateRange("2019-01-01", "2019-02-01");
        DateRange b = new DateRange("2019-01-01", "2019-03-01");
        DateRange c = new DateRange("2018-01-01", "2019-03-01");

        List<DateRange> sorted = Stream.of(a, b, c)
            .sorted()
            .toList();

        assertEquals(3, sorted.size());
        assertEquals(c, sorted.get(0));
        assertEquals(a, sorted.get(1));
        assertEquals(b, sorted.get(2));
    }

    @Test
    void quarterly() {
        List<DateRange> quarters = DateRange.quarterly(parse("2025-01-01"), parse("2026-01-01"));

        assertEquals(4, quarters.size());
        assertEquals("2025-01-01..2025-04-01", quarters.get(0).toString());
        assertEquals("2025-04-01..2025-07-01", quarters.get(1).toString());
        assertEquals("2025-07-01..2025-10-01", quarters.get(2).toString());
        assertEquals("2025-10-01..2026-01-01", quarters.get(3).toString());
    }

    @Test
    void match() {
        DateRange jan = new DateRange("2025-01-01", "2025-02-01");
        DateRange feb = new DateRange("2025-02-01", "2025-03-01");

        assertEquals(jan, DateRange.match(parse("2025-01-01"), List.of(jan, feb)).orElse(null));
        assertEquals(jan, DateRange.match(parse("2025-01-15"), List.of(jan, feb)).orElse(null));
        assertEquals(feb, DateRange.match(parse("2025-02-01"), List.of(jan, feb)).orElse(null));
        assertNull(DateRange.match(parse("2025-03-01"), List.of(jan, feb)).orElse(null));
    }
}
