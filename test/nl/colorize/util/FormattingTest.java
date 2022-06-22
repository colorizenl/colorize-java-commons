//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormattingTest {
    
    @Test
    public void testNumberFormat() {
        assertEquals("1.0", Formatting.numberFormat(1f, 1));
        assertEquals("0.12", Formatting.numberFormat(0.1234f, 2));
        assertEquals("0.14", Formatting.numberFormat(0.145f, 2));
        
        for (int i = 0; i < 1000; i++) {
            float n = (float) (Math.random() * 100d);
            float expected = Float.parseFloat(String.format("%.2f", n));
            float actual = Float.parseFloat(Formatting.numberFormat(n, 2));
            // Note that this will not catch 19.9 versus 19.90, but that
            // is a known difference.
            assertEquals(expected, actual, 0.0001f);
        }
    }
    
    @Test
    public void testTimeFormat() {
        assertEquals("0.000", Formatting.timeFormat(0, true));
        assertEquals("0.010", Formatting.timeFormat(10, true));
        assertEquals("1.234", Formatting.timeFormat(1234, true));
        assertEquals("12.345", Formatting.timeFormat(12345, true));
        assertEquals("59.999", Formatting.timeFormat(59999, true));
        assertEquals("1:00.000", Formatting.timeFormat(60000, true));
        assertEquals("1:00:00.000", Formatting.timeFormat(3600000, true));
        assertEquals("1:00:00", Formatting.timeFormat(3600000, false));
    }
    
    @Test
    public void testTimeDiffFormat() {
        assertEquals("- 10.000", Formatting.timeDiffFormat(10000, 20000, true));
        assertEquals("+ 10.000", Formatting.timeDiffFormat(30000, 20000, true));
        assertEquals("+ 0.000", Formatting.timeDiffFormat(10, 10, true));
    }
    
    @Test
    public void testToTitleCase() {
        assertEquals("S", Formatting.toTitleCase("s"));
        assertEquals("John Doe", Formatting.toTitleCase("john doe"));
        assertEquals("John Doe", Formatting.toTitleCase("JOHN_DOE"));
    }
    
    @Test
    public void testMemoryFormat() {
        assertEquals("2 bytes", Formatting.memoryFormat(2, 0));
        assertEquals("2 bytes", Formatting.memoryFormat(2, 1));
        assertEquals("1 KB", Formatting.memoryFormat(1024, 0));
        assertEquals("1.0 KB", Formatting.memoryFormat(1024, 1));
        assertEquals("1.0 MB", Formatting.memoryFormat(1024 * 1024, 1));
    }

    @Test
    public void testToHexString() {
        assertEquals("", Formatting.toHexString(new byte[0]));
        assertEquals("61", Formatting.toHexString(new byte[] {'a'}));
        assertEquals("61313233", Formatting.toHexString(new byte[] {'a', '1', '2', '3'}));
        assertEquals("6465616462656566",
            Formatting.toHexString(new byte[] {'d', 'e', 'a', 'd', 'b', 'e', 'e', 'f'}));
    }

    private Date asDate(String date) {
        return Formatting.toDate(date);
    }
    
    private String format(String pattern, Date date) {
        return new SimpleDateFormat(pattern).format(date);
    }
}
