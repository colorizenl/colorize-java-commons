//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Miscellaneous utility and convenience versions for working with text,
 * including regular expressions.
 */
public final class TextUtils {

    public static final Splitter LINE_SPLITTER = Splitter.on("\n");
    public static final Joiner LINE_JOINER = Joiner.on("\n");

    private static final Pattern WORD_SEPARATOR = Pattern.compile("[ _]");
    private static final Splitter WORD_SPLITTER = Splitter.on(WORD_SEPARATOR).omitEmptyStrings();
    
    private TextUtils() {
    }

    /**
     * Returns the string {@code str} formatted in "title format". This will make
     * the first letter of each word uppercase, and the rest of the word lowercase.
     * Both whitespace characters and underscores are considered word separators.
     */
    public static String toTitleCase(String str) {
        if (str.trim().isEmpty()) {
            return str;
        }

        StringBuilder sb = new StringBuilder(str.length());
        for (String word : WORD_SPLITTER.split(str)) {
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.substring(0, sb.length() - 1);
    }
    
    /**
     * Returns the number of occurrences of the string {@code needle} within the
     * string {@code haystack}.
     *
     * @throws IllegalArgumentException if {@code needle} is an empty string.
     */
    public static int countOccurrences(String haystack, String needle) {
        Preconditions.checkArgument(!needle.isEmpty(), "Cannot provide empty string");
        
        int index = 0;
        int count = 0;
        
        while (index < haystack.length()) {
            int occurrenceIndex = haystack.indexOf(needle, index);
            if (occurrenceIndex == -1) {
                break;
            }
            index = occurrenceIndex + needle.length();
            count++;
        }
        
        return count;
    }
    
    /**
     * Adds the specified prefix string to the start of another string, but 
     * only if it is not already there.
     */
    public static String addLeading(String str, String prefix) {
        if (prefix.isEmpty() || str.startsWith(prefix)) {
            return str;
        }
        return prefix + str;
    }
    
    /**
     * Adds the specified suffix string to the end of another string, but 
     * only if it is not already there.
     */
    public static String addTrailing(String str, String suffix) {
        if (suffix.isEmpty() || str.endsWith(suffix)) {
            return str;
        }
        return str + suffix;
    }
    
    public static String removeLeading(String str, String leading) {
        while (str.startsWith(leading)) {
            str = str.substring(leading.length());
        }
        return str;
    }

    public static String removeTrailing(String str, String trailing) {
        while (str.endsWith(trailing)) {
            str = str.substring(0, str.length() - trailing.length());
        }
        return str;
    }

    public static String removeLeadingAndTrailing(String str, String leading, String trailing) {
        str = removeLeading(str, leading);
        str = removeTrailing(str, trailing);
        return str;
    }

    public static boolean startsWith(String str, Collection<String> alternatives) {
        Preconditions.checkArgument(!alternatives.isEmpty(),
            "Must provide at least one alternative");

        return alternatives.stream()
            .anyMatch(str::startsWith);
    }

    public static boolean endsWith(String str, Collection<String> alternatives) {
        Preconditions.checkArgument(!alternatives.isEmpty(),
            "Must provide at least one alternative");

        return alternatives.stream()
            .anyMatch(str::endsWith);
    }

    public static boolean contains(String str, Collection<String> alternatives) {
        Preconditions.checkArgument(!alternatives.isEmpty(),
            "Must provide at least one alternative");

        return alternatives.stream()
            .anyMatch(str::contains);
    }

    /**
     * Returns all matches for a regular expression.
     *
     * @param group Adds the specified capture group to the list of results.
     */
    public static List<String> matchAll(String input, Pattern regex, int group) {
        List<String> matches = new ArrayList<>();
        Matcher matcher = regex.matcher(input);
        while (matcher.find()) {
            matches.add(matcher.group(group));
        }
        return matches;
    }
    
    /**
     * Returns all matches for a regular expression.
     */
    public static List<String> matchAll(String input, Pattern regex) {
        return matchAll(input, regex, 0);
    }
    
    /**
     * Returns the first match of a regular expression.
     *
     * @param group Adds the specified capture group to the list of results.
     */
    public static Optional<String> matchFirst(String input, Pattern regex, int group) {
        Matcher matcher = regex.matcher(input);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * Returns the first match of a regular expression.
     */
    public static Optional<String> matchFirst(String input, Pattern regex) {
        return matchFirst(input, regex, 0);
    }
    
    /**
     * Reads all lines, and returns only the lines that match a regular expression.
     * The reader is closed afterward.
     *
     * @param group Adds the specified capture group to the list of results.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(Reader input, Pattern regex, int group) throws IOException {
        List<String> matches = new ArrayList<>();

        try (BufferedReader reader = toBufferedReader(input)) {
            reader.lines().forEach(line -> {
                Matcher matcher = regex.matcher(line);
                if (matcher.matches()) {
                    matches.add(matcher.group(group));
                }
            });
        }

        return matches;
    }
    
    /**
     * Reads all lines, and returns only the lines that match a regular expression. 
     * The reader is closed afterward.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(Reader input, Pattern regex) throws IOException {
        return matchLines(input, regex, 0);
    }

    /**
     * Reads all lines in a string, and returns only the lines that match a regular
     * expression.
     *
     * @param group Adds the specified capture group to the list of results.
     */
    public static List<String> matchLines(String input, Pattern regex, int group) {
        try {
            return matchLines(new StringReader(input), regex, group);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    /**
     * Reads all lines in a string, and returns only the lines that match a regular
     * expression. 
     */
    public static List<String> matchLines(String input, Pattern regex) {
        return matchLines(input, regex, 0);
    }

    /**
     * Runs a regular expression on all lines within the specified string, then
     * invokes a callback function for every match.
     */
    public static void matchLines(String input, Pattern regex, Consumer<List<String>> callback) {
        for (String line : LINE_SPLITTER.split(input)) {
            Matcher matcher = regex.matcher(line);
            if (!line.isBlank() && matcher.matches()) {
                List<String> groups = new ArrayList<>();
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    groups.add(matcher.group(i));
                }

                callback.accept(groups);
            }
        }
    }

    private static BufferedReader toBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader) {
            return (BufferedReader) reader;
        } else {
            return new BufferedReader(reader);
        }
    }

    /**
     * Formats a floating point number with the specified number of decimals.
     * This method is a convenience version for {@link NumberFormat}. It uses
     * the {@code en_US} locale, meaning that it will use the format "1,000.5".
     */
    public static String numberFormat(float n, int decimals) {
        Preconditions.checkArgument(decimals >= 0,
            "Invalid number of decimals: " + decimals);

        NumberFormat format = NumberFormat.getInstance(Locale.US);
        format.setMinimumFractionDigits(decimals);
        format.setMaximumFractionDigits(decimals);
        format.setGroupingUsed(true);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(n);
    }

    /**
     * Formats elapsed time, represented by the number of seconds. The
     * granularity of the returned value depends on how much time has elapsed.
     * For example, 3 seconds will produce “0:03”, while an hour will produce
     * “1:00:00”. If the value of {@code includeMilliseconds} is set to true,
     * the number of milliseconds will be appended to the result.
     */
    public static String timeFormat(float seconds, boolean includeMilliseconds) {
        return timeFormat(Math.round(seconds * 1000f), includeMilliseconds);
    }

    /**
     * Formats elapsed time, represented by the number of milliseconds. The
     * granularity of the returned value depends on how much time has elapsed.
     * For example, 3 seconds will produce “0:03”, while an hour will produce
     * “1:00:00”. If the value of {@code includeMilliseconds} is set to true,
     * the number of milliseconds will be appended to the result.
     */
    public static String timeFormat(long milliseconds, boolean includeMilliseconds) {
        Preconditions.checkArgument(milliseconds >= 0L,
            "Cannot format negative elapsed time: " + milliseconds);

        if (milliseconds == 0L) {
            return "----";
        }

        long hours = milliseconds / 3_600_000L;
        long minutes = (milliseconds % 3_600_000L) / 60_000L;
        long seconds = (milliseconds % 60_000L) / 1000L;
        long remaining = milliseconds % 1000L;

        if (includeMilliseconds) {
            return timeFormat(hours, minutes, seconds, remaining);
        } else {
            return timeFormat(hours, minutes, seconds);
        }
    }

    private static String timeFormat(long hours, long minutes, long seconds) {
        StringBuilder buffer = new StringBuilder();
        if (hours > 0L) {
            buffer.append(hours);
            buffer.append(":");
            buffer.append(Strings.padStart(String.valueOf(minutes), 2, '0'));
        } else {
            buffer.append(minutes);
        }
        buffer.append(":");
        buffer.append(Strings.padStart(String.valueOf(seconds), 2, '0'));
        return buffer.toString();
    }

    private static String timeFormat(long hours, long minutes, long seconds, long milliseconds) {
        StringBuilder buffer = new StringBuilder();
        if (hours > 0L) {
            buffer.append(hours);
            buffer.append(":");
            buffer.append(Strings.padStart(String.valueOf(minutes), 2, '0'));
            buffer.append(":");
            buffer.append(Strings.padStart(String.valueOf(seconds), 2, '0'));
        } else if (minutes > 0L) {
            buffer.append(minutes);
            buffer.append(":");
            buffer.append(Strings.padStart(String.valueOf(seconds), 2, '0'));
        } else {
            buffer.append(seconds);
        }
        buffer.append(".");
        buffer.append(Strings.padStart(String.valueOf(milliseconds), 3, '0'));
        return buffer.toString();
    }
}
