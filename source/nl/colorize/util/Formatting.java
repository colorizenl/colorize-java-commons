//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Miscellaneous utility functions for creating formatted strings out of data.
 * Examples are formatting durations, word wrapping text, and formatting a
 * number of bytes in meaningful units.
 */
public final class Formatting {

    private static final String YYYYMMDD = "yyyyMMdd";
    private static final String YYYY_MM_DD = "yyyy-MM-dd";
    private static final String YYYY_MM_DD_TIME = "yyyy-MM-dd HH:mm";
    private static final String YYYY_MM_DD_SECONDS = "yyyy-MM-dd HH:mm:ss";

    private static final Pattern WORD_SEPARATOR_PATTERN = Pattern.compile("[ _]");
    private static final Splitter WORD_SPLITTER = Splitter.on(WORD_SEPARATOR_PATTERN).omitEmptyStrings();

    private Formatting() {
    }

    /**
     * Formats a number with a set amount of decimal places.
     * <p>
     * <strong>Note:</strong> In conventional cases {@code String.format} should
     * be used instead of this method. However, on some platforms, such as TeaVM,
     * {@code printf} might not be fully available or have issues. This method
     * has not been marked as deprecated to keep supporting those platforms.
     */
    public static String numberFormat(float n, int decimals) {
        Preconditions.checkArgument(decimals >= 1, decimals + " decimal places");

        double factor = Math.pow(10, decimals);
        return String.valueOf(Math.round(n * factor) / factor);
    }
    
    /**
     * Formats a time in millisecond precision.
     * @param includeMillis If false, the result is in second precision.
     * @throws IllegalArgumentException if the time is negative.
     */
    public static String timeFormat(long time, boolean includeMillis) {
        Preconditions.checkArgument(time >= 0L, "Cannot format negative time");

        long hours = time / 3_600_000L;
        long minutes = (time % 3_600_000L) / 60_000L;
        long seconds = (time % 3_600_000L % 60_000L) / 1000L;
        long millis = time % 3_600_000L % 60_000L % 1000L;
        
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
        long bytesInMB = 1_048_576L;
        long bytesInGB = 1_073_741_824L;
        
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
     * Wrapper around {@code SimpleDateFormat.format}, that removes the need to
     * create a {@code SimpleDateFormat} instance every single time.
     *
     * @deprecated Use {@link DateParser#format(Date, String)} instead.
     */
    @Deprecated
    public static String formatDate(Date input, String dateFormat) {
        return new SimpleDateFormat(dateFormat).format(input);
    }

    /**
     * Wrapper around {@code SimpleDateFormat.parse}, that throws an unchecked
     * exception instead of a {@link ParseException}.
     *
     * @throws IllegalArgumentException if the input string cannot be parsed
     *         using the provided date format.
     *
     * @deprecated Use {@link DateParser#parse(String, String)} instead.
     */
    @Deprecated
    public static Date parseDate(String input, String dateFormat) {
        try {
            return new SimpleDateFormat(dateFormat).parse(input);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse date: " + input, e);
        }
    }

    /**
     * Utility method to parse a date that has been formatted using the ISO 8601
     * date format. This allows for multiple precision levels, so the following
     * formats are supported:
     *
     * <ul>
     *   <li>yyyy-mm-dd hh:mm:ss
     *   <li>yyyy-mm-dd hh:mm
     *   <li>yyyy-mm-dd
     * </ul>
     *
     * @throws IllegalArgumentException if the date's notation is not included
     *         in the list above..
     *
     * @deprecated Use {@link DateParser#parse(String)} instead.
     */
    @Deprecated
    public static Date toDate(String date) {
        Map<Integer, String> formats = ImmutableMap.of(
            19, YYYY_MM_DD_SECONDS,
            16, YYYY_MM_DD_TIME,
            10, YYYY_MM_DD,
            8, YYYYMMDD
        );

        String formatString = formats.get(date.length());

        if (formatString == null) {
            throw new IllegalArgumentException("Date notation not supported: " + date);
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(formatString);
            return dateFormat.parse(date);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Cannot parse date " + date +
                " using format " + formatString);
        }
    }

    /**
     * Returns the hexcode for the specified byte array. For example, 0xDEADBEEF
     * will return a hexcode of 6465616462656566.
     */
    public static String toHexString(byte[] bytes) {
        // Implementation based on
        // http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
