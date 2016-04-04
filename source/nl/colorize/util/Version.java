//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.primitives.Ints;

/**
 * Represents a version number with arbitrary precision. Examples of version
 * numbers that can be represented by this class are "1.0" and "1.6.0b31".  
 */
public final class Version implements Comparable<Version>, Serializable {

	private String versionString;
	private int[] digits;
	
	private static final Pattern VERSION_STRING_PATTERN = Pattern.compile("(\\d[\\.\\d]*)(.*)");
	private static final long serialVersionUID = 9;
	
	private Version(String versionString, int[] digits) {
		this.versionString = versionString;
		this.digits = digits;
	}
	
	/**
	 * Returns the digit at the specified position. If no digit exists at that
	 * position this will return 0.
	 */
	public int getDigit(int position) {
		if (position >= digits.length) {
			return 0;
		}
		return digits[position];
	}
	
	/**
	 * Compares this version number to {@code other}. This will return one of
	 * the following values:
	 * 
	 * <ul>
	 *   <li>1 if this version is newer than {@code other}
	 *   <li>-1 if this version is older than {@code other}
	 *   <li>0 if both versions are equal
	 * </ul>
	 */
	public int compareTo(Version other) {
		int precision = Math.max(digits.length, other.digits.length);
		return compareTo(other, precision);
	}
	
	/**
	 * Compares this version number to {@code other}, considering only the first
	 * N digits. For example, comparing "1.0.0" and "1.0.2" will return 1 with
	 * a precision of 3 digits, but 0 with a precision of 2 digits.
	 * @throws IllegalArgumentException if {@code precision} is 0 or less.
	 */
	public int compareTo(Version other, int precision) {
		if (precision <= 0) {
			throw new IllegalArgumentException("Invalid precision: " + precision);
		}
		
		for (int i = 0; i < precision; i++) {
			int thisDigit = getDigit(i);
			int otherDigit = other.getDigit(i);
			
			if (thisDigit > otherDigit) {
				return 1;
			} else if (thisDigit < otherDigit) {
				return -1;
			}
		}
		
		return 0;
	}
	
	/**
	 * Returns true if this version is equal to or newer than {@code other}.
	 */
	public boolean isAtLeast(Version other) {
		return compareTo(other) >= 0;
	}
	
	public boolean isNewerThan(Version other) {
		return compareTo(other) >= 1;
	}
	
	public boolean isOlderThan(Version other) {
		return compareTo(other) <= -1;
	}
	
	/**
	 * Returns a new {@code Version} instance that is a truncated version of this
	 * one. For example, truncating the version number 1.2.3 to 2 digits will
	 * return 1.2. Truncating a version number that contains textual suffixes will
	 * remove those suffixes.
	 * @throws IllegalArgumentException if there is not at least 1 digit left.
	 */
	public Version truncate(int maxDigits) {
		if (maxDigits < 1) {
			throw new IllegalArgumentException("Must keep at least 1 digit");
		}
		
		int[] truncatedDigits = new int[Math.min(maxDigits, digits.length)];
		for (int i = 0; i < truncatedDigits.length; i++) {
			truncatedDigits[i] = digits[i];
		}
		
		return parse(Joiner.on(".").join(Ints.asList(truncatedDigits)));
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Version) {
			Version other = (Version) o;
			return compareTo(other) == 0;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return versionString.hashCode();
	}
	
	/**
	 * Returns the version string that is represented by this object.
	 */
	@Override
	public String toString() {
		return versionString;
	}
	
	/**
	 * Parses a version number from the specified version string.
	 * @throws IllegalArgumentException if the version string cannot be parsed.
	 */
	public static Version parse(String versionString) {
		if (versionString == null) {
			throw new IllegalArgumentException("Version string is empty");
		}
		
		Matcher matcher = VERSION_STRING_PATTERN.matcher(versionString);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Cannot parse version string: " + versionString);
		}
		
		String[] parts = matcher.group(1).split("\\.");
		int[] digits = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			digits[i] = Integer.parseInt(parts[i]);
		}
		
		return new Version(versionString, digits); 
	}
	
	/**
	 * Returns true if {@code versionString} represents a version number that
	 * can be represented by this class. In other words, this method indicates
	 * whether {@link #parse(String)} will throw an exception if invoked with
	 * {@code versionString}.
	 */
	public static boolean canParse(String versionString) {
		if (versionString == null || versionString.isEmpty()) {
			return false;
		}
		
		Matcher matcher = VERSION_STRING_PATTERN.matcher(versionString);
		return matcher.matches();
	}
}
