//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import nl.colorize.util.DateParser;
import nl.colorize.util.Platform;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Predicate;

import static nl.colorize.util.DateParser.format;

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
            "Invalid date range: " + format(start, "yyyy-MM-dd") + " - " + format(end, "yyyy-MM-dd"));
    }

    /**
     * Secondary constructor that uses {@link DateParser#parse(String)} to
     * parse dates from strings.
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
     * Returns true if this date range includes the specified date, using the
     * same logic describes in {@link #contains(Date)}.
     */
    @Override
    public boolean test(Date date) {
        return contains(date);
    }

    /**
     * Splits this date range into daily intervals. This might include partial
     * days, depending on the start and end date of this date range.
     */
    public List<DateRange> splitDays() {
        return toIntervals(GregorianCalendar.DAY_OF_MONTH, 1, false);
    }

    /**
     * Splits this date range into weekly intervals. This might include partial
     * weeks, depending on the start and end date of this date range.
     */
    public List<DateRange> splitWeeks() {
        return toIntervals(GregorianCalendar.DAY_OF_MONTH, 7, true);
    }

    /**
     * Splits this date range into monthly intervals. This might include
     * partial months, depending on the start and end date of this date range.
     */
    public List<DateRange> splitMonths() {
        return toIntervals(GregorianCalendar.MONTH, 1, false);
    }

    /**
     * Splits this date range into yearly intervals. This might include
     * partial years, depending on the start and end date of this date range.
     */
    public List<DateRange> splitYearly() {
        return toIntervals(GregorianCalendar.YEAR, 1, false);
    }

    private List<DateRange> toIntervals(int field, int increment, boolean resetWeek) {
        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(start);
        if (resetWeek) {
            calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
            calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        } else {
            calendar.set(GregorianCalendar.DAY_OF_MONTH, 1);
        }

        List<DateRange> intervals = new ArrayList<>();

        while (true) {
            Date intervalStart = calendar.getTime();
            long intervalEndTime = getIntervalEnd(intervalStart, field, increment);
            Date intervalEnd = new Date(Math.min(intervalEndTime, end.getTime()));
            intervals.add(new DateRange(intervalStart, intervalEnd));
            calendar.add(field, increment);

            if (intervalEndTime >= end.getTime()) {
                break;
            }
        }

        return intervals;
    }

    private long getIntervalEnd(Date intervalStart, int field, int increment) {
        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(intervalStart);
        calendar.add(field, increment);
        return calendar.getTime().getTime();
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
        return format(start, "yyyy-MM-dd") + " - " + format(end, "yyyy-MM-dd");
    }
}
