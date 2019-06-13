//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Charsets;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code LogHelper} class.
 */
public class LogHelperTest {
    
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
        Logger logger = LogHelper.getLogger("nl.test.c", LogHelper.createConsoleHandler());
        logger.warning("This is a test");
        assertEquals("WARNING  This is a test\n", errBuffer.toString().replaceFirst("[\\d-: ]+", ""));
    }

    @Test
    public void testFileHandler() throws IOException {
        File tempFile = LoadUtils.getTempFile(".log");
        FileHandler fileHandler = LogHelper.createFileHandler(tempFile, Charsets.UTF_8);
        fileHandler.setFormatter(LogHelper.createCompactFormatter());
        Logger logger = LogHelper.getLogger("nl.test.x.y.z", fileHandler);
        logger.info("Test log message");
        assertEquals("INFO     Test log message\n", FileUtils.read(tempFile, Charsets.UTF_8)
                .replaceFirst("[\\d-: ]+", ""));
    }
    
    @Test
    public void testCallbackHandler() {
        final AtomicInteger count = new AtomicInteger(0);
        final Consumer<Throwable> callback = value -> count.incrementAndGet();

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
                LogHelper.createStringHandler(stringWriter, LogHelper.createCompactFormatter()));
        logger.info("Test");
        assertEquals("INFO     Test\n", stringWriter.toString().replaceFirst("[\\d-: ]+", ""));
    }
    
    @Test
    public void testGetStackTrace() {
        String stackTrace = LogHelper.getStackTrace(new Exception());
        assertTrue(stackTrace.startsWith("java.lang.Exception\n" +
                "\tat nl.colorize.util.LogHelperTest.testGetStackTrace"));
    }
}
