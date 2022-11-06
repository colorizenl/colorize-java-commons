//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Creates a log message formatter that uses a compact format with log records
 * only taking up a single line. This makes it easier to process log files in
 * other tools, or scan them manually.
 * <p>
 * Messages are logged using the default time zone for date and time. See
 * {@link Platform#getDefaultTimeZone()} for more information.
 */
public class CompactFormatter extends Formatter {

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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(Platform.getDefaultTimeZone());
        return dateFormat.format(timestamp);
    }
}
