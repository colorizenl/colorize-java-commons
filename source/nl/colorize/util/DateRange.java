//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import static nl.colorize.util.Formatting.formatDate;
import static nl.colorize.util.Formatting.toDate;

/**
 * Defines a range between two {@link Date}s, with the start date being inclusive
 * and the end date being exclusive. The date range can be split into a number of
 * intervals, such as weeks or months.
 */
public class DateRange implements Comparable<DateRange> {

    private String label;
    private Date start;
    private Date end;

    private ListMultimap<Interval, DateRange> intervalCache;

    public DateRange(Date start, Date end) {
        this(formatDate(start, "yyyy-MM-dd") + " - " + formatDate(end, "yyyy-MM-dd"),
            start, end);
    }

    /**
     * Creates a date range based on two date strings in ISO 8601 format.
     */
    public DateRange(String start, String end) {
        this(toDate(start), toDate(end));
    }

    private DateRange(String label, Date start, Date end) {
        Preconditions.checkArgument(start.getTime() < end.getTime(), "Invalid date range");

        this.label = label;
        this.start = start;
        this.end = end;

        intervalCache = ArrayListMultimap.create();
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
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
        if (intervalCache.containsKey(interval)) {
            List<DateRange> cached = intervalCache.get(interval);
            return ImmutableList.copyOf(cached);
        } else {
            List<DateRange> result = toIntervals(interval);
            intervalCache.putAll(interval, result);
            return result;
        }
    }

    private List<DateRange> toIntervals(Interval interval) {
        switch (interval) {
            case DAY : return toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 1);
            case WEEK : return toIntervals(interval, GregorianCalendar.DAY_OF_MONTH, 7);
            case MONTH : return toIntervals(interval, GregorianCalendar.MONTH, 1);
            case QUARTER : return toIntervals(interval, GregorianCalendar.MONTH, 3);
            case YEAR : return toIntervals(interval, GregorianCalendar.YEAR, 1);
            default : throw new IllegalArgumentException("Interval not supported");
        }
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
            String label = getIntervalLabel(interval, intervalStart);
            intervals.add(new DateRange(label, intervalStart, intervalEnd));
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

    private String getIntervalLabel(Interval interval, Date intervalStart) {
        if (interval == Interval.YEAR) {
            return formatDate(intervalStart, "yyyy");
        } else if (interval == Interval.QUARTER) {
            int quarter = (int) ((intervalStart.getMonth() + 1) / 3.1) + 1;
            return "Q" + quarter + " " + formatDate(intervalStart, "yyyy");
        } else if (interval == Interval.MONTH) {
            return formatDate(intervalStart, "M/yyyy");
        } else {
            return formatDate(intervalStart, "yyyy-MM-dd");
        }
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

    public boolean equals(Object o) {
        if (o instanceof DateRange) {
            DateRange other = (DateRange) o;
            return start.equals(other.start) && end.equals(other.end);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public String toString() {
        return label;
    }

    public enum Interval {
        DAY,
        WEEK,
        MONTH,
        QUARTER,
        YEAR
    }
}
