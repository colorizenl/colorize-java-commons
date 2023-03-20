//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import nl.colorize.util.stats.Tuple;
import nl.colorize.util.stats.TupleList;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.GregorianCalendar;
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
 * If a date string does not contain an explicit time zone, the default time
 * zone will be used. See {@link Platform#getDefaultTimeZone()} for how to
 * configure the default time zone.
 * <p>
 * <strong>Note:</strong> The {@code java.time} API introduced in Java 8 pretty
 * much solves most of the usability problems with the old date and calendar
 * APIs. However, this class is not deprecated because {@code java.time} is
 * only partially available on some platforms and Java implementations.
 */
public final class DateParser {

    private static final TupleList<Pattern, String> PATTERNS = new TupleList<Pattern, String>()
        .append(Pattern.compile("\\d{8}"), "yyyyMMdd")
        .append(Pattern.compile("\\d{4}-\\d{2}-\\d{2}"), "yyyy-MM-dd")
        .append(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}"), "yyyy-MM-dd HH:mm")
        .append(Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), "yyyy-MM-dd HH:mm:ss")
        .append(Pattern.compile("\\d{2}-\\d{2}-\\d{4}"), "dd-MM-yyyy")
        .append(Pattern.compile("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}"), "dd-MM-yyyy HH:mm:ss")
        .append(Pattern.compile("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}"), "dd-MM-yyyy HH:mm:ss")
        .append(Pattern.compile("\\d{2}/\\d{2}/\\d{4}"), "MM/dd/yyyy")
        .immutable();

    private static final Map<ChronoUnit, Integer> CALENDAR_FIELD_MAPPING = Map.of(
        ChronoUnit.SECONDS, GregorianCalendar.SECOND,
        ChronoUnit.MINUTES, GregorianCalendar.MINUTE,
        ChronoUnit.HOURS, GregorianCalendar.HOUR,
        ChronoUnit.DAYS, GregorianCalendar.DAY_OF_MONTH,
        ChronoUnit.WEEKS, GregorianCalendar.WEEK_OF_YEAR,
        ChronoUnit.MONTHS, GregorianCalendar.MONTH,
        ChronoUnit.YEARS, GregorianCalendar.YEAR
    );

    private static final TupleList<ChronoUnit, String> LABELS = new TupleList<ChronoUnit, String>()
        .append(ChronoUnit.YEARS, "YEAR")
        .append(ChronoUnit.MONTHS, "MONTH")
        .append(ChronoUnit.WEEKS, "WEEK")
        .append(ChronoUnit.DAYS, "DAY")
        .append(ChronoUnit.HOURS, "HOUR")
        .append(ChronoUnit.MINUTES, "MINUTE")
        .immutable();

    private static final TranslationBundle BUNDLE = TranslationBundle.fromPropertiesFile(
        new ResourceFile("custom-swing-components.properties"));

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
        for (Tuple<Pattern, String> entry : PATTERNS) {
            Matcher matcher = entry.left().matcher(input);
            if (matcher.matches()) {
                return parse(input, entry.right());
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

    /**
     * Returns a new date that is created by adding the specified time unit
     * to the original date. The amount can be negative, which will return a
     * date that is before the original.
     * <p>
     * This method supports the following time units:
     * <ul>
     *   <li>{@link ChronoUnit#SECONDS}</li>
     *   <li>{@link ChronoUnit#MINUTES}</li>
     *   <li>{@link ChronoUnit#HOURS}</li>
     *   <li>{@link ChronoUnit#DAYS}</li>
     *   <li>{@link ChronoUnit#WEEKS}</li>
     *   <li>{@link ChronoUnit#MONTHS}</li>
     *   <li>{@link ChronoUnit#YEARS}</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the provided time unit is not in
     *         the list of supported time units.
     */
    public static Date add(Date original, ChronoUnit unit, int amount) {
        Preconditions.checkArgument(CALENDAR_FIELD_MAPPING.containsKey(unit),
            "Time unit not supported: " + unit);

        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(original);
        calendar.add(CALENDAR_FIELD_MAPPING.get(unit), amount);
        return calendar.getTime();
    }

    /**
     * Returns the difference between two dates, expressed in the specified
     * time unit. The difference is absolute, so it doesn't matter whether the
     * first or second argument is more recent. The value is rounded, so a
     * delta of 11 days with a unit of weeks will return 2.
     */
    public static long delta(Date a, Date b, ChronoUnit unit) {
        long milliseconds = Math.abs(a.getTime() - b.getTime());
        return Math.round((milliseconds / 1000.0) / unit.getDuration().getSeconds());
    }

    /**
     * Formats a date relative to another date, with the precision being
     * decided by the distance between the two dates. Examples of returned
     * values are "2 hours ago", "yesterday", and "3 weeks ago".
     */
    public static String formatRelative(Date date, Date reference) {
        if (date.getTime() > reference.getTime()) {
            return BUNDLE.getString("FUTURE");
        }

        long deltaInSeconds = Math.abs(date.getTime() - reference.getTime()) / 1000L;

        for (Tuple<ChronoUnit, String> entry : LABELS) {
            long secondsInUnit = entry.left().getDuration().getSeconds();
            long deltaInUnit = deltaInSeconds / secondsInUnit;

            if (deltaInSeconds >= 2L * secondsInUnit) {
                return BUNDLE.getString(entry.right() + "S", deltaInUnit);
            } else if (deltaInSeconds >= secondsInUnit) {
                return BUNDLE.getString(entry.right(), deltaInUnit);
            }
        }

        return BUNDLE.getString("NOW");
    }

    /**
     * Formats a date relative to the current date, with the precision being
     * decided by the distance between the two dates. Examples of returned
     * values are "2 hours ago", "yesterday", and "3 weeks ago".
     */
    public static String formatRelative(Date date) {
        return formatRelative(date, new Date());
    }
}
