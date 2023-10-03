//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import static nl.colorize.util.DateParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateParserTest {

    @Test
    void tryParse() {
        assertTrue(DateParser.tryParse("2012-07-01", "yyyy-MM-dd").isPresent());
        assertFalse(DateParser.tryParse("wrong", "dd-MM-yyyy").isPresent());
    }

    @Test
    void throwExceptionIfParsingIsNotPossible() {
        assertEquals("30 Jun 2012 22:00:00 GMT",
            parse("2012-07-01", "yyyy-MM-dd").toGMTString());
        assertThrows(IllegalArgumentException.class,
            () -> parse("wrong", "dd-MM-yyyy"));
    }

    @Test
    void autoDetectDateFormat() {
        assertEquals("30 Jun 2012 22:00:00 GMT", parse("2012-07-01").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", parse("20120701").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", parse("01-07-2012").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", parse("07/01/2012").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", parse("2012-07-01 15:00").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", parse("2012-07-01 15:00:00").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", parse("01-07-2012 15:00:00").toGMTString());
    }

    @Test
    void addDate() {
        Date original = parse("2022-07-01 15:00");

        assertEquals("2022-07-01 17:00",
            DateParser.format(DateParser.add(original, ChronoUnit.HOURS, 2), "yyyy-MM-dd HH:mm"));
        assertEquals("2022-07-01 13:00",
            DateParser.format(DateParser.add(original, ChronoUnit.HOURS, -2), "yyyy-MM-dd HH:mm"));
        assertEquals("2022-07-03 15:00",
            DateParser.format(DateParser.add(original, ChronoUnit.DAYS, 2), "yyyy-MM-dd HH:mm"));
        assertEquals("2022-07-15 15:00",
            DateParser.format(DateParser.add(original, ChronoUnit.WEEKS, 2), "yyyy-MM-dd HH:mm"));
        assertEquals("2022-08-01 15:00",
            DateParser.format(DateParser.add(original, ChronoUnit.MONTHS, 1), "yyyy-MM-dd HH:mm"));
        assertEquals("2023-07-01 15:00",
            DateParser.format(DateParser.add(original, ChronoUnit.YEARS, 1), "yyyy-MM-dd HH:mm"));
    }

    @Test
    void delta() {
        Date a = parse("2022-07-01 15:00");
        Date b = parse("2022-07-12 15:00");
        Date c = parse("2022-08-02 15:00");

        assertEquals(11L * 24L, DateParser.delta(a, b, ChronoUnit.HOURS));
        assertEquals(11L, DateParser.delta(a, b, ChronoUnit.DAYS));
        assertEquals(2L, DateParser.delta(a, b, ChronoUnit.WEEKS));
        assertEquals(0L, DateParser.delta(a, b, ChronoUnit.MONTHS));

        assertEquals(32L * 24L, DateParser.delta(a, c, ChronoUnit.HOURS));
        assertEquals(32L, DateParser.delta(a, c, ChronoUnit.DAYS));
        assertEquals(5L, DateParser.delta(a, c, ChronoUnit.WEEKS));
        assertEquals(1L, DateParser.delta(a, c, ChronoUnit.MONTHS));
    }

    @Test
    void formatRelative() {
        Date reference = parse("2022-07-01 15:00");

        assertEquals("the future", DateParser.formatRelative(parse("2022-09-01 12:00"), reference));
        assertEquals("just now", DateParser.formatRelative(parse("2022-07-01 15:00"), reference));
        assertEquals("15 minutes ago", DateParser.formatRelative(parse("2022-07-01 14:45"), reference));
        assertEquals("last week", DateParser.formatRelative(parse("2022-06-24 15:00"), reference));
        assertEquals("2 weeks ago", DateParser.formatRelative(parse("2022-06-15 15:00"), reference));
        assertEquals("1 month ago", DateParser.formatRelative(parse("2022-05-27 15:00"), reference));
        assertEquals("1 year ago", DateParser.formatRelative(parse("2021-06-15 15:00"), reference));
    }

    @Test
    void parseJavaScriptLocalDateTimeFormat() {
        Date date = parse("2018-06-12T19:30");

        assertEquals("12 Jun 2018 17:30:00 GMT", date.toGMTString());
    }
}
