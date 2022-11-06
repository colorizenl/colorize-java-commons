//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static nl.colorize.util.DateParser.format;

/**
 * Defines a range between two {@link Date}s, with the start date being inclusive
 * and the end date being exclusive. The date range can be split into a number of
 * intervals, such as weeks or months.
 */
public record DateRange(Date start, Date end, Interval interval) implements Comparable<DateRange> {

    public DateRange {
        Preconditions.checkArgument(start.getTime() < end.getTime(),
            "Invalid date range");
    }

    public DateRange(Date start, Date end) {
        this(start, end, Interval.FREE);
    }

    public DateRange(String start, String end) {
        this(DateParser.parse(start), DateParser.parse(end), Interval.FREE);
    }

    public boolean contains(Date date) {
        return date.getTime() >= start.getTime() && date.getTime() < end.getTime();
    }

    /**
     * Splits this date range into a number of intervals. Note that the intervals
     * might not exactly match the start and end date of the original date range.
     * For example, splitting the date range between 2018-10-15 and 2019-12-15
     * by month will yield October, November, and December 2018.
     */
    public List<DateRange> split(Interval interval) {
        return switch (interval) {
            case FREE -> List.of(this);
            case DAY -> toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 1);
            case WEEK -> toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 7);
            case MONTH -> toIntervals(interval, GregorianCalendar.MONTH, 1);
            case QUARTER -> toIntervals(interval, GregorianCalendar.MONTH, 3);
            case YEAR -> toIntervals(interval, GregorianCalendar.YEAR, 1);
        };
    }

    private List<DateRange> toIntervals(Interval interval, int field, int increment) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(start);
        if (interval == Interval.WEEK) {
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
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(intervalStart);
        calendar.add(field, increment);
        return new Date(calendar.getTime().getTime() - 1L);
    }

    /**
     * Splits this date range into intervals, then returns the first interval
     * that contains the specified date. See the documentation for
     * {@link #split(Interval)} for a description on how the date range is
     * split into intervals.
     */
    public Optional<DateRange> matchInterval(Interval interval, Date date) {
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
        int quarter = (int) ((start.getMonth() + 1) / 3.1) + 1;

        return switch (interval) {
            case FREE -> format(start, "yyyy-MM-dd") + " - " + format(end, "yyyy-MM-dd");
            case DAY, WEEK -> format(start, "yyyy-MM-dd");
            case MONTH -> format(start, "M/yyyy");
            case QUARTER -> "Q" + quarter + " " + format(start, "yyyy");
            case YEAR -> format(start, "yyyy");
        };
    }

    public enum Interval {
        FREE,
        DAY,
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }
}
