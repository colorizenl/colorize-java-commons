//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

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
            DateParser.parse("2012-07-01", "yyyy-MM-dd").toGMTString());
        assertThrows(IllegalArgumentException.class,
            () -> DateParser.parse("wrong", "dd-MM-yyyy"));
    }

    @Test
    void autoDetectDateFormat() {
        assertEquals("30 Jun 2012 22:00:00 GMT", DateParser.parse("2012-07-01").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", DateParser.parse("20120701").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", DateParser.parse("01-07-2012").toGMTString());
        assertEquals("30 Jun 2012 22:00:00 GMT", DateParser.parse("07/01/2012").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", DateParser.parse("2012-07-01 15:00").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", DateParser.parse("2012-07-01 15:00:00").toGMTString());
        assertEquals("1 Jul 2012 13:00:00 GMT", DateParser.parse("01-07-2012 15:00:00").toGMTString());
    }
}
