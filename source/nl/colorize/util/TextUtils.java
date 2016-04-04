//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

/**
 * Miscellaneous utility and convenience versions for working with text.
 */
public final class TextUtils {

	private TextUtils() {
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
}
