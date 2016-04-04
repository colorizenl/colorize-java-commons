//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log message formatter that is much more compact than the default one. Log
 * messages only take one line, which makes it easier to visually scan log
 * files. 
 */
public class CompactFormatter extends Formatter {

	private boolean printLevel;
	private boolean printTimestamp;
	
	private SimpleDateFormat dateFormat;
	private Date scratchDate;
	
	private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public CompactFormatter() {
		this(true, true);
	}
	
	public CompactFormatter(boolean printLevel, boolean printTimestamp) {
		this.printLevel = printLevel;
		this.printTimestamp = printTimestamp;
		
		dateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
		scratchDate = new Date();
	}
	
	@Override
	public synchronized String format(LogRecord record) {
		scratchDate.setTime(record.getMillis());
		return format(record.getMessage(), record.getThrown(), record.getLevel(), scratchDate);
	}
	
	public String format(String message, Throwable thrown, Level level, Date timestamp) {
		StringBuilder log = new StringBuilder();
		if (printTimestamp) {
			log.append(dateFormat.format(timestamp));
			log.append("  ");
		}
		if (shouldPrintLevel(level)) {
			log.append(level.toString());
			for (int i = level.toString().length(); i < 9; i++) {
				log.append(' ');
			}
		}
		log.append(message);
		log.append(Platform.getLineSeparator());
		if (thrown != null) {
			log.append(LogHelper.getStackTrace(thrown));
		}
		return log.toString();
	}

	private boolean shouldPrintLevel(Level level) {
		return printLevel || (level == Level.WARNING || level == Level.SEVERE);
	}
}
