//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * Lists supported platform families. Examples of such a "family" are Windows
 * or macOS, rather than specific platform <em>versions</em> such as Windows
 * 11 or macOS Ventura. Used in conjunction with {@link Platform}.
 */
public enum PlatformFamily {

    WINDOWS("Windows"),
    MAC("macOS"),
    LINUX("Linux"),
    GOOGLE_CLOUD("Google Cloud"),
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
        return this == GOOGLE_CLOUD;
    }

    public boolean isMobile() {
        return this == IOS || this == ANDROID;
    }

    public boolean isBrowser() {
        return this == TEAVM;
    }

    public boolean hasLocalFileSystem() {
        return !isCloud() && !isBrowser();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
