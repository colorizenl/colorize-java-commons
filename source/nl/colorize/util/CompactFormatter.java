//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log message formatter that uses a compact format so that log messages only
 * contain a single line. This makes it easier to visually scan log files.
 * <p>
 * By default, log messages are reported in the Europe/Amsterdam time zone.
 * If this time zone is not available on the platform it reverts to the
 * platform's default time zone. The time zone can also be set explicitly
 * by providing it in the constructor.
 */
public class CompactFormatter extends Formatter {

    private TimeZone timezone;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_TIME_ZONE = "Europe/Amsterdam";

    public CompactFormatter(TimeZone timezone) {
        this.timezone = timezone;
    }

    public CompactFormatter() {
        Set<String> availableTimeZones = ImmutableSet.copyOf(TimeZone.getAvailableIDs());

        if (availableTimeZones.contains(DEFAULT_TIME_ZONE)) {
            timezone = TimeZone.getTimeZone(DEFAULT_TIME_ZONE);
        } else {
            timezone = TimeZone.getDefault();
        }
    }

    @Override
    public synchronized String format(LogRecord record) {
        Date date = new Date(record.getMillis());
        return format(record.getMessage(), record.getThrown(), record.getLevel(), date);
    }

    public String format(String message, Throwable thrown, Level level, Date timestamp) {
        StringBuilder log = new StringBuilder();
        log.append(format(timestamp));
        log.append("  ");
        log.append(Strings.padEnd(level.toString(), 9, ' '));
        log.append(message);
        log.append(Platform.getLineSeparator());
        if (thrown != null) {
            log.append(LogHelper.getStackTrace(thrown));
        }
        return log.toString();
    }

    private String format(Date timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(timezone);
        return dateFormat.format(timestamp);
    }
}
