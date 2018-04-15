//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Miscellaneous utility and convenience versions for working with text.
 */
public final class TextUtils {
    
    private static final CharMatcher TEXT_MATCHER = Escape.CHARACTER.or(CharMatcher.whitespace());

    private TextUtils() {
    }
    
    /**
     * Returns the number of occurrences of the string {@code needle} within the
     * string {@code haystack}.
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
            index = occurrenceIndex + 1;
            count++;
        }
        
        return count;
    }
    
    /**
     * Adds the specified prefix string to the start of another string, but 
     * only if it is not already there.
     */
    public static String addLeading(String str, String prefix) {
        if (str.startsWith(prefix)) {
            return str;
        }
        return prefix + str;
    }
    
    /**
     * Adds the specified suffix string to the end of another string, but 
     * only if it is not already there.
     */
    public static String addTrailing(String str, String suffix) {
        if (str.endsWith(suffix)) {
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
    
    /**
     * Returns all matches for a regular expression.
     * @param group Adds the specified capture group to the list of results.
     */
    public static List<String> matchAll(String input, Pattern regex, int group) {
        List<String> matches = new ArrayList<String>();
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
     * @param group Adds the specified capture group to the list of results.
     */
    public static Optional<String> matchFirst(String input, Pattern regex, int group) {
        Matcher matcher = regex.matcher(input);
        if (matcher.find()) {
            return Optional.of(matcher.group(group));
        } else {
            return Optional.absent();
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
     * The reader is closed afterwards.
     * @param group Adds the specified capture group to the list of results.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(Reader input, Pattern regex, int group) throws IOException {
        List<String> matches = new ArrayList<String>();
        for (String line : LoadUtils.readLines(input)) {
            Matcher matcher = regex.matcher(line);
            if (matcher.matches()) {
                matches.add(matcher.group(group));
            }
        }
        return matches;
    }
    
    /**
     * Reads all lines, and returns only the lines that match a regular expression. 
     * The reader is closed afterwards.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(Reader input, Pattern regex) throws IOException {
        return matchLines(input, regex, 0);
    }
    
    /**
     * Reads all lines from a file, and returns only the lines that match a 
     * regular expression.
     * @param group Adds the specified capture group to the list of results.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(File input, Charset charset, Pattern regex, int group) 
            throws IOException {
        return matchLines(Files.newReader(input, charset), regex, group);
    }
    
    /**
     * Reads all lines from a file, and returns only the lines that match a 
     * regular expression. 
     * @throws IOException if an I/O error occurs while reading.
     */
    public static List<String> matchLines(File input, Charset charset, Pattern regex) throws IOException {
        return matchLines(input, charset, regex, 0);
    }
    
    /**
     * Reads all lines in a string, and returns only the lines that match a regular
     * expression. 
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
     * Removes all text between and including {@code from} and {@code to}. If the
     * string does not contain both markers this does nothing and returns the 
     * original string.
     */
    public static String removeBetween(String input, String from, String to) {
        int fromIndex = input.indexOf(from);
        int toIndex = input.indexOf(to);
        
        if (fromIndex == -1 || toIndex == -1 || fromIndex >= toIndex) {
            return input;
        }
        
        StringBuilder buffer = new StringBuilder();
        buffer.append(input.substring(0, fromIndex));
        buffer.append(input.substring(toIndex + to.length()));
        return buffer.toString();
    }
    
    /**
     * Returns the longest possible string that is a prefix for both {@code a}
     * and {@code b}. Returns an empty string if there is no common prefix.
     */
    public static String calculateLongestCommonPrefix(String a, String b) {
        for (int i = 0; i < Math.min(a.length(), b.length()); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return a.substring(0, i);
            }
        }
        return a;
    }
    
    /**
     * Returns the longest possible list of strings that is a prefix for both
     * {@code a} and {@code b}. Returns an empty list if there is no common prefix.
     */
    public static List<String> calculateLongestCommonPrefix(List<String> a, List<String> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            if (!a.get(i).equals(b.get(i))) {
                return ImmutableList.copyOf(a.subList(0, i));
            }
        }
        return ImmutableList.copyOf(a);
    }
    
    /**
     * Returns the longest possible list of strings that is a prefix for both
     * {@code a} and {@code b}. Returns an empty list if there is no common prefix.
     */
    public static List<String> calculateLongestCommonPrefix(String[] a, String[] b) {
        return calculateLongestCommonPrefix(ImmutableList.copyOf(a), ImmutableList.copyOf(b));
    }
    
    /**
     * Calculates the <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">
     * Levenshtein distance</a> between two strings.
     */
    public static int calculateLevenshteinDistance(String a, String b) {
        a = normalizeText(a);
        b = normalizeText(b);
        
        int[] cost = new int[a.length() + 1];
        int[] newCost = new int[a.length() + 1];
        for (int i = 0; i < cost.length; i++) {
            cost[i] = i;
        }
        
        for (int j = 1; j < b.length() + 1; j++) {
            newCost[0] = j;
            
            for (int i = 1; i < a.length() + 1; i++) {
                int match = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                int replaceCost = cost[i - 1] + match;
                int insertCost = cost[i] + 1;
                int deleteCost = newCost[i - 1] + 1;
                newCost[i] = Math.min(Math.min(insertCost, deleteCost), replaceCost);
            }
            
            int[] swap = cost;
            cost = newCost;
            newCost = swap;
        }
        
        return cost[a.length()];
    }
    
    private static String normalizeText(String text) {
        return TEXT_MATCHER.retainFrom(text).toLowerCase();
    }
    
    /**
     * Calculates the <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">
     * Levenshtein distance</a> between two strings, but returns the result relative
     * to the length of the longest input string. The returned value is therefore
     * a score between 0.0 (strings are equal) and 1.0 (strings are completely
     * different).
     */
    public static float calculateRelativeLevenshteinDistance(String a, String b) {
        int distance = calculateLevenshteinDistance(a, b);
        float relativeDistance = distance / (float) Math.max(a.length(), b.length());
        relativeDistance = Math.max(relativeDistance, 0f);
        relativeDistance = Math.min(relativeDistance, 1f);
        return relativeDistance;
    }
    
    /**
     * Returns all candidates that "fuzzy match" the specified string. Fuzzy matching
     * is done using the <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">
     * Levenshtein distance</a>, relative to the length of the string, against the
     * provided threshold.
     * @throws IllegalArgumentException if the threshold is outside the range between
     *         0.0 (strings must be equal) and 1.0 (any string is allowed).
     */
    public static List<String> fuzzyMatch(String str, Collection<String> candidates, float threshold) {
        Preconditions.checkArgument(threshold >= 0f && threshold <= 1f, 
                "Threshold is outside range 0.0 - 1.0: " + threshold);
        
        List<String> fuzzyMatches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate != null && isFuzzyMatch(str, candidate, threshold)) {
                fuzzyMatches.add(candidate);
            }
        }
        return fuzzyMatches;
    }

    private static boolean isFuzzyMatch(String str, String candidate, float threshold) {
        float distance = calculateRelativeLevenshteinDistance(str, candidate);
        return distance <= threshold;
    }
}
