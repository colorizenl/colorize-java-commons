//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import nl.colorize.util.swing.SwingUtils;

/**
 * Miscellaneous utility functions for creating formatted strings out of data.
 * Examples are formatting durations, word wrapping text, and formatting a
 * number of bytes in meaningful units.
 * <p>
 * This class defines a number of {@code SimpleDateFormat} patterns for commonly
 * used date notations as constants. These constants are not instances of
 * {@code SimpleDateFormat} itself because that class is (unfortunately) not
 * thread safe. 
 */
public final class Formatting {

    // SimpleDateFormat patterns, see class documentation.
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYY_MM_DD_TIME = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_SECONDS = "yyyy-MM-dd HH:mm:ss";
    public static final String DD_MM_YYYY = "dd-MM-yyyy";
    public static final String DD_MM_YYYY_TIME = "dd-MM-yyyy HH:mm";
    public static final String DD_MM_YYYY_SECONDS = "dd-MM-yyyy HH:mm:ss";
    public static final String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss";
    
    private static final Pattern WORD_SEPARATOR_PATTERN = Pattern.compile("[ _]");
    private static final Splitter WORD_SPLITTER = Splitter.on(WORD_SEPARATOR_PATTERN).omitEmptyStrings();

    private static final DynamicResourceBundle DURATIONS_BUNDLE = SwingUtils.getCustomComponentsBundle();
    private static final long MILLIS_IN_HOUR = 3600000L;
    private static final long MILLIS_IN_DAY = 24L * MILLIS_IN_HOUR;
    // Number of milliseconds in each time unit. Note that these values are an
    // apporoximation, they are not intended to be exact. The values for month
    // and year in particular are not that accurate, but since this is only used
    // for human-readable text labels the error margin is acceptable.
    private static final Map<String,Long> TIME_UNITS = new ImmutableMap.Builder<String,Long>()
            .put("SECOND", 1000L)
            .put("MINUTE", 60000L)
            .put("HOUR", MILLIS_IN_HOUR)
            .put("DAY", MILLIS_IN_DAY)
            .put("WEEK", MILLIS_IN_DAY * 7L)
            .put("MONTH", MILLIS_IN_DAY * 30L)
            .put("YEAR", MILLIS_IN_DAY * 365L)
            .build();

    private Formatting() {
    }

    /**
     * Formats a number with a set amount of decimal places.
     * @throws IllegalArgumentException for less than one decimal places.
     * @deprecated Use {@link java.text.DecimalFormat} or
     *             {@link java.lang.String#format(String, Object...)} instead.
     */
    @Deprecated
    public static String numberFormat(float n, int decimals) {
        if (decimals < 1) {
            throw new IllegalArgumentException(decimals + " decimal places");
        }
        
        double factor = Math.pow(10, decimals);
        return String.valueOf(Math.round(n * factor) / factor);
    }
    
    /**
     * Formats a time in millisecond precision.
     * @param includeMillis If false, the result is in second precision.
     * @throws IllegalArgumentException if the time is negative.
     */
    public static String timeFormat(long time, boolean includeMillis) {
        if (time < 0) {
            throw new IllegalArgumentException("Cannot format negative time");
        }
        
        long hours = time / 3600000L;
        long minutes = (time % 3600000L) / 60000L;
        long seconds = (time % 3600000L % 60000L) / 1000L;
        long millis = time % 3600000L % 60000L % 1000L;
        
        String formatted = "";
        if (hours > 0) {
            formatted = String.format("%d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        } else if (minutes > 0) {
            formatted = String.format("%d:%02d.%03d", minutes, seconds, millis);
        } else {
            formatted = String.format("%d.%03d", seconds, millis);
        }
        
        if (!includeMillis) {
            formatted = formatted.substring(0, formatted.length() - 4);
        }
        
        return formatted;
    }
    
    /**
     * Formats the difference between two times in millisecond precision.
     * @param includeMillis If false, the result is in second precision.
     * @throws IllegalArgumentException if one of the durations is negative. 
     */
    public static String timeDiffFormat(int time1, int time2, boolean includeMillis) {
        StringBuilder format = new StringBuilder(15);
        format.append(time1 < time2 ? "- " : "+ ");
        format.append(timeFormat(Math.abs(time1 - time2), includeMillis));
        return format.toString();
    }
    
    /**
     * Returns the string {@code s} formatted in "title format". This will make
     * the first letter of each word uppercase, and the rest of the word lowercase.
     * Both whitespace characters and underscores are considered word separators.
     */
    public static String toTitleCase(String str) {
        if (str.trim().length() == 0) {
            return str;
        }
        
        StringBuilder sb = new StringBuilder(str.length());
        for (String word : WORD_SPLITTER.split(str)) {
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().substring(0, sb.length() - 1);
    }
    
    /**
     * Formats a number of bytes. This method assumes 1 KB is 1024, not 1000.
     * Examples of returned values are "100 bytes", "1.23 KB", and "6.3 MB".
     * @param decimals Number of digits behind the comma.
     */
    public static String memoryFormat(long bytes, int decimals) {
        long bytesInKB = 1024L;
        long bytesInMB = 1048576L;
        long bytesInGB = 1073741824L;
        
        if (bytes >= bytesInGB) {
            float gb = (float) bytes / (float) bytesInGB;
            return String.format("%." + decimals + "f GB", gb);
        } else if (bytes >= bytesInMB) {
            float mb = (float) bytes / (float) bytesInMB;
            return String.format("%." + decimals + "f MB", mb);
        } else if (bytes >= bytesInKB) {
            float kb = (float) bytes / (float) bytesInKB;
            return String.format("%." + decimals + "f KB", kb);
        } else {
            return bytes + " bytes";
        }
    }
    
    /**
     * Formats the difference between two dates, using text labels from the
     * specified resource bundle.
     */
    public static String formatDateDiff(Date first, Date second, ResourceBundle bundle) {
        long diff = Math.abs(second.getTime() - first.getTime());
        String timeUnit = getAppropriateTimeUnit(diff);
        long diffInUnit = diff / TIME_UNITS.get(timeUnit);
        
        if (bundle instanceof DynamicResourceBundle) {
            return ((DynamicResourceBundle) bundle).getString(timeUnit, diffInUnit);
        } else {
            return new DynamicResourceBundle(bundle).getString(timeUnit, diffInUnit);
        }
    }
    
    private static String getAppropriateTimeUnit(long diff) {
        String timeUnit = "SECOND";
        for (Map.Entry<String,Long> entry : TIME_UNITS.entrySet()) {
            if (Math.abs(diff) >= 2L * entry.getValue()) {
                timeUnit = entry.getKey();
            }
        }
        return timeUnit;
    }

    /**
     * Formats the difference between a date and now, using text labels from
     * the specified resource bundle.
     */
    public static String formatDateDiff(Date date, ResourceBundle bundle) {
        return formatDateDiff(date, new Date(), bundle);
    }
    
    /**
     * Formats the difference between two dates, using text labels from the
     * default resource bundle.
     */
    public static String formatDateDiff(Date first, Date second) {
        return formatDateDiff(first, second, DURATIONS_BUNDLE);
    }
    
    /**
     * Formats the difference between a date and now, using text labels from
     * the default resource bundle.
     */
    public static String formatDateDiff(Date date) {
        return formatDateDiff(date, DURATIONS_BUNDLE);
    }
    
    /**
     * Takes an arbitrary string and attempts to format it in a human-readable 
     * way. Usage of uppercase letters, hyphens, and underscores will be normalized.
     * @deprecated This method is too vague about the ways it changes the original
     *             string, which makes it difficult to determine whether it should
     *             be used in a specific situation. 
     */
    @Deprecated
    public static String humanReadableFormat(String str) {
        if (str.isEmpty()) {
            return str;
        }
        
        String formatted = str.trim();
        if (Character.isUpperCase(formatted.charAt(0))) {
            formatted = formatted.substring(0, 1) + formatted.substring(1).toLowerCase();
        } else {
            formatted = formatted.toLowerCase();
        }
        formatted = formatted.replace("_", " ");
        formatted = formatted.replace("-", " ");
        return formatted;
    }
    
    /**
     * Formats a file's location in a human readable format containing the file's
     * name and parent directory. This can be used to communicate file locations
     * in a user interface to communicate the file's location without having to
     * display the file's full path.
     */
    public static String formatHumanReadableFileLocation(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return file.getName();
        }
        return parent.getName() + " \u00BB " + file.getName(); 
    }
}
