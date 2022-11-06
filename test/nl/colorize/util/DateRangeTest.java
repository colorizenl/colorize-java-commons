//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static nl.colorize.util.Formatting.toDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DateRangeTest {

    @Test
    void contains() {
        DateRange range = new DateRange(toDate("2018-10-10"),
            toDate("2019-01-07"));

        assertFalse(range.contains(toDate("2018-10-09")));
        assertTrue(range.contains(toDate("2018-10-10")));
        assertTrue(range.contains(toDate("2018-11-10")));
        assertFalse(range.contains(toDate("2019-01-07")));
        assertFalse(range.contains(toDate("2019-01-08")));
    }

    @Test
    void splitDays() {
        DateRange range = new DateRange(toDate("2018-10-01"),
            toDate("2018-10-03"));
        List<DateRange> intervals = range.split(DateRange.Interval.DAY);

        assertEquals(3, intervals.size());
        assertEquals("2018-10-01", intervals.get(0).toString());
        assertEquals("2018-10-02", intervals.get(1).toString());
        assertEquals("2018-10-03", intervals.get(2).toString());
    }

    @Test
    void splitWeeks() {
        DateRange range = new DateRange(toDate("2018-10-10"),
            toDate("2018-10-21"));
        List<DateRange> intervals = range.split(DateRange.Interval.WEEK);

        assertEquals(2, intervals.size());
        assertEquals("2018-10-08", intervals.get(0).toString());
        assertEquals("2018-10-15", intervals.get(1).toString());
    }

    @Test
    void splitMonths() {
        DateRange range = new DateRange(toDate("2018-10-01"),
            toDate("2019-01-15"));
        List<DateRange> intervals = range.split(DateRange.Interval.MONTH);

        assertEquals(4, intervals.size());
        assertEquals("10/2018", intervals.get(0).toString());
        assertEquals("11/2018", intervals.get(1).toString());
        assertEquals("12/2018", intervals.get(2).toString());
        assertEquals("1/2019", intervals.get(3).toString());
    }

    @Test
    void splitQuarters() {
        DateRange range = new DateRange(toDate("2018-10-01"),
            toDate("2019-01-15"));
        List<DateRange> intervals = range.split(DateRange.Interval.QUARTER);

        assertEquals(2, intervals.size());
        assertEquals("Q4 2018", intervals.get(0).toString());
        assertEquals("Q1 2019", intervals.get(1).toString());
    }

    @Test
    void splitYears() {
        DateRange range = new DateRange(Formatting.toDate("2018-10-01"),
            Formatting.toDate("2018-12-15"));
        List<DateRange> intervals = range.split(DateRange.Interval.YEAR);

        assertEquals(1, intervals.size());
        assertEquals("2018", intervals.get(0).toString());
    }

    @Test
    void matchInterval() {
        DateRange range = new DateRange(toDate("2018-10-01"),
            toDate("2018-12-15"));
        DateRange.Interval interval = DateRange.Interval.MONTH;

        assertEquals("10/2018", range.matchInterval(interval, toDate("2018-10-01")).get().toString());
        assertEquals("11/2018", range.matchInterval(interval, toDate("2018-11-01")).get().toString());
        assertFalse(range.matchInterval(interval, toDate("2019-11-01")).isPresent());
    }

    @Test
    void span() {
        DateRange a = new DateRange("2018-06-12", "2018-10-10");
        DateRange b = new DateRange("2019-01-01", "2019-04-11");
        DateRange c = new DateRange("2019-01-01", "2019-03-01");

        assertEquals("2018-06-12 - 2019-04-11", a.span(b).toString());
        assertEquals("2019-01-01 - 2019-04-11", b.span(c).toString());
    }
}
