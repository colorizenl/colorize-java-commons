//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import nl.colorize.util.Callback;
import nl.colorize.util.CompactFormatter;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.testutil.TestDataHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the {@code LogHelper} class.
 */
public class TestLogHelper {
	
	private PrintStream stdout;
	private ByteArrayOutputStream outBuffer;
	
	private PrintStream stderr;
	private ByteArrayOutputStream errBuffer;
	
	@Before
	public void before() {
		stdout = System.out;
		outBuffer = prepareBuffer(outBuffer);
		System.setOut(new PrintStream(outBuffer));
		
		stderr = System.err;
		errBuffer = prepareBuffer(errBuffer);
		System.setErr(new PrintStream(errBuffer));
	}
	
	@After
	public void after() {
		System.setOut(stdout);
		System.setErr(stderr);
	}
	
	private ByteArrayOutputStream prepareBuffer(ByteArrayOutputStream buffer) {
		if (buffer == null) {
			buffer = new ByteArrayOutputStream();
		}
		buffer.reset();
		return buffer;
	}
	
	@Test
	public void testDefaultColorizeLoggerConfiguration() throws Exception {
		Logger logger = LogHelper.getLogger("nl.colorize.qqq");
		logger.warning("This is a test");
		assertEquals("", outBuffer.toString());
		assertTrue(errBuffer.toString().contains("WARNING  This is a test"));
	}
	
	@Test
	public void testConfigureLogger() throws Exception {
		LogHelper.resetColorizeLoggerConfiguration();
		Logger logger = LogHelper.getLogger("nl.test.c", LogHelper.createPlainConsoleHandler());
		logger.warning("This is a test");
		assertEquals("WARNING  This is a test\n", errBuffer.toString());
	}
	
	@Test
	public void testConfigureColorizeLogger() throws Exception {
		Logger logger = LogHelper.getLogger("nl.kees.c", LogHelper.createPlainConsoleHandler());
		logger.warning("This is a test");
		assertEquals("WARNING  This is a test\n", errBuffer.toString());
	}

	@Test
	public void testFileHandler() throws IOException {
		File tempFile = LoadUtils.getTempFile(".log");
		FileHandler fileHandler = LogHelper.createFileHandler(tempFile, Charsets.UTF_8);
		fileHandler.setFormatter(new CompactFormatter(true, false));
		Logger logger = LogHelper.getLogger("nl.test.x.y.z", fileHandler);
		logger.info("Test log message");
		assertEquals("INFO     Test log message\n", Files.toString(tempFile, Charsets.UTF_8));
	}
	
	@Test
	public void testCallbackHandler() {
		final AtomicInteger count = new AtomicInteger(0);
		final Callback<Throwable> callback = new Callback<Throwable>() {
			public void call(Throwable value) {
				count.incrementAndGet();
			}
		};
		
		Handler handler = LogHelper.createCallbackHandler(callback);
		Logger logger = LogHelper.getLogger(getClass(), handler);
		logger.log(Level.WARNING, "Test", new Exception());
		logger.log(Level.SEVERE, "Test", new RuntimeException());
		assertEquals(1, count.get());
		handler.setLevel(Level.WARNING);
		logger.log(Level.WARNING, "Test", new Exception());
		assertEquals(2, count.get());
	}
	
	@Test
	public void testStringHandler() {
		StringWriter stringWriter = new StringWriter();
		Logger logger = LogHelper.getLogger(getClass().getName(), 
				LogHelper.createStringHandler(stringWriter, new CompactFormatter(true, false)));
		logger.info("Test");
		assertEquals("INFO     Test\n", stringWriter.toString());
	}
	
	@Test
	public void testGetStackTrace() {
		String stackTrace = LogHelper.getStackTrace(new Exception());
		assertTrue(stackTrace.startsWith("java.lang.Exception\n" +
				"\tat nl.colorize.util.TestLogHelper.testGetStackTrace"));
	}
	
	@Test
	public void testCompactFormatterLevelNames() {
		CompactFormatter formatter = new CompactFormatter(true, true);
		Date date = TestDataHelper.asDate("2013-01-14 14:53:00");
		
		assertEquals("2013-01-14 14:53:00  INFO     First\n", 
				formatter.format("First", null, Level.INFO, date));
		assertEquals("2013-01-14 14:53:00  WARNING  Second\n", 
				formatter.format("Second", null, Level.WARNING, date));
		assertEquals("2013-01-14 14:53:00  SEVERE   Third\n", 
				formatter.format("Third", null, Level.SEVERE, date));
	}
}
