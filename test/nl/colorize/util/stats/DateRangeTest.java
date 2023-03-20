//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import nl.colorize.util.DateParser;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangeTest {

    @Test
    void contains() {
        DateRange range = new DateRange(DateParser.parse("2018-10-10"),
            DateParser.parse("2019-01-07"));

        assertFalse(range.contains(DateParser.parse("2018-10-09")));
        assertTrue(range.contains(DateParser.parse("2018-10-10")));
        assertTrue(range.contains(DateParser.parse("2018-11-10")));
        assertFalse(range.contains(DateParser.parse("2019-01-07")));
        assertFalse(range.contains(DateParser.parse("2019-01-08")));
    }

    @Test
    void splitDays() {
        DateRange range = new DateRange(DateParser.parse("2018-10-01"),
            DateParser.parse("2018-10-03"));
        List<DateRange> intervals = range.split(ChronoUnit.DAYS);

        assertEquals(3, intervals.size());
        assertEquals("2018-10-01", intervals.get(0).toString());
        assertEquals("2018-10-02", intervals.get(1).toString());
        assertEquals("2018-10-03", intervals.get(2).toString());
    }

    @Test
    void splitWeeks() {
        DateRange range = new DateRange(DateParser.parse("2018-10-10"),
            DateParser.parse("2018-10-21"));
        List<DateRange> intervals = range.split(ChronoUnit.WEEKS);

        assertEquals(2, intervals.size());
        assertEquals("2018-10-08", intervals.get(0).toString());
        assertEquals("2018-10-15", intervals.get(1).toString());
    }

    @Test
    void splitMonths() {
        DateRange range = new DateRange(DateParser.parse("2018-10-01"),
            DateParser.parse("2019-01-15"));
        List<DateRange> intervals = range.split(ChronoUnit.MONTHS);

        assertEquals(4, intervals.size());
        assertEquals("10/2018", intervals.get(0).toString());
        assertEquals("11/2018", intervals.get(1).toString());
        assertEquals("12/2018", intervals.get(2).toString());
        assertEquals("1/2019", intervals.get(3).toString());
    }

    @Test
    void splitYears() {
        DateRange range = new DateRange(DateParser.parse("2018-10-01"),
            DateParser.parse("2018-12-15"));
        List<DateRange> intervals = range.split(ChronoUnit.YEARS);

        assertEquals(1, intervals.size());
        assertEquals("2018", intervals.get(0).toString());
    }

    @Test
    void matchInterval() {
        DateRange range = new DateRange(DateParser.parse("2018-10-01"),
            DateParser.parse("2018-12-15"));

        assertEquals("10/2018",
            range.matchInterval(ChronoUnit.MONTHS, DateParser.parse("2018-10-01")).get().toString());
        assertEquals("11/2018",
            range.matchInterval(ChronoUnit.MONTHS, DateParser.parse("2018-11-01")).get().toString());
        assertFalse(range.matchInterval(ChronoUnit.MONTHS, DateParser.parse("2019-11-01")).isPresent());
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
