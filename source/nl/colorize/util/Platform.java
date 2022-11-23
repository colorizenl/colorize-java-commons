//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TimeZone;

import static nl.colorize.util.PlatformFamily.LINUX;
import static nl.colorize.util.PlatformFamily.MAC;
import static nl.colorize.util.PlatformFamily.TEAVM;
import static nl.colorize.util.PlatformFamily.WINDOWS;

/**
 * Provides access to the underlying platform. This includes information such 
 * as the platform's name and the location for storing application data.
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
 * <p>
 * This class differentiates between platform <em>name</em> and platform
 * <em>family</em>. Examples of the former are Windows 11 or macOS Ventura,
 * while examples of the latter are simply "Windows" or "macOS". Application
 * behavior is typically more likely to be influenced by platform family than
 * by specific platform versions. See {@link PlatformFamily} for the list of
 * supported platform families. If a platform is not explicitly supported this
 * class can still be used, but platform conventions might not be followed.
 */
public final class Platform {

    private static TimeZone cachedDefaultTimeZone = null;

    protected static final String COLORIZE_TIMEZONE_ENV = "COLORIZE_TIMEZONE";

    private static final Map<String, PlatformFamily> OS_NAMES = Map.of(
        "windows", WINDOWS,
        "os x", MAC,
        "macos", MAC,
        "teavm", TEAVM,
        "linux", LINUX
    );

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
        .put("10.16", "Big Sur")
        // Big Sur is both 10.16 and 11.0
        .put("11.", "Big Sur")
        .put("12.", "Monterey")
        .put("13.", "Ventura")
        .build();
    
    private static final Version MIN_REQUIRED_JAVA_VERSION = Version.parse("17.0.0");
    private static final String AMSTERDAM_TIME_ZONE = "Europe/Amsterdam";

    private Platform() {
    }

    /**
     * Returns the display name for the current platform. Examples are "Windows
     * 11" or "macOS Ventura". As explained in the class documentation,
     * application logic is more likely to be influenced by platform family
     * than by specific platform versions and/or variants. Applications should
     * therefore prefer {@link #getPlatformFamily()} rather than parsing the
     * display name string returned by this method.
     * <p>
     * When running via TeaVM, this method will always return "TeaVM",
     * regardless of the underlying operating system and/or browser.
     */
    public static String getPlatformName() {
        PlatformFamily platformFamily = getPlatformFamily();

        return switch (platformFamily) {
            case WINDOWS -> System.getProperty("os.name", "Unknown");
            case MAC -> "macOS " + getMacVersionName();
            default -> platformFamily.toString();
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
     * Returns the {@link PlatformFamily} for the current platform. Applications
     * should prefer this method over {@link #getPlatformName()} for
     * platform-specific behavior, though the latter might be used if different
     * behavior for specific platform versions or variants is needed.
     * <p>
     * When running via TeaVM, this method will always return
     * {@link PlatformFamily#TEAVM}, regardless of the underlying operating
     * system and/or browser.
     */
    public static PlatformFamily getPlatformFamily() {
        String os = System.getProperty("os.name", "Unknown").toLowerCase();
        String vendor = System.getProperty("java.vendor", "Unknown").toLowerCase();

        if (vendor.contains("android")) {
            return PlatformFamily.ANDROID;
        } else if (System.getProperty("com.google.appengine.runtime.environment") != null) {
            return PlatformFamily.GOOGLE_CLOUD;
        }

        return OS_NAMES.keySet().stream()
            .filter(os::contains)
            .map(OS_NAMES::get)
            .findFirst()
            .orElse(PlatformFamily.UNKNOWN);
    }
    
    public static boolean isWindows() {
        return getPlatformFamily() == WINDOWS;
    }
    
    public static boolean isMac() {
        return getPlatformFamily() == MAC;
    }
    
    public static boolean isLinux() {
        return getPlatformFamily() == LINUX;
    }

    public static boolean isTeaVM() {
        return getPlatformFamily() == TEAVM;
    }

    /**
     * Returns true if the application is running inside of the Mac app sandbox.
     * When running inside of the sandbox access to system resources is limited 
     * to the entitlements specified when signing the app. Using the sandbox is
     * mandatory for apps distributed through the Mac App Store, but optional
     * for those distributed outside of the App Store with Developer ID.
     */
    public static boolean isMacAppSandboxEnabled() {
        //TOOD Developer ID applications can also use the sandbox, although
        //     this is not mandatory.
        return isMacAppStore();
    }
    
    /**
     * Returns true if the application was downloaded from the Mac App Store.
     * Note that App Store applications are always sandboxed (see
     * {@link #isMacAppSandboxEnabled()} for more information).
     */
    public static boolean isMacAppStore() {
        String sandboxContainer = System.getenv("APP_SANDBOX_CONTAINER_ID");
        return isMac() && sandboxContainer != null && !sandboxContainer.isEmpty();
    }
    
    /**
     * Returns the number of available processor cores. Note that this number
     * might change over time, for example because of energy saver features.
     */
    public static int getSystemProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }
    
    /**
     * Returns the system's processor architecture as described by the "os.arch"
     * system property.
     */
    public static String getSystemArchitecture() {
        return System.getProperty("os.arch");
    }
    
    /**
     * Returns the amount of heap memory that is currently being used by the JVM,
     * in bytes.
     */
    public static long getUsedHeapMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Returns the version of the Java Virtual Machine. An example is "1.7.0_55".
     * On some platforms it might not be possible to detect the JVM version, in
     * these cases this returns the minimum Java version supported by this class. 
     */
    public static Version getJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        // The system property is "" on Google App Engine and "0" on Android.
        if (javaVersion != null && javaVersion.length() >= 2 && Version.canParse(javaVersion)) {
            return Version.parse(javaVersion);
        } else {
            return MIN_REQUIRED_JAVA_VERSION;
        }
    }

    /**
     * Returns a {@code File} that points to the current working directory.
     *
     * @throws UnsupportedOperationException on platforms that do not have the
     *         concept of a working directory.
     */
    public static File getUserWorkingDirectory() {
        String workingDirectory = System.getProperty("user.dir");
        if (workingDirectory == null || workingDirectory.length() == 0) {
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
        } else if (isMacAppSandboxEnabled()) {
            return new File(System.getenv("HOME"));
        } else if (isMac()) {
            // These directory names are always in English, regardless
            // of the language of the user interface.
            File applicationSupport = new File(System.getenv("HOME") + "/Library/Application Support");
            return new File(applicationSupport, app);
        } else if (getPlatformFamily().hasLocalFileSystem()) {
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
        } else if (isMacAppSandboxEnabled()) {
            return new File(System.getenv("HOME"));
        } else if (isMac()) {
            // The "Documents" directory has the same name on non-English
            // versions of macOS, according to Apple's documentation.
            return new File(System.getenv("HOME") + "/Documents");
        } else if (getPlatformFamily().hasLocalFileSystem()) {
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
     * Returns the home directory of the Java runtime environment used to run
     * the application. In some environments this location is not available
     * due to security considerations, in these cases this method will return
     * {@code null}.
     */
    public static File getJavaHome() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty() || !new File(javaHome).exists()) {
            return null;
        }
        return new File(javaHome);
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
     * Returns the platform-specific character(s) used for newlines. This is 
     * "\r\n" on Windows and "\n" on nearly all other platforms.
     *
     * @deprecated This method is no longer necessary since
     *             {@link System#lineSeparator()} was introduced in Java 7.
     */
    @Deprecated
    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    /**
     * Returns the default timezone. By default, this will return the
     * {@code Europe/Amsterdam} time zone. The default time zone can be set
     * explicitly using the environment variable "COLORIZE_TIMEZONE". If the
     * requested time zone is not available on the current platform, the GMT
     * time zone is used instead.
     */
    public static TimeZone getDefaultTimeZone() {
        if (cachedDefaultTimeZone != null) {
            return cachedDefaultTimeZone;
        }

        String requestedTimeZone = System.getenv(COLORIZE_TIMEZONE_ENV);
        if (requestedTimeZone == null || requestedTimeZone.isEmpty()) {
            requestedTimeZone = AMSTERDAM_TIME_ZONE;
        }

        cachedDefaultTimeZone = TimeZone.getTimeZone(requestedTimeZone);
        return cachedDefaultTimeZone;
    }
}
