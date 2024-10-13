//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a version number with arbitrary precision. Examples of version
 * numbers that can be represented by this class are "1.0" and "1.6.0b31".  
 */
public final class Version implements Comparable<Version> {

    private String versionString;
    private List<Integer> digits;

    public static final Version UNKNOWN = new Version("0.0.0", List.of(0, 0, 0));
    
    private static final Pattern VERSION_STRING_PATTERN = Pattern.compile("v?(\\d[\\.\\d]*)(.*)");
    private static final Splitter VERSION_SPLITTER = Splitter.on(".").omitEmptyStrings();
    private static final Joiner VERSION_JOINER = Joiner.on(".");

    private Version(String versionString, List<Integer> digits) {
        this.versionString = versionString;
        this.digits = List.copyOf(digits);
    }

    /**
     * Returns the digit at the specified position. If no digit exists at that
     * position this will return 0.
     */
    protected int getDigit(int position) {
        if (position >= digits.size()) {
            return 0;
        }
        return digits.get(position);
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
    @Override
    public int compareTo(Version other) {
        int precision = Math.max(digits.size(), other.digits.size());
        return compareTo(other, precision);
    }
    
    /**
     * Compares this version number to {@code other}, considering only the first
     * N digits. For example, comparing "1.0.0" and "1.0.2" will return 1 with
     * a precision of 3 digits, but 0 with a precision of 2 digits.
     *
     * @throws IllegalArgumentException if {@code precision} is 0 or less.
     */
    public int compareTo(Version other, int precision) {
        Preconditions.checkArgument(precision > 0, "Invalid precision: " + precision);

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
        Preconditions.checkArgument(maxDigits >= 1, "Must keep at least 1 digit");

        List<Integer> truncatedDigits = digits.stream()
            .limit(Math.min(maxDigits, digits.size()))
            .toList();

        return new Version(VERSION_JOINER.join(truncatedDigits), truncatedDigits);
    }

    public boolean isUnknown() {
        return equals(UNKNOWN);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Version other) {
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
        if (equals(UNKNOWN)) {
            return "UNKNOWN";
        }
        return versionString;
    }
    
    /**
     * Parses a version number from the specified version string.
     *
     * @throws IllegalArgumentException if the version string cannot be parsed.
     */
    public static Version parse(String versionString) {
        if (versionString.isEmpty() || versionString.equalsIgnoreCase("UNKNOWN")) {
            return UNKNOWN;
        }

        Matcher matcher = VERSION_STRING_PATTERN.matcher(versionString);

        Preconditions.checkArgument(matcher.matches(), "Cannot parse version: " + versionString);

        List<Integer> digits = VERSION_SPLITTER.splitToList(matcher.group(1)).stream()
            .map(Integer::parseInt)
            .toList();

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
