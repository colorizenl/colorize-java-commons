//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LogHelperTest {
    
    private static PrintStream stdout;
    private static ByteArrayOutputStream outBuffer;
    
    private static PrintStream stderr;
    private static ByteArrayOutputStream errBuffer;
    
    @BeforeAll
    public static void before() {
        stdout = System.out;
        outBuffer = prepareBuffer(outBuffer);
        System.setOut(new PrintStream(outBuffer));
        
        stderr = System.err;
        errBuffer = prepareBuffer(errBuffer);
        System.setErr(new PrintStream(errBuffer));
    }
    
    @AfterAll
    public static void after() {
        System.setOut(stdout);
        System.setErr(stderr);
    }
    
    private static ByteArrayOutputStream prepareBuffer(ByteArrayOutputStream buffer) {
        if (buffer == null) {
            buffer = new ByteArrayOutputStream();
        }
        buffer.reset();
        return buffer;
    }
    
    @Test
    public void testDefaultColorizeLoggerConfiguration() {
        Logger logger = LogHelper.getLogger("nl.colorize.qqqq");
        logger.warning("This is a test");

        assertEquals("", outBuffer.toString());
        assertTrue(errBuffer.toString().contains("WARNING  This is a test"));
    }
    
    @Test
    public void testConfigureLogger() {
        Logger logger = LogHelper.getLogger("nl.test.c");
        logger.addHandler(LogHelper.createConsoleHandler());
        logger.warning("This is a test");

        assertTrue(errBuffer.toString().replaceFirst("[\\d-: ]+", "")
            .contains("WARNING  This is a test\n"));
    }

    @Test
    public void testFileHandler(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "test.log");
        FileHandler fileHandler = LogHelper.createFileHandler(tempFile, UTF_8);
        fileHandler.setFormatter(LogHelper.createCompactFormatter());

        Logger logger = LogHelper.getLogger("nl.test.x.y.z");
        logger.addHandler(fileHandler);
        logger.info("Test log message");

        assertEquals("INFO     Test log message\n",
            Files.readString(tempFile.toPath(), UTF_8)
            .replaceFirst("[\\d-: ]+", ""));
    }
    
    @Test
    public void testCallbackHandler() {
        final AtomicInteger count = new AtomicInteger(0);
        final Consumer<Throwable> callback = value -> count.incrementAndGet();

        Handler handler = LogHelper.createCallbackHandler(callback);
        Logger logger = LogHelper.getLogger(getClass());
        logger.addHandler(handler);
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
        Logger logger = LogHelper.getLogger(getClass().getName());
        logger.addHandler(LogHelper.createStringHandler(stringWriter,
            LogHelper.createCompactFormatter()));
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
