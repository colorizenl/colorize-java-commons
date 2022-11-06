//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.*;

class CompactFormatterTest {

    @Test
    public void testLogMessages() {
        LogRecord record = new LogRecord(Level.INFO, "This is a message");
        record.setMillis(1565434294781L);

        Formatter formatter = LogHelper.createCompactFormatter();
        String result = formatter.format(record);

        assertEquals("2019-08-10 12:51:34  INFO     This is a message\n", result);
    }

    @Test
    public void testLogStackTrace() {
        LogRecord record = new LogRecord(Level.SEVERE, "This is an error message");
        record.setMillis(1565434294781L);
        record.setThrown(new IllegalArgumentException());

        Formatter formatter = LogHelper.createCompactFormatter();
        String result = formatter.format(record).substring(0, 161);

        String expected = """
            2019-08-10 12:51:34  SEVERE   This is an error message
            java.lang.IllegalArgumentException
            \tat nl.colorize.util.CompactFormatterTest.testLogStackTrace(CompactForm""";

        assertEquals(expected, result);
    }
}
