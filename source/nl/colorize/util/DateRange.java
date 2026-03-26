//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Defines a range between two {@link Date}s, with the start date being inclusive
 * and the end date being exclusive. Although the name of this class implies a
 * <em>date</em> range, this is technically a <em>date/time</em> range since
 * {@link Date} instances always include a time.
 * <p>
 * If dates do not specify an explicit time zone, the default time zone will be
 * used. See {@link Platform#getDefaultTimeZone()} for how to configure the
 * default time zone.
 */
public record DateRange(Date start, Date end) implements Predicate<Date>, Comparable<DateRange> {

    public DateRange {
        Preconditions.checkArgument(start.getTime() < end.getTime(),
            "Invalid date range (start date %s, end date %s)", start, end);
    }

    /**
     * Secondary constructor that uses {@link DateParser#parse(String)} to
     * parse the specified start and end date.
     */
    public DateRange(String start, String end) {
        this(DateParser.parse(start), DateParser.parse(end));
    }

    /**
     * Returns true if this date range contains the specified date. Note the
     * start date is considered inclusive but the end date is considered
     * exclusive, so this method will return false if the argument matches
     * the end date exactly.
     */
    public boolean contains(Date date) {
        return date.getTime() >= start.getTime() && date.getTime() < end.getTime();
    }

    /**
     * Returns true if this date range contains the specified date.
     *
     * @see #contains(Date)
     */
    @Override
    public boolean test(Date date) {
        return contains(date);
    }

    /**
     * Splits this date range into weekly intervals. This might include partial
     * weeks, depending on the start and end date of this date range. Using
     * this method is equivalent to {@code DateRange.weekly(start, end)}.
     */
    public List<DateRange> splitWeeks() {
        return weekly(start, end);
    }

    /**
     * Splits this date range into monthly intervals. This might include
     * partial months, depending on the start and end date of this date range.
     * Using this method is equivalent to {@code DateRange.monthly(start, end)}.
     */
    public List<DateRange> splitMonths() {
        return monthly(start, end);
    }

    /**
     * Splits this date range into yearly intervals. This might include
     * partial years, depending on the start and end date of this date range.
     * Using this method is equivalent to {@code DateRange.yearly(start, end)}.
     */
    public List<DateRange> splitYears() {
        return yearly(start, end);
    }

    /**
     * Creates a new date range that represents the smallest possible period
     * that includes both this date range and the specified other date range.
     */
    public DateRange span(DateRange other) {
        long spanStart = Math.min(start.getTime(), other.start.getTime());
        long spanEnd = Math.max(end.getTime(), other.end.getTime());
        return new DateRange(new Date(spanStart), new Date(spanEnd));
    }

    @Override
    public int compareTo(DateRange other) {
        return start.compareTo(other.start);
    }

    @Override
    public String toString() {
        String formattedStart = DateParser.format(start, "yyyy-MM-dd");
        String formattedEnd = DateParser.format(end, "yyyy-MM-dd");
        if (formattedStart.equals(formattedEnd)) {
            return formattedStart;
        }
        return formattedStart + ".." + formattedEnd;
    }

    /**
     * Factory method that returns a list of {@link DateRange}s for every day
     * between the specified start date (inclusive) and end date (exclusive).
     */
    public static List<DateRange> daily(Date start, Date end) {
        DateRange period = new DateRange(start, end);
        return generate(period, GregorianCalendar.DAY_OF_MONTH, 1);
    }

    /**
     * Factory method that returns a list of {@link DateRange}s for every week
     * between the specified start date (inclusive) and end date (exclusive).
     * Weeks are assumed to start on Monday. The result may include partial
     * weeks if the start and end date do not align to week boundaries.
     */
    public static List<DateRange> weekly(Date start, Date end) {
        DateRange period = new DateRange(start, end);
        return generate(period, GregorianCalendar.DAY_OF_MONTH, 7);
    }

    /**
     * Factory method that returns a list of {@link DateRange}s for every month
     * between the specified start date (inclusive) and end date (exclusive).
     * The result may include partial months if the start and end date do not
     * align to month boundaries.
     */
    public static List<DateRange> monthly(Date start, Date end) {
        DateRange period = new DateRange(start, end);
        return generate(period, GregorianCalendar.MONTH, 1);
    }

    /**
     * Factory method that returns a list of {@link DateRange}s for every
     * quarter between the specified start date (inclusive) and end date
     * (exclusive). The result may include partial quarters if the start
     * and end date do not align to quarter boundaries.
     */
    public static List<DateRange> quarterly(Date start, Date end) {
        DateRange period = new DateRange(start, end);
        return generate(period, GregorianCalendar.MONTH, 3);
    }

    /**
     * Factory method that returns a list of {@link DateRange}s for every year
     * between the specified start date (inclusive) and end date (exclusive).
     * The result may include partial years if the start and end date do not
     * align to year boundaries.
     */
    public static List<DateRange> yearly(Date start, Date end) {
        DateRange period = new DateRange(start, end);
        return generate(period, GregorianCalendar.YEAR, 1);
    }

    /**
     * Generates a list of {@link DateRange}s by splitting an overall
     * {@link DateRange} based on the specified interval. The result may
     * include "partial" results, for example a partial week or partial
     * month, if the original overall {@link DateRange} does not align
     * to the requested interval.
     */
    private static List<DateRange> generate(DateRange period, int field, int amount) {
        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(period.start);
        reset(calendar, field, amount);

        List<DateRange> intervals = new ArrayList<>();

        while (calendar.getTime().getTime() < period.end.getTime()) {
            Date intervalStart = calendar.getTime();
            calendar.add(field, amount);
            Date intervalEnd = calendar.getTime();
            intervals.add(new DateRange(intervalStart, intervalEnd));
        }

        trim(period, intervals);

        return intervals;
    }

    private static void reset(GregorianCalendar calendar, int field, int amount) {
        if (field == GregorianCalendar.DAY_OF_MONTH && amount == 7) {
            calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
            calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        } else if (field != GregorianCalendar.DAY_OF_MONTH) {
            calendar.set(GregorianCalendar.DAY_OF_MONTH, 1);
        }
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);
    }

    private static void trim(DateRange period, List<DateRange> intervals) {
        if (intervals.isEmpty()) {
            return;
        }

        DateRange first = intervals.getFirst();
        DateRange last = intervals.getLast();

        if (first.start.getTime() < period.start.getTime()) {
            intervals.removeFirst();
            intervals.addFirst(new DateRange(period.start, first.end));
        }

        if (last.end.getTime() > period.end.getTime()) {
            intervals.removeLast();
            intervals.addLast(new DateRange(last.start, period.end));
        }
    }

    /**
     * Returns the first matching {@link DateRange} containing the specified
     * date. Returns an empty date if none of the options match.
     */
    public static Optional<DateRange> match(Date needle, List<DateRange> haystack) {
        return haystack.stream()
            .filter(dateRange -> dateRange.test(needle))
            .findFirst();
    }
}
