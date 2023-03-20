//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.base.Preconditions;
import nl.colorize.util.DateParser;
import nl.colorize.util.Platform;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static nl.colorize.util.DateParser.format;

/**
 * Defines a range between two {@link Date}s, with the start date being inclusive
 * and the end date being exclusive. The date range can be split into a number of
 * intervals, such as weeks or months. An interval of {@link ChronoUnit#FOREVER}
 * is used to indicate a date range with a custom interval.
 * <p>
 * If dates do not specify an explicit time zone, the default time zone will be
 * used. See {@link Platform#getDefaultTimeZone()} for how to configure the
 * default time zone.
 */
public record DateRange(Date start, Date end, ChronoUnit interval) implements Comparable<DateRange> {

    public DateRange {
        Preconditions.checkArgument(start.getTime() < end.getTime(),
            "Invalid date range");
    }

    public DateRange(Date start, Date end) {
        this(start, end, ChronoUnit.FOREVER);
    }

    public DateRange(String start, String end) {
        this(DateParser.parse(start), DateParser.parse(end), ChronoUnit.FOREVER);
    }

    public boolean contains(Date date) {
        return date.getTime() >= start.getTime() && date.getTime() < end.getTime();
    }

    /**
     * Splits this date range into a number of intervals. Note that the intervals
     * might not exactly match the start and end date of the original date range.
     * For example, splitting the date range between 2018-10-15 and 2019-12-15
     * by month will yield October, November, and December 2018.
     * <p>
     * This method supports the following time units:
     * <ul>
     *   <li>{@link ChronoUnit#FOREVER}</li>
     *   <li>{@link ChronoUnit#DAYS}</li>
     *   <li>{@link ChronoUnit#WEEKS}</li>
     *   <li>{@link ChronoUnit#MONTHS}</li>
     *   <li>{@link ChronoUnit#YEARS}</li>
     * </ul>
     *
     * @throws IllegalArgumentException if the requested time unit is not
     *         supported by this method.
     */
    public List<DateRange> split(ChronoUnit interval) {
        return switch (interval) {
            case FOREVER -> List.of(this);
            case DAYS -> toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 1);
            case WEEKS -> toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 7);
            case MONTHS -> toIntervals(interval, GregorianCalendar.MONTH, 1);
            case YEARS -> toIntervals(interval, GregorianCalendar.YEAR, 1);
            default -> throw new IllegalArgumentException("Unit not supported: " + interval);
        };
    }

    private List<DateRange> toIntervals(ChronoUnit interval, int field, int increment) {
        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(start);
        if (interval == ChronoUnit.WEEKS) {
            calendar.setFirstDayOfWeek(GregorianCalendar.MONDAY);
            calendar.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.MONDAY);
        } else {
            calendar.set(GregorianCalendar.DAY_OF_MONTH, 1);
        }

        List<DateRange> intervals = new ArrayList<>();

        while (calendar.getTime().getTime() <= end.getTime()) {
            Date intervalStart = calendar.getTime();
            Date intervalEnd = getIntervalEnd(intervalStart, field, increment);
            intervals.add(new DateRange(intervalStart, intervalEnd, interval));
            calendar.add(field, increment);
        }

        return intervals;
    }

    private Date getIntervalEnd(Date intervalStart, int field, int increment) {
        GregorianCalendar calendar = new GregorianCalendar(Platform.getDefaultTimeZone());
        calendar.setTime(intervalStart);
        calendar.add(field, increment);
        return new Date(calendar.getTime().getTime() - 1L);
    }

    /**
     * Splits this date range into intervals, then returns the first interval
     * that contains the specified date. See the documentation for
     * {@link #split(ChronoUnit)} for a description on how the date range is
     * split into intervals.
     */
    public Optional<DateRange> matchInterval(ChronoUnit interval, Date date) {
        return split(interval).stream()
            .filter(subRange -> subRange.contains(date))
            .findFirst();
    }

    /**
     * Creates a new date range that spans both this and {@code other}.
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
        return switch (interval) {
            case DAYS, WEEKS -> format(start, "yyyy-MM-dd");
            case MONTHS -> format(start, "M/yyyy");
            case YEARS -> format(start, "yyyy");
            default -> format(start, "yyyy-MM-dd") + " - " + format(end, "yyyy-MM-dd");
        };
    }
}
