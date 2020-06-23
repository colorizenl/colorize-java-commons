//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * Enumerates all platform families that are supported by this library.
 * The term "family" generally refers to multiple versions or distributions
 * of an operating system that are (mostly) compatible with each other. So,
 * Windows 7 and Windows 10 are both considered Windows, and Ubuntu and
 * Debian are both considered Linux.
 */
public enum PlatformFamily {

    WINDOWS("Windows"),
    MAC("macOS"),
    LINUX("Linux"),
    GOOGLE_CLOUD("Google Cloud"),
    AWS("AWS"),
    ANDROID("Android"),
    IOS("iOS"),
    TEAVM("TeaVM"),
    UNKNOWN("Unknown");

    private String displayName;

    private PlatformFamily(String displayName) {
        this.displayName = displayName;
    }

    public boolean isDesktop() {
        return this == WINDOWS || this == MAC || this == LINUX;
    }

    public boolean isCloud() {
        return this == AWS || this == GOOGLE_CLOUD;
    }

    public boolean isMobile() {
        return this == IOS || this == ANDROID;
    }

    public boolean isBrowser() {
        return this == TEAVM;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
