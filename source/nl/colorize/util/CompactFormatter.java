//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Strings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
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
    public String format(LogRecord record) {
        StringBuilder log = new StringBuilder();
        log.append(format(record.getMillis()));
        log.append("  ");
        log.append(Strings.padEnd(record.getLevel().toString(), 9, ' '));
        log.append(record.getMessage());
        log.append(System.lineSeparator());
        if (record.getThrown() != null) {
            log.append(LogHelper.getStackTrace(record.getThrown()));
        }
        return log.toString();
    }

    private String format(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(Platform.getDefaultTimeZone());
        return dateFormat.format(new Date(timestamp));
    }
}
