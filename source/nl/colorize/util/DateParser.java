//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
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

    private static final List<DatePattern> DATE_PATTERNS = List.of(
        new DatePattern("\\d{8}",                                    "yyyyMMdd"),
        new DatePattern("\\d{4}-\\d{2}-\\d{2}",                      "yyyy-MM-dd"),
        new DatePattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}",        "yyyy-MM-dd HH:mm"),
        new DatePattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}",        "yyyy-MM-dd'T'HH:mm"),
        new DatePattern("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd HH:mm:ss"),
        new DatePattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}", "yyyy-MM-dd'T'HH:mm:ss"),
        new DatePattern("\\d{1,2}-\\d{1,2}-\\d{4}",                      "dd-MM-yyyy"),
        new DatePattern("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}",        "dd-MM-yyyy HH:mm:ss"),
        new DatePattern("\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}", "dd-MM-yyyy HH:mm:ss"),
        new DatePattern("\\d{2}/\\d{2}/\\d{4}",                      "MM/dd/yyyy")
    );

    // Needs to be an ImmutableMap to preserve order.
    private static final Map<ChronoUnit, Integer> CALENDAR_FIELD_MAPPING = ImmutableMap.of(
        ChronoUnit.YEARS, GregorianCalendar.YEAR,
        ChronoUnit.MONTHS, GregorianCalendar.MONTH,
        ChronoUnit.WEEKS, GregorianCalendar.WEEK_OF_YEAR,
        ChronoUnit.DAYS, GregorianCalendar.DAY_OF_MONTH,
        ChronoUnit.HOURS, GregorianCalendar.HOUR,
        ChronoUnit.MINUTES, GregorianCalendar.MINUTE,
        ChronoUnit.SECONDS, GregorianCalendar.SECOND
    );

    private static TranslationBundle BUNDLE = TranslationBundle.fromPropertiesFile("""
        future=the future
        now=just now
        second=just now
        seconds=seconds ago
        minute=1 minute ago
        minutes={0} minutes ago
        hour=1 hour ago
        hours={0} hours ago
        day=yesterday
        days={0} days ago
        week=last week
        weeks={0} weeks ago
        month=1 month ago
        months={0} months ago
        year=1 year ago
        years={0} years ago
    """);

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
        return DATE_PATTERNS.stream()
            .filter(datePattern -> datePattern.pattern.matcher(input).matches())
            .map(datePattern -> parse(input, datePattern.dateFormat))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unable to detect date format: " + input));
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
     *
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
            return BUNDLE.getString("future");
        }

        long deltaInSeconds = Math.abs(date.getTime() - reference.getTime()) / 1000L;

        for (ChronoUnit chronoUnit : CALENDAR_FIELD_MAPPING.keySet()) {
            long secondsInUnit = chronoUnit.getDuration().getSeconds();
            long deltaInUnit = deltaInSeconds / secondsInUnit;

            if (deltaInSeconds >= 2L * secondsInUnit) {
                String plural = chronoUnit.toString().toLowerCase();
                return BUNDLE.getString(plural, deltaInUnit);
            } else if (deltaInSeconds >= secondsInUnit) {
                String singular = TextUtils.removeTrailing(chronoUnit.toString().toLowerCase(), "s");
                return BUNDLE.getString(singular, deltaInUnit);
            }
        }

        return BUNDLE.getString("now");
    }

    /**
     * Formats a date relative to the current date, with the precision being
     * decided by the distance between the two dates. Examples of returned
     * values are "2 hours ago", "yesterday", and "3 weeks ago".
     */
    public static String formatRelative(Date date) {
        return formatRelative(date, new Date());
    }

    /**
     * Converts the specified {@link Date} instance to a {@link LocalDate}.
     * This method will use timezone information as explained in the class
     * documentation.
     */
    public static LocalDate convertDate(Date date) {
        ZoneId timezone = Platform.getDefaultTimeZone().toZoneId();
        return date.toInstant().atZone(timezone).toLocalDate();
    }

    /**
     * Converts the specified {@link Date} instance to a {@link LocalDateTime}.
     * This method will use timezone information as explained in the class
     * documentation.
     */
    public static LocalDateTime convertDateTime(Date date) {
        ZoneId timezone = Platform.getDefaultTimeZone().toZoneId();
        return date.toInstant().atZone(timezone).toLocalDateTime();
    }

    /**
     * Returns the translations that are used by this class for display names.
     * By default, only English is supported, but this can be extended by
     * adding additional translations.
     */
    public static TranslationBundle getTranslationBundle() {
        return BUNDLE;
    }

    /**
     * Internal representation for mapping between a date/time regular
     * expression and the corresponding date format.
     */
    private record DatePattern(Pattern pattern, String dateFormat) {

        public DatePattern(String pattern, String dateFormat) {
            this(Pattern.compile(pattern), dateFormat);
        }
    }
}
