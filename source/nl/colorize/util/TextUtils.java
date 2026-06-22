//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
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

    /**
     * Removes the specified fragment from the start of {@code str}.
     *
     * @throws IllegalArgumentException if {@code fragment} is empty.
     */
    public static String removeLeading(String str, String fragment) {
        Preconditions.checkArgument(!fragment.isEmpty(), "Empty fragment");
        while (str.startsWith(fragment)) {
            str = str.substring(fragment.length());
        }
        return str;
    }

    /**
     * Removes the specified fragment from the end of {@code str}.
     *
     * @throws IllegalArgumentException if {@code fragment} is empty.
     */
    public static String removeTrailing(String str, String fragment) {
        Preconditions.checkArgument(!fragment.isEmpty(), "Empty fragment");
        while (str.endsWith(fragment)) {
            str = str.substring(0, str.length() - fragment.length());
        }
        return str;
    }

    /**
     * Removes the specified fragment from both the start and the end of
     * {@code str}.
     *
     * @throws IllegalArgumentException if {@code fragment} is empty.
     */
    public static String removeSurrounding(String str, String fragment) {
        Preconditions.checkArgument(!fragment.isEmpty(), "Empty fragment");
        return removeTrailing(removeLeading(str, fragment), fragment);
    }

    /**
     * Removes the specified fragments from the start and the end of
     * {@code str}.
     *
     * @throws IllegalArgumentException if one of the fragments is empty.
     */
    public static String removeSurrounding(String str, String leading, String trailing) {
        Preconditions.checkArgument(!leading.isEmpty(), "Empty fragment");
        Preconditions.checkArgument(!trailing.isEmpty(), "Empty fragment");
        return removeTrailing(removeLeading(str, leading), trailing);
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
     * Returns a list containing all matches for a regular expression, with
     * each match being represented by a {@link MatchResult}.
     *
     * @deprecated This method is made redundant by {@link Matcher#results()},
     *             although that is not yet supported by TeaVM. Avoid using
     *             this method if your application does not need to support
     *             TeaVM.
     */
    @Deprecated
    public static List<MatchResult> match(String input, Pattern regex) {
        Matcher matcher = regex.matcher(input);
        List<MatchResult> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.toMatchResult());
        }
        return matches;
    }

    /**
     * Returns a list containing all matches for a regular expression, with
     * each match being represented by the match group with the specified
     * index.
     */
    public static List<String> matchAll(String input, Pattern regex, int group) {
        Matcher matcher = regex.matcher(input);
        List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group(group));
        }
        return matches;
    }
    
    /**
     * Returns a list containing all matches for a regular expression, with
     * each match being represented by the entire matched text.
     */
    public static List<String> matchAll(String input, Pattern regex) {
        return matchAll(input, regex, 0);
    }

    /**
     * Returns the first match of a regular expression, with the match being
     * represented by the match group with the specified index.
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
     * Returns the first match of a regular expression, with the match being
     * represented by the entire matched text.
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
     *
     * @deprecated Prefer using {@link #matchLines(List, Pattern)} to have more
     *             explicit control over splitting the text into lines.
     */
    @Deprecated
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

    /**
     * Returns a list containing all lines matching a regular expression, with
     * each match being represented by a {@link MatchResult}.
     */
    public static List<MatchResult> matchLines(List<String> lines, Pattern regex) {
        List<MatchResult> matches = new ArrayList<>();
        for (String line : lines) {
            Matcher matcher = regex.matcher(line);
            if (matcher.matches()) {
                matches.add(matcher.toMatchResult());
            }
        }
        return matches;
    }

    /**
     * Limits the length of a string to the specified value, using
     * ellipses ({@code ...}) to indicate the string has been shortened.
     */
    public static String limit(String str, int maxLength) {
        Preconditions.checkArgument(maxLength >= 1, "Invalid maximum length: " + maxLength);
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...";
    }

    /**
     * Groups a list of strings into "chunks" delimited by line matching the
     * specified predicate. This is conceptually similar to {@link Splitter},
     * but operating on entire lines rather than characters. Also,
     * <em>unlike</em> {@link Splitter}, the marker line is included in the
     * resulting chunk.
     */
    public static List<List<String>> splitChunks(List<String> lines, Predicate<String> marker) {
        List<List<String>> chunks = new ArrayList<>();

        for (String line : lines) {
            if (marker.test(line) || chunks.isEmpty()) {
                chunks.add(new ArrayList<>());
            }
            chunks.getLast().add(line);
        }

        chunks.forEach(TextUtils::trimChunk);
        return chunks;
    }

    private static void trimChunk(List<String> chunk) {
        while (!chunk.isEmpty() && chunk.getLast().trim().isEmpty()) {
            chunk.removeLast();
        }
    }

    /**
     * Formats a floating point number with the specified number of decimals.
     * This method is a convenience version for {@link NumberFormat}. It uses
     * the {@code en_US} locale, meaning that it will use the format "1,000.5".
     */
    public static String numberFormat(double n, int decimals) {
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
    public static String timeFormat(double seconds, boolean includeMilliseconds) {
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

    /**
     * Returns a {@link Comparator} that sorts strings in ascending order,
     * based on what is considered a reasonable default for a human-readable
     * list.
     *
     * <ul>
     *   <li>If a string can be parsed as a number, it will be sorted as such.
     *       This helps to avoid confusion when sorting number-strings in
     *       lexicographic order.</li>
     *   <li>{@code null} values are always sorted at the end.</li>
     *   <li>Remaining strings are compared in case-insensitive order.</li>
     * </ul>
     */
    public static Comparator<String> autoSortAsc() {
        return Comparator.nullsLast(TextUtils::autoSort);
    }

    /**
     * Returns a {@link Comparator} that sorts strings in descending order,
     * based on what is considered a reasonable default for a human-readable
     * list.
     *
     * <ul>
     *   <li>If a string can be parsed as a number, it will be sorted as such.
     *       This helps to avoid confusion when sorting number-strings in
     *       lexicographic order.</li>
     *   <li>{@code null} values are always sorted at the end.</li>
     *   <li>Remaining strings are compared in case-insensitive order.</li>
     * </ul>
     */
    public static Comparator<String> autoSortDesc() {
        Comparator<String> base = TextUtils::autoSort;
        return Comparator.nullsLast(base.reversed());
    }

    private static int autoSort(String a, String b) {
        try {
            double numericA = Double.parseDouble(a);
            double numericB = Double.parseDouble(b);
            return Double.compare(numericA, numericB);
        } catch (NumberFormatException e) {
            return a.toLowerCase().compareTo(b.toLowerCase());
        }
    }

    private static BufferedReader toBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader buffer) {
            return buffer;
        } else {
            return new BufferedReader(reader);
        }
    }
}
