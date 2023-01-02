//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convenience functions for parsing, formatting, and working with dates. The
 * {@link Date} and {@link SimpleDateFormat} are often inconvenient to use. For
 * example, {@code SimpleDateFormat} is not thread safe, and parsing dates
 * throws a checked exception.
 * <p>
 * If the input string does not contain an explicit time zone, the default
 * time zone will be used. See {@link Platform#getDefaultTimeZone()} for more
 * information.
 * <p>
 * <strong>Note:</strong> The {@code java.time} API introduced in Java 8 pretty
 * much solves most of the usability problems this class attempts to work
 * around. However, this API is not yet available in certain environments that
 * only support a subset of the Java standard library, such as Android, TeaVM,
 * and Google Cloud. This class will only be deprecated once the
 * {@code java.time} is available in all environments.
 */
public final class DateParser {

    private static final Map<Pattern, String> PATTERNS = new ImmutableMap.Builder<Pattern, String>()
        .put(Pattern.compile("\\d{8}"), "yyyyMMdd")
        .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"), "yyyy-MM-dd")
        .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"), "yyyy-MM-dd HH:mm")
        .put(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), "yyyy-MM-dd HH:mm:ss")
        .put(Pattern.compile("\\d{2}-\\d{2}-\\d{4}"), "dd-MM-yyyy")
        .put(Pattern.compile("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}"), "dd-MM-yyyy HH:mm:ss")
        .put(Pattern.compile("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}"), "dd-MM-yyyy HH:mm:ss")
        .put(Pattern.compile("\\d{2}/\\d{2}/\\d{4}"), "MM/dd/yyyy")
        .build();

    private DateParser() {
    }

    /**
     * Wrapper around {@code SimpleDateFormat.parse} to parse a date using the
     * specified date format in a thread-safe way.
     */
    public static Optional<Date> tryParse(String input, String dateFormat) {
        try {
            Date date = create(dateFormat).parse(input);
            return Optional.of(date);
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Wrapper around {@code SimpleDateFormat.parse} to parse a date using the
     * specified date format in a thread-safe way.
     *
     * @throws IllegalArgumentException if the specified input string does not
     *         follow the date format.
     */
    public static Date parse(String input, String dateFormat) {
        return tryParse(input, dateFormat)
            .orElseThrow(() -> new IllegalArgumentException("Cannot parse date: " + input));
    }

    /**
     * Parses a date while attempting to automatically detect the date format
     * for the specified input string.
     *
     * @throws IllegalArgumentException if the input string does not confirm to
     *         any of the date formats supported by this method.
     */
    public static Date parse(String input) {
        for (Map.Entry<Pattern, String> entry : PATTERNS.entrySet()) {
            Matcher matcher = entry.getKey().matcher(input);
            if (matcher.matches()) {
                return parse(input, entry.getValue());
            }
        }

        throw new IllegalArgumentException("Unable to detect date format: " + input);
    }

    /**
     * Convenience method that wraps around {@code SimpleDateFormat.format} and
     * formats a date using the default time zone.
     */
    public static String format(Date date, String dateFormat) {
        return create(dateFormat).format(date);
    }

    /**
     * Convenience method that wraps around {@code SimpleDateFormat.format} and
     * formats a date in the time zone with the specified name.
     */
    public static String format(Date date, String dateFormat, String timeZone) {
        SimpleDateFormat instance = create(dateFormat);
        instance.setTimeZone(TimeZone.getTimeZone(timeZone));
        return instance.format(date);
    }

    private static SimpleDateFormat create(String dateFormat) {
        SimpleDateFormat instance = new SimpleDateFormat(dateFormat);
        instance.setTimeZone(Platform.getDefaultTimeZone());
        return instance;
    }
}
