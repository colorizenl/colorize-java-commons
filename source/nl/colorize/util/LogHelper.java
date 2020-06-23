//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Helper methods for initializing and configuring {@code java.util.logging}
 * loggers programmatically.
 */
public final class LogHelper {

    private static AtomicBoolean rootColorizeLoggerConfigured = new AtomicBoolean(false);
    
    private static final String ROOT_COLORIZE_LOGGER_NAME = "nl.colorize";

    private static final String COLORIZE_LOGGING_CONFIGURATION =
        "handlers=java.util.logging.ConsoleHandler\n" +
        "java.util.logging.ConsoleHandler.formatter=" + CompactFormatter.class.getName();

    private LogHelper() {
    }
    
    private static Logger lookupLogger(String name) {
        return Logger.getLogger(name);
    }
    
    /**
     * Obtains the logger with the name of the specified class. The logger is
     * returned with its existing, possibly inherited, configuration.
     */
    public static Logger getLogger(Class<?> name) {
        return getLogger(name.getName());
    }
    
    /**
     * Obtains the logger with the specified name. The logger is returned with
     * its existing, possibly inherited, configuration.
     */
    public static Logger getLogger(String name) {
        Logger logger = lookupLogger(name);
        
        // TeaVM redirects logging to the browser console and
        // will crash trying to redirect it.
        if (Platform.isTeaVM()) {
            return logger;
        }

        // Make sure the root Colorize logger is configured.
        if (isColorizeLogger(logger) && !rootColorizeLoggerConfigured.get()) {
            rootColorizeLoggerConfigured.set(true);
            configureRootColorizeLogger();
        }
        
        return logger;
    }

    private static boolean isColorizeLogger(Logger logger) {
        return logger.getName() != null && logger.getName().startsWith(ROOT_COLORIZE_LOGGER_NAME);
    }
    
    private static synchronized void configureRootColorizeLogger() {
        LogManager logManager = LogManager.getLogManager();
        byte[] config = COLORIZE_LOGGING_CONFIGURATION.getBytes(StandardCharsets.UTF_8);

        try (InputStream stream = new ByteArrayInputStream(config)) {
            logManager.readConfiguration(stream);
        } catch (IOException | UnsupportedCharsetException e) {
            // Use the default logging configuration.
        }
    }

    /**
     * Creates a log handler that will log to {@code stderr}. The handler will
     * use the platform's default character encoding.
     */
    public static ConsoleHandler createConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CompactFormatter());
        return consoleHandler;
    }
    
    /**
     * Creates a log handler that will print messages to {@code stderr} and will
     * not apply any formatting to messages.
     * @deprecated Use {@link #createConsoleHandler()} in combination with 
     *             a {@code CompactFormatter} instead.
     */
    @Deprecated
    public static ConsoleHandler createPlainConsoleHandler() {
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CompactFormatter());
        consoleHandler.setLevel(Level.INFO);
        return consoleHandler;
    }
    
    /**
     * Creates a log handler that will log to the specified file using the
     * platform's default character encoding.
     * @throws IllegalArgumentException if the file is not writeable.
     */
    public static FileHandler createFileHandler(File logfile) {
        return createFileHandler(logfile, null);
    }
    
    /**
     * Creates a log handler that will log to the specified file.
     * @throws IllegalArgumentException if the file is not writeable.
     */
    public static FileHandler createFileHandler(File logfile, Charset encoding) {
        try {
            FileHandler fileHandler = new FileHandler(logfile.getAbsolutePath(), true);
            fileHandler.setFormatter(new CompactFormatter());
            if (encoding != null) {
                fileHandler.setEncoding(encoding.displayName());
            }
            return fileHandler;
        } catch (IOException e) {
            throw new IllegalArgumentException("Log file is not writable", e);
        }
    }
    
    /**
     * Creates a log handler that will invoke the specified callback function
     * for every log message that contains an attached {@code Throwable}. The
     * log level is initially set to {@code SEVERE}.
     */
    public static Handler createCallbackHandler(Consumer<Throwable> callback) {
        Handler handler = new AbstractLogHandler() {
            @Override
            public void publish(LogRecord record) {
                if (isLoggable(record) && (record.getThrown() != null)) {
                    callback.accept(record.getThrown());
                }
            }
        };
        handler.setFormatter(new CompactFormatter());
        handler.setLevel(Level.SEVERE);
        return handler;
    }
    
    /**
     * Creates a log handler that will write log messages to the specified
     * {@code StringWriter}.
     */
    public static Handler createStringHandler(final StringWriter stringWriter, Formatter formatter) {
        Handler handler = new AbstractLogHandler() {
            public void publish(LogRecord record) {
                stringWriter.write(getFormatter().format(record));
            }
        };
        handler.setFormatter(formatter);
        return handler;
    }
    
    /**
     * Creates a log handler that will write log messages to the specified
     * {@code StringWriter} and uses a compact formatter.
     */
    public static Handler createStringHandler(StringWriter stringWriter) {
        return createStringHandler(stringWriter, new CompactFormatter());
    }

    /**
     * Returns the strack trace for the specified exception as a string.
     */
    public static String getStackTrace(Throwable e) {
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        e.printStackTrace(writer);
        writer.close();
        return buffer.toString();
    }

    /**
     * Skeletal implementation of a log handler. It leaves only the
     * {@code publish(...)} method to be implemented by subclasses.
     */
    private static abstract class AbstractLogHandler extends Handler {
        
        @Override
        public abstract void publish(LogRecord record);
        
        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
