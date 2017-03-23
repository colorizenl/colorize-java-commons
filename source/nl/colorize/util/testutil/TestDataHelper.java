//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.testutil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import nl.colorize.util.DynamicResourceBundle;
import nl.colorize.util.FormatUtils;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.http.Method;
import nl.colorize.util.http.URLLoader;

/**
 * Helper class for creating test data. This includes mock implementations of
 * external resources (such as files), methods to create random data, and methods
 * for quickly creating data objects (such as {@code Date}s or {@code Properties}).
 * <p>
 * <b>This class should only be used within test code.</b> It is only public so
 * that other Colorize projects can also use it in their tests. 
 */
public final class TestDataHelper {

	private TestDataHelper() {
	}
	
	/**
	 * Logs miscellaneous system information, useful for debugging, to the
	 * specified logger.
	 * @param applicationName Name of the current application. Needed because
	 *        some platforms only allow access to application data if the
	 *        name of the directory is the same as the name of the application.
	 */
	public static void logSystemInformation(Logger logger, String applicationName) {
		logger.info("--- System information ---");
		logger.info("Platform:           " + Platform.getPlatformName());
		logger.info("Java version:       " + Platform.getJavaVersion());
		logger.info("Java vendor:        " + System.getProperty("java.vendor"));
		logger.info("Working directory:  " + Platform.getUserWorkingDirectory());
		logger.info("User home:          " + Platform.getUserHomeDir().getAbsolutePath());
		logger.info("Application data:   " + Platform.getApplicationData(applicationName, "temp.txt")
				.getParentFile().getAbsolutePath());
		logger.info("Temp dir:           " + Platform.getTempDir());
		
		logger.info("--- System properties ---");
		TreeMap<Object,Object> systemProperties = new TreeMap<Object,Object>(System.getProperties());
		for (Map.Entry<Object,Object> entry : systemProperties.entrySet()) {
			logger.info(entry.getKey() + " = " + entry.getValue());
		}
		
		logger.info("--- Environment variables ---");
		TreeMap<String,String> envVariables = new TreeMap<String,String>(System.getenv());
		for (Map.Entry<String,String> entry : envVariables.entrySet()) {
			logger.info(entry.getKey() + " = " + entry.getValue());
		}
	}
	
	public static HttpURLConnection mockURLConnection(String url, final int httpStatus,
			final String mimeType, final byte[] response) {
		return new HttpURLConnection(LoadUtils.toURL(url)) {
			@Override public void connect() throws IOException {
			}

			@Override public void disconnect() {
			}
			
			@Override public int getResponseCode() {
				return httpStatus;
			}
			
			@Override public Map<String, List<String>> getHeaderFields() {
				Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();
				headers.put(HttpHeaders.CONTENT_TYPE, ImmutableList.of(mimeType));
				return headers;
			}
			
			@Override public String getContentType() {
				return mimeType;
			}
			
			@Override public InputStream getInputStream() {
				return new ByteArrayInputStream(response);
			}

			@Override public boolean usingProxy() {
				return false;
			}
		};
	}
	
	public static HttpURLConnection mockURLConnection(String url, int httpStatus, String mimeType, 
			String response) {
		return mockURLConnection(url, httpStatus, mimeType, response.getBytes(Charsets.UTF_8));
	}
	
	public static URLLoader mockURLLoader(String url, final HttpURLConnection connection) {
		// The charset here is only used to send the request, not to
		// read the response. 
		return new URLLoader(url, Method.GET, Charsets.UTF_8) {
			@Override
			public HttpURLConnection openConnection() throws IOException {
				return connection;
			}
		};
	}
	
	/**
	 * A {@code ByteArrayInputStream} that has an artificial delay each time 
	 * data is read from it. 
	 */
	public static ByteArrayInputStream slowInputStream(byte[] data, final int delay) {
		return new ByteArrayInputStream(data) {
			@Override
			public synchronized int read() {
				int result = super.read();
				if (result != -1) {
					sleep(delay);
				}
				return result;
			}
			
			@Override
			public synchronized int read(byte[] b, int off, int len) {
				int result = super.read(b, off, len);
				if (result != -1) {
					sleep((long) delay * (long) result);
				}
				return result;
			}
		};
	}
	
	public static DynamicResourceBundle asResourceBundle(String key, String value, String... rest) {
		return new DynamicResourceBundle(LoadUtils.toProperties(key, value, rest));
	}
	
	/**
	 * Quickly create a {@link java.util.Date} from a string. The string can have
	 * one of the following formats:
	 * <ul>
	 *   <li>yyyy-mm-dd hh:mm:ss
	 *   <li>yyyy-mm-dd hh:mm
	 *   <li>yyyy-mm-dd
	 * </ul>
	 * @throws IllegalArgumentException if the date's notation is not listed above.
	 * @throws RuntimeException if the date cannot be parsed.
	 */
	public static Date asDate(String date) {
		String formatString = null;
		if (date.length() == 19) {
			formatString = FormatUtils.YYYY_MM_DD_SECONDS;
		} else if (date.length() == 16) {
			formatString = FormatUtils.YYYY_MM_DD_TIME;
		} else if (date.length() == 10) {
			formatString = FormatUtils.YYYY_MM_DD;
		} else {
			throw new IllegalArgumentException("Date notation not supported: " + date);
		}
		
		SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
		try {
			return dateFormat.parse(date);
		} catch (ParseException e) {
			throw new RuntimeException("Cannot parse date " + date + " using format " + formatString);
		}
	}
	
	public static Date asDate(int year, int month, int day) {
		return asDate(String.format("%d-%02d-%02d 00:00:00", year, month, day));
	}
	
	/**
	 * Creates a log handler that will write log messages to the specified
	 * {@code StringWriter}.
	 */
	public static Handler createInMemoryLogHandler(StringWriter writer) {
		return LogHelper.createStringHandler(writer, LogHelper.createCompactFormatter());
	}
	
	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new AssertionError(e);
		}	
	}
	
	public static Map<String, String> asMap(String... nameValuePairs) {
		if (nameValuePairs.length == 0) {
			return Collections.emptyMap();
		}
		
		if (nameValuePairs.length % 2 != 0) {
			throw new IllegalArgumentException("Invalid data length: " + nameValuePairs.length);
		}
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (int i = 0; i < nameValuePairs.length; i += 2) {
			map.put(nameValuePairs[i], nameValuePairs[i + 1]);
		}
		return map;
	}
}
