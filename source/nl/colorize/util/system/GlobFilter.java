//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.system;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filename filter that accepts paths matching a glob pattern. This class follows
 * the convention described in the
 * <a href="http://en.wikipedia.org/wiki/Glob_(programming)">Wikipedia article</a>.
 * The glob pattern is matched to the file's name, not to the absolute or relative
 * path of the file. For example, the pattern "*abc*" will match "abc.txt" but not
 * "abc/def.txt" (since the file name is "def.txt").
 * <p>
 * Instances of this class is both immutable and thread-safe.
 * <p>
 * See {@link com.google.common.io.PatternFilenameFilter} for a file filter that
 * uses regular expressions to match files.
 */
public final class GlobFilter implements FilenameFilter {

	private String globPattern;
	private Pattern regex;
	
	/**
	 * Creates a filter that uses the specified glob pattern and is case-insensitive.
	 */
	public GlobFilter(String globPattern) {
		this(globPattern, false);
	}
	
	/**
	 * Creates a filter that uses the specified glob pattern.
	 */
	public GlobFilter(String globPattern, boolean caseSensitive) {
		this.globPattern = globPattern;
		this.regex = toRegularExpression(caseSensitive);
	}
	
	private Pattern toRegularExpression(boolean caseSensitive) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\\Q");
		for (int i = 0; i < globPattern.length(); i++) {
			char c = globPattern.charAt(i);
			switch (c) {
				case '\\' : buffer.append(globPattern.charAt(++i)); break;
				case '?' : buffer.append("\\E.\\Q"); break;
				case '*' : buffer.append("\\E.*?\\Q"); break;
				case '[' : buffer.append("\\E["); break;
				case ']' : buffer.append("]\\Q"); break;
				default : buffer.append(c); break;
			}
		}
		buffer.append("\\E");
		
		// Strip off empty escape sequences
		String pattern = buffer.toString().replace("\\Q\\E", "");
		int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
		return Pattern.compile(pattern, flags);
	}

	public boolean accept(File dir, String name) {
		Matcher matcher = regex.matcher(name);
		return matcher.matches();
	}

	@Override
	public String toString() {
		return globPattern;
	}
}
