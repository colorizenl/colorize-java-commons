//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompactFormatterTest {

    @Test
    public void testLogMessages() {
        LogRecord record = new LogRecord(Level.INFO, "This is a message");
        record.setMillis(1565434294781L);

        CompactFormatter formatter = new CompactFormatter();
        String result = formatter.format(record);

        assertEquals("2019-08-10 12:51:34  INFO     This is a message\n", result);
    }

    @Test
    public void testLogStackTrace() {
        LogRecord record = new LogRecord(Level.SEVERE, "This is an error message");
        record.setMillis(1565434294781L);
        record.setThrown(new IllegalArgumentException());

        CompactFormatter formatter = new CompactFormatter();
        String result = formatter.format(record).substring(0, 160);

        assertEquals("2019-08-10 12:51:34  SEVERE   This is an error message\n" +
            "java.lang.IllegalArgumentException\n" +
            "\tat nl.colorize.util.CompactFormatterTest.testLogStackTrace(CompactFor",
            result);
    }
}
