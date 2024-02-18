//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;

/**
 * Provides access to the underlying platform. This includes basic information
 * such as the platform's name, but also platform-specific behavior such as the
 * expected location for storing application data.
 * <p>
 * Most of this information can also be obtained from system properties, 
 * environment variables, or the {@link java.lang.System} class. However,
 * working directly with these properties is error-prone, since the properties
 * are scattered across various locations, need to be parsed by applications,
 * and do not always behave consistently across different platforms. In addition
 * to that, the standard APIs have not always been updated to reflect modern
 * best practices for each platform. For example, the {@code java.util.prefs}
 * API for storing application preferences was never updated for the introduction
 * of the Mac App Store in 2010, or Mac App Store sandboxing in 2014. This class
 * therefore provides a way to manage application data and preferences in a way
 * that is considered suitable and native for each platform.
 */
public enum Platform {

    WINDOWS("Windows", "windows"),
    MAC("macOS", "mac"),
    LINUX("Linux", "linux"),
    GOOGLE_CLOUD("Google Cloud", "googlecloud"),
    ANDROID("Android", "android"),
    IOS("iOS", "robovm"),
    TEAVM("TeaVM", "teavm"),
    UNKNOWN("Unknown", "unknown");

    private String displayName;
    private String osName;

    private static final Map<String, String> MAC_VERSION_NAMES = new ImmutableMap.Builder<String, String>()
        .put("10.4", "Tiger")
        .put("10.5", "Leopard")
        .put("10.6", "Snow Leopard")
        .put("10.7", "Lion")
        .put("10.8", "Mountain Lion")
        .put("10.9", "Mavericks")
        .put("10.10", "Yosemite")
        .put("10.11", "El Capitan")
        .put("10.12", "Sierra")
        .put("10.13", "High Sierra")
        .put("10.14", "Mojave")
        .put("10.15", "Catalina")
        .put("11.", "Big Sur")
        .put("12.", "Monterey")
        .put("13.", "Ventura")
        .put("14.", "Sonoma")
        .build();

    private static final String COLORIZE_TIMEZONE_ENV = "COLORIZE_TIMEZONE";
    private static final String AMSTERDAM_TIME_ZONE = "Europe/Amsterdam";

    private Platform(String displayName, String osName) {
        this.displayName = displayName;
        this.osName = osName;
    }

    /**
     * Returns the display name for this platform family. Examples are "macOS",
     * "Windows", and "Google Cloud". The platform family does not include the
     * platform version, use {@link #getPlatformName()} to obtain the complete
     * display name.
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Returns the current platform <em>family</em>. This does not return the
     * specific platform version, as most platform-specific behavior is based
     * on the platform family rather than the specific versions.
     * {@link #getPlatformName()} can be used to obtain the full platform
     * display name if version-specific behavior is required.
     * <p>
     * When running on TeaVM, this method will always return {@link #TEAVM},
     * regardless of the underlying operating system and/or browser.
     */
    public static Platform getPlatform() {
        String os = System.getProperty("os.name", "Unknown").toLowerCase();
        String vendor = System.getProperty("java.vendor", "Unknown").toLowerCase();

        if (vendor.contains("android")) {
            return ANDROID;
        } else if (System.getenv("GAE_APPLICATION") != null) {
            return GOOGLE_CLOUD;
        } else if (System.getProperty("com.google.appengine.runtime.environment") != null) {
            return GOOGLE_CLOUD;
        }

        return Arrays.stream(values())
            .filter(platform -> os.contains(platform.osName))
            .findFirst()
            .orElse(UNKNOWN);
    }

    /**
     * Returns the display name for the current platform, for example "Windows
     * 11" or "macOS Ventura". Platform-specific behavior is more likely to be
     * based on the platform family rather than the specific platform.
     * Applications should therefore prefer {@link #getPlatform()} instead of
     * parsing the display name string returned by this method.
     * <p>
     * When running via TeaVM, this method will always return "TeaVM",
     * regardless of the underlying operating system and/or browser.
     */
    public static String getPlatformName() {
        Platform platform = getPlatform();

        return switch (platform) {
            case WINDOWS -> System.getProperty("os.name", "Unknown");
            case MAC -> "macOS " + getMacVersionName();
            default -> platform.toString();
        };
    }

    private static String getMacVersionName() {
        String osVersion = System.getProperty("os.version");

        return MAC_VERSION_NAMES.keySet().stream()
            .filter(v -> osVersion.startsWith(v))
            .map(MAC_VERSION_NAMES::get)
            .findFirst()
            .orElse(osVersion);
    }

    /**
     * Convenience method that can be used to check the current platform.
     * For example, using {@code Platform.is(Platform.MAC)} is a shorthand
     * for {@code Platform.getPlatform().equals(Platform.MAC)}.
     */
    public static boolean is(Platform platformFamily) {
        return getPlatform().equals(platformFamily);
    }

    public static boolean isWindows() {
        return getPlatform() == WINDOWS;
    }
    
    public static boolean isMac() {
        return getPlatform() == MAC;
    }

    public static boolean isLinux() {
        return getPlatform() == LINUX;
    }

    public static boolean isGoogleCloud() {
        return getPlatform() == GOOGLE_CLOUD;
    }
    
    public static boolean isTeaVM() {
        return getPlatform() == TEAVM;
    }

    /**
     * Returns true if the current platform is running on a Mac and if the
     * Java Virtual Machine is running from within the Mac App Store sandbox.
     */
    public static boolean isMacAppStore() {
        String sandboxContainer = System.getenv("APP_SANDBOX_CONTAINER_ID");
        return isMac() && sandboxContainer != null && !sandboxContainer.isEmpty();
    }

    /**
     * Returns the system's processor architecture, as described by the
     * {@code os.arch} system property.
     */
    public static String getSystemArchitecture() {
        return System.getProperty("os.arch");
    }
    
    /**
     * Returns the amount of heap memory that is currently being used by the
     * Java Virtual Machine, in bytes.
     */
    public static long getUsedHeapMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Returns the version of the Java Virtual Machine. Examples of returned
     * version numbers are "1.7.0_55" and "21.0.1". Detection is based on the
     * value of the {@code java.version} system property. Returns
     * {@link Version#UNKNOWN} if the current platform does not provide Java
     * version information.
     */
    public static Version getJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        // The system property is "" on Google App Engine and "0" on Android.
        if (javaVersion == null || javaVersion.length() < 2 || !Version.canParse(javaVersion)) {
            return Version.UNKNOWN;
        }
        return Version.parse(javaVersion);
    }

    /**
     * Returns a {@code File} that points to the current working directory.
     *
     * @throws UnsupportedOperationException on platforms that do not have the
     *         concept of a working directory.
     */
    public static File getUserWorkingDirectory() {
        String workingDirectory = System.getProperty("user.dir");
        if (workingDirectory == null || workingDirectory.isEmpty()) {
            throw new UnsupportedOperationException("Platform does not support working directory");
        }
        return new File(workingDirectory);
    }
        
    /**
     * Returns the name of the current user.
     *
     * @throws UnsupportedOperationException on platforms that do not support
     *         user accounts. Examples are Android and Google App Engine.
     */
    public static String getUserAccount() {
        String userAccount = System.getProperty("user.name");
        if (userAccount == null || userAccount.isEmpty()) {
            throw new UnsupportedOperationException("Platform does not support user accounts");
        }
        return userAccount;
    }
    
    /**
     * Returns a {@code File} that points to the current user's home directory.
     *
     * @throws UnsupportedOperationException on platforms that do not support
     *         user accounts. Examples are Android and Google App Engine.
     */
    public static File getUserHomeDir() {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            throw new UnsupportedOperationException("Platform does not support user accounts");
        }
        return new File(userHome);
    }

    /**
     * Returns a {@code File} that points to the current user's desktop directory.
     * Note that this method is only available on desktop platforms, and that even
     * then some platforms might not allow applications to write directly to the
     * user's desktop.
     *
     * @throws UnsupportedOperationException if the user desktop is not available
     *         or not accessible.
     */
    public static File getUserDesktopDir() {
        if (Platform.isMacAppStore()) {
            throw new UnsupportedOperationException(
                "Mac App Store does not allow applications to write directly to desktop");
        }

        File home = Platform.getUserHomeDir();
        File desktop = new File(home, "Desktop");
        if (desktop.exists()) {
            return desktop;
        } else {
            return home;
        }
    }

    private static boolean hasWritableFileSystem() {
        Platform platform = getPlatform();
        return platform == WINDOWS || platform == MAC || platform == LINUX;
    }

    /**
     * Returns the platform's directory for storing application data, for the
     * application with the specified name.
     *
     * @throws IllegalArgumentException If the application name cannot be used
     *         as a directory name.
     * @throws UnsupportedOperationException if the platform does not allow
     *         application data (e.g. Google App Engine).
     */
    public static File getApplicationDataDirectory(String app) {
        if (isWindows()) {
            File applicationDataRoot = new File(System.getenv("APPDATA"));
            return new File(applicationDataRoot, app);
        } else if (isMacAppStore()) {
            return new File(System.getenv("HOME"));
        } else if (isMac()) {
            // These directory names are always in English, regardless
            // of the language of the user interface.
            File applicationSupport = new File(System.getenv("HOME") + "/Library/Application Support");
            return new File(applicationSupport, app);
        } else if (hasWritableFileSystem()) {
            return new File(getUserHomeDir(), "." + app);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns a {@code File} handle that points to a file inside the platform's
     * directory for storing application data, for the application with the
     * specified name. If the application data directory does not already exist
     * it will be created.
     *
     * @param app Name of the application for which to create the directory.
     *
     * @throws IllegalArgumentException If the application name cannot be used
     *         as a directory name, if the path is empty, or if an absolute path
     *         is used instead of a relative path.
     * @throws UnsupportedOperationException if the platform does not allow
     *         application data (e.g. Google App Engine).
     */
    public static File getApplicationData(String app, String path) {
        Preconditions.checkArgument(!path.trim().isEmpty() && !path.startsWith("/"),
            "Invalid path: " + path);

        File appDataDir = getApplicationDataDirectory(app);
        try {
            FileUtils.mkdir(appDataDir);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot create application data directory", e);
        }
        
        return new File(appDataDir.getAbsolutePath() + "/" + path);
    }

    /**
     * Returns the platform's standard directory for storing user data (e.g. "My
     * Documents" on Windows).
     *
     * @throws UnsupportedOperationException if the platform does not allow access
     *         to user files (e.g. Google App Engine).
     */
    public static File getUserDataDirectory() {
        if (isWindows()) {
            return getWindowsMyDocumentsDirectory();
        } else if (isMacAppStore()) {
            return new File(System.getenv("HOME"));
        } else if (isMac()) {
            // The "Documents" directory has the same name on non-English
            // versions of macOS, according to Apple's documentation.
            return new File(System.getenv("HOME") + "/Documents");
        } else if (hasWritableFileSystem()) {
            return getUserHomeDir();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static File getWindowsMyDocumentsDirectory() {
        try {
            return FileSystemView.getFileSystemView().getDefaultDirectory();
        } catch (Exception e) {
            return getUserHomeDir();
        }
    }

    /**
     * Returns a {@code File} handle that points to a file in the platform's 
     * standard directory for storing user data (e.g. "My Documents" on Windows).
     *
     * @throws IllegalArgumentException if the path is absolute. 
     * @throws UnsupportedOperationException if the platform does not allow access
     *         to user files (e.g. Google App Engine).
     */
    public static File getUserData(String path) {
        if (path.isEmpty() || path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        
        File dir = getUserDataDirectory();
        return new File(dir.getAbsolutePath() + "/" + path);
    }

    /**
     * Returns the platform's directory for temporary files.
     *
     * @throws UnsupportedOperationException if the platform has no writable
     *         file system. An example is Google App Engine.
     */
    public static File getTempDir() {
        String tempDirectory = System.getProperty("java.io.tmpdir");
        if (tempDirectory == null || tempDirectory.isEmpty()) {
            throw new UnsupportedOperationException("Platform does not support temp files");
        }
        return new File(tempDirectory);
    }

    /**
     * Returns the default timezone. By default, this will return the
     * {@code Europe/Amsterdam} time zone. The default time zone can be set
     * explicitly using the environment variable "COLORIZE_TIMEZONE". If the
     * requested time zone is not available on the current platform, the GMT
     * time zone is used instead.
     */
    public static TimeZone getDefaultTimeZone() {
        String requestedTimeZone = System.getenv(COLORIZE_TIMEZONE_ENV);

        if (requestedTimeZone == null || requestedTimeZone.isEmpty()) {
            requestedTimeZone = AMSTERDAM_TIME_ZONE;
        }

        return TimeZone.getTimeZone(requestedTimeZone);
    }
}
