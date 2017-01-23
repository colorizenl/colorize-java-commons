//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Helper methods for initializing and configuring {@code java.util.logging}
 * loggers programmatically.
 */
public final class LogHelper {

	private static Logger rootColorizeLogger;
	
	private static final String ROOT_COLORIZE_LOGGER_NAME = "nl.colorize";
	
	// Some utility class use their own loggers which we also want 
	// to use the standard Colorize logging configuration.
	private static final List<String> ADDITIONAL_COLORIZE_LOGGER_NAMES = ImmutableList.of(
			"com.google.common.io.Closeables",
			"com.google.common.eventbus.EventBus");

	private LogHelper() {
	}
	
	private static Logger lookupLogger(String name) {
		return Logger.getLogger(name);
	}
	
	/**
	 * Obtains the logger with the name of the specified class. If only the name 
	 * attribute is specified the logger is returned with its existing (possibly
	 * inherited) configuration. If one or more handlers are specified the logger 
	 * is reconfigured using those handlers.
	 */
	public static Logger getLogger(Class<?> name, Handler... handlers) {
		return getLogger(name.getName(), handlers);
	}
	
	/**
	 * Obtains the logger with the specified name. If only the name attribute is
	 * specified the logger is returned with its existing (possibly inherited) 
	 * configuration. If one or more handlers are specified the logger is 
	 * reconfigured using those handlers.
	 */
	public static Logger getLogger(String name, Handler... handlers) {
		Logger logger = lookupLogger(name);
		
		// Google App Engine uses its own logger configuration and will
		// throw a security exception when applications try to change it.
		if (Platform.isGoogleAppEngine()) {
			return logger;
		}
		
		if (handlers.length >= 1) {
			configureLogger(logger, handlers);
		} else if (isColorizeLogger(logger)) {
			// Make sure the root Colorize logger is configured.
			getRootColorizeLogger();
		}
		
		return logger;
	}
	
	/**
	 * Configures a logger to use the specified handlers. Any handlers already
	 * present, including inherited ones, will be removed.
	 * @throws IllegalArgumentException if no handlers are specified.
	 */
	public static void configureLogger(Logger logger, Handler... handlers) {
		if (handlers.length == 0) {
			throw new IllegalArgumentException("No handlers specified");
		}
		
		logger.setUseParentHandlers(false);
		
		Handler[] existingHandlers = logger.getHandlers();
		for (Handler handler : existingHandlers) {
			logger.removeHandler(handler);
		}
		
		for (Handler handler : handlers) {
			logger.addHandler(handler);
		}
	}
	
	private static boolean isColorizeLogger(Logger logger) {
		return logger.getName() != null && logger.getName().startsWith(ROOT_COLORIZE_LOGGER_NAME);
	}
	
	/**
	 * Returns the root logger for the {@code nl.colorize} namespace, configuring
	 * it if this is the first time it's used.
	 */
	public static synchronized Logger getRootColorizeLogger() {
		if (rootColorizeLogger == null) {
			rootColorizeLogger = lookupLogger(ROOT_COLORIZE_LOGGER_NAME);
			configureLogger(rootColorizeLogger, createConsoleHandler());
			
			for (String loggerName : ADDITIONAL_COLORIZE_LOGGER_NAMES) {
				Logger additionalLogger = lookupLogger(loggerName);
				configureLogger(additionalLogger, createConsoleHandler());
			}
		}
		
		return rootColorizeLogger;
	}
	
	/**
	 * Resets the configuration of all Colorize loggers in the {@code nl.colorize}
	 * namespace.
	 */
	@VisibleForTesting
	public static synchronized void resetColorizeLoggerConfiguration() {
		rootColorizeLogger = null;
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
	 *             {@link #createCompactFormatter()} instead.
	 */
	@Deprecated
	public static ConsoleHandler createPlainConsoleHandler() {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setFormatter(createCompactFormatter());
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
	public static Handler createCallbackHandler(final Callback<Throwable> callback) {
		Handler handler = new AbstractLogHandler() {
			public void publish(LogRecord record) {
				if (isLoggable(record) && (record.getThrown() != null)) {
					callback.call(record.getThrown());
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
	 * Removes all handlers from the specified logger. Note that the logger
	 * might still inherit handlers from its parents.
	 */
	public static void removeHandlers(Logger logger) {
		Handler[] handlers = logger.getHandlers();
		for (Handler handler : handlers) {
			logger.removeHandler(handler);
		}
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
	 * {@link publish(...)} method to be implemented by subclasses.
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
	
	/**
	 * Creates a log message formatter that uses a compact format so that log 
	 * messages only contain a single line. This makes it easier to visually 
	 * scan log files.
	 */
	public static Formatter createCompactFormatter() {
		return new CompactFormatter();
	}
	
	private static class CompactFormatter extends Formatter {
	
		private SimpleDateFormat dateFormat;
		private Date scratchDate;

		public CompactFormatter() {
			dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			scratchDate = new Date();
		}
		
		@Override
		public synchronized String format(LogRecord record) {
			scratchDate.setTime(record.getMillis());
			return format(record.getMessage(), record.getThrown(), record.getLevel(), scratchDate);
		}
		
		public String format(String message, Throwable thrown, Level level, Date timestamp) {
			StringBuilder log = new StringBuilder();
			log.append(dateFormat.format(timestamp));
			log.append("  ");
			log.append(Strings.padEnd(level.toString(), 9, ' '));
			log.append(message);
			log.append(Platform.getLineSeparator());
			if (thrown != null) {
				log.append(LogHelper.getStackTrace(thrown));
			}
			return log.toString();
		}
	}
}
