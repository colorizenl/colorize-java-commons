//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import nl.colorize.util.FormatUtils;
import nl.colorize.util.testutil.TestDataHelper;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code FormatUtils} class.
 */
public class TestFormatUtils {
	
	@Test
	@SuppressWarnings("deprecation")
	public void testNumberFormat() {
		assertEquals("1.0", FormatUtils.numberFormat(1f, 1));
		assertEquals("0.12", FormatUtils.numberFormat(0.1234f, 2));
		assertEquals("0.14", FormatUtils.numberFormat(0.145f, 2));
		
		for (int i = 0; i < 1000; i++) {
			float n = (float) (Math.random() * 100d);
			float expected = Float.parseFloat(String.format("%.2f", n));
			float actual = Float.parseFloat(FormatUtils.numberFormat(n, 2));
			// Note that this will not catch 19.9 versus 19.90, but that
			// is a known difference.
			assertEquals(expected, actual, 0.0001f);
		}
	}
	
	@Test
	public void testTimeFormat() {
		assertEquals("0.000", FormatUtils.timeFormat(0, true));
		assertEquals("0.010", FormatUtils.timeFormat(10, true));
		assertEquals("1.234", FormatUtils.timeFormat(1234, true));
		assertEquals("12.345", FormatUtils.timeFormat(12345, true));
		assertEquals("59.999", FormatUtils.timeFormat(59999, true));
		assertEquals("1:00.000", FormatUtils.timeFormat(60000, true));
		assertEquals("1:00:00.000", FormatUtils.timeFormat(3600000, true));		
		assertEquals("1:00:00", FormatUtils.timeFormat(3600000, false));
	}
	
	@Test
	public void testTimeDiffFormat() {
		assertEquals("- 10.000", FormatUtils.timeDiffFormat(10000, 20000, true));
		assertEquals("+ 10.000", FormatUtils.timeDiffFormat(30000, 20000, true));
		assertEquals("+ 0.000", FormatUtils.timeDiffFormat(10, 10, true));
	}
	
	@Test
	public void testToTitleCase() {
		assertEquals("S", FormatUtils.toTitleCase("s"));
		assertEquals("John Doe", FormatUtils.toTitleCase("john doe"));
		assertEquals("John Doe", FormatUtils.toTitleCase("JOHN_DOE"));
	}
	
	@Test
	public void testMemoryFormat() {
		assertEquals("2 bytes", FormatUtils.memoryFormat(2, 0));
		assertEquals("2 bytes", FormatUtils.memoryFormat(2, 1));
		assertEquals("1 KB", FormatUtils.memoryFormat(1024, 0));
		assertEquals("1.0 KB", FormatUtils.memoryFormat(1024, 1));
		assertEquals("1.0 MB", FormatUtils.memoryFormat(1024 * 1024, 1));	
	}
	
	@Test
	public void testDateFormats() {
		Date date = TestDataHelper.asDate("2011-04-05 12:01:30");
		assertEquals("20110405", format(FormatUtils.YYYYMMDD, date));
		assertEquals("2011-04-05", format(FormatUtils.YYYY_MM_DD, date));
		assertEquals("2011-04-05 12:01", format(FormatUtils.YYYY_MM_DD_TIME, date));
		assertEquals("2011-04-05 12:01:30", format(FormatUtils.YYYY_MM_DD_SECONDS, date));
		assertEquals("05-04-2011", format(FormatUtils.DD_MM_YYYY, date));
		assertEquals("05-04-2011 12:01", format(FormatUtils.DD_MM_YYYY_TIME, date));
		assertEquals("05-04-2011 12:01:30", format(FormatUtils.DD_MM_YYYY_SECONDS, date));
		assertEquals("2011-04-05T12:01:30", format(FormatUtils.ISO_8601, date));
	}

	@Test
	public void testFormatDiff() {
		Date base = TestDataHelper.asDate("2010-01-01 12:00:00");
		assertEquals("seconds ago", FormatUtils.formatDateDiff(asDate("2010-01-01 12:00:00"), base));
		assertEquals("5 minutes ago", FormatUtils.formatDateDiff(asDate("2010-01-01 12:05:00"), base));
		assertEquals("8 hours ago", FormatUtils.formatDateDiff(asDate("2010-01-01 20:00:00"), base));
		assertEquals("2 days ago", FormatUtils.formatDateDiff(asDate("2010-01-03 12:00:00"), base));
		assertEquals("2 weeks ago", FormatUtils.formatDateDiff(asDate("2010-01-15 12:00:00"), base));
		assertEquals("6 months ago", FormatUtils.formatDateDiff(asDate("2010-07-15 00:00:00"), base));
		assertEquals("100 years ago", FormatUtils.formatDateDiff(asDate("2110-01-01 12:00:00"), base));
	}
	
	@Test
	@SuppressWarnings("deprecation")
	public void testHumanReadableFormat() {
		assertEquals("something", FormatUtils.humanReadableFormat("something"));
		assertEquals("Something", FormatUtils.humanReadableFormat("Something"));
		assertEquals("Something else", FormatUtils.humanReadableFormat("Something Else"));
		assertEquals("Something else", FormatUtils.humanReadableFormat("SOMETHING ELSE"));
		assertEquals("Something else", FormatUtils.humanReadableFormat("SOMETHING_ELSE"));
	}
	
	@Test
	public void testFormatHumanReadableFileLocation() {
		assertEquals("a \u00BB b.txt", FormatUtils.formatHumanReadableFileLocation(new File("a/b.txt")));
		assertEquals("a \u00BB b.txt", FormatUtils.formatHumanReadableFileLocation(new File("/a/b.txt")));
		assertEquals("b \u00BB c.txt", FormatUtils.formatHumanReadableFileLocation(new File("/a/b/c.txt")));
		assertEquals("a.txt", FormatUtils.formatHumanReadableFileLocation(new File("a.txt")));
	}
	
	private Date asDate(String date) {
		return TestDataHelper.asDate(date);
	}
	
	private String format(String pattern, Date date) {
		return new SimpleDateFormat(pattern).format(date);
	}
}
