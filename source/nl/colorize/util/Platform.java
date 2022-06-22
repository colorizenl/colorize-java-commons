//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableMap;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Provides access to the underlying platform. This includes information such 
 * as the platform's name and the location for storing application data.
 * <p>
 * Most of this information can also be obtained from system properties, 
 * environment variables, or the {@link java.lang.System} class. However,
 * working directly with these properties is error prone, since the properties
 * are scattered across various locations, need to be parsed by applications,
 * and do not always behave consistently across different platforms.In addition
 * to that, the standard APIs have not always been updated to reflect modern
 * best practices for each platform. For example, the {@code java.util.prefs}
 * API for storing application preferences was never updated for the introduction
 * of the Mac App Store in 2010, or Mac App Store sandboxing in 2014. This class 
 * therefore provides methods for storing application data, user data, and
 * application preferences in the recommended location for each platform.
 * <p>
 * By default, this class supports a number of desktop, server, cloud, and mobile
 * platforms:
 * <ul>
 *   <li>Windows (desktop, server)</li>
 *   <li>macOS (desktop, formerly known as OS X)</li>
 *   <li>Linux (desktop, server)</li>
 *   <li>Google Cloud Platform (cloud)</li>
 *   <li>AWS (cloud)</li>
 *   <li>Android (mobile)</li>
 *   <li>iOS (mobile, via RoboVM)</li>
 *   <li>TeaVM (browser)</li>
 * </ul>
 * <p>
 * Note that the above list refers to platform <em>families</em>, not individual
 * platform versions. For example, "Windows" includes support from Windows XP to
 * Windows Server 2008 to Windows 10. Use {@link #getPlatformName()} to obtain
 * the platform's display name including the version number, and use
 * {@link #getPlatformFamily()} to obtain the platform family.
 * <p>
 * If a platform is not explicitly supported this class can still be used, but
 * the platform's conventions might not be followed.
 */
public final class Platform {

    private static AtomicBoolean teaVM = new AtomicBoolean(false);

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
        // Big Sur is both 10.16 and 11.0
        .put("10.16", "Big Sur")
        .put("11.", "Big Sur")
        .put("12.", "Monterey")
        .put("13.", "Ventura")
        .build();
    
    private static final Version MIN_REQUIRED_JAVA_VERSION = Version.parse("17.0.0");
    private static final Version UNKNOWN_ANDROID_VERSION = Version.parse("0.0");

    private Platform() {
    }

    /**
     * Returns the platform's human-readable name, such as "Windows 10" or
     * "macOS Catalina". For platform-specific logic, it is more reliable to
     * use {@link #getPlatformFamily()} rather than parsing the string
     * returned by this method.
     */
    public static String getPlatformName() {    
        String os = System.getProperty("os.name", "Unknown");
        String vendor = System.getProperty("java.vendor", "Unknown");
        
        // Handle cases where the os.name system property does not return
        // the commonly used name of the OS. For example, on Android the
        // value of this system property is "Linux", which is technically
        // correct but not very useful.
        if (vendor.toLowerCase().contains("android")) {
            return "Android " + getAndroidVersion();
        } else if (isGoogleCloud()) {
            return "Google Cloud";
        } else if (os.toLowerCase().contains("os x") || os.toLowerCase().contains("macos")) {
            return "macOS " + getMacOSVersionName();
        } else if (isAWS()) {
            return "AWS";
        } else if (isTeaVM()) {
            return "TeaVM";
        } else {
            return os;
        }
    }
    
    private static String getMacOSVersionName() {
        String version = System.getProperty("os.version");
        for (Map.Entry<String, String> entry : MAC_VERSION_NAMES.entrySet()) {
            if (version.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return version;
    }
    
    private static Version getAndroidVersion() {
        try {
            Class<?> buildVersionClass = Class.forName("android.os.Build$VERSION");
            String release = (String) buildVersionClass.getField("RELEASE").get(null);
            return Version.parse(release).truncate(2);
        } catch (Exception e) {
            return UNKNOWN_ANDROID_VERSION;
        }
    }
    
    /**
     * Returns the current platform family. Use {@link #getPlatformName()} for
     * obtaining the platform's display name as a string, including the exact
     * platform version name. Refer to the {@link PlatformFamily} documentation
     * for more information on what is considered a "family".
     */
    public static PlatformFamily getPlatformFamily() {
        Map<PlatformFamily, Supplier<Boolean>> mapper =
                new ImmutableMap.Builder<PlatformFamily, Supplier<Boolean>>()
            .put(PlatformFamily.WINDOWS, Platform::isWindows)
            .put(PlatformFamily.MAC, Platform::isMac)
            .put(PlatformFamily.LINUX, Platform::isLinux)
            .put(PlatformFamily.GOOGLE_CLOUD, Platform::isGoogleCloud)
            .put(PlatformFamily.AWS, Platform::isAWS)
            .put(PlatformFamily.ANDROID, Platform::isAndroid)
            .put(PlatformFamily.TEAVM, Platform::isTeaVM)
            .build();

        return mapper.keySet().stream()
            .filter(family -> mapper.get(family).get())
            .findFirst()
            .orElse(PlatformFamily.UNKNOWN);
    }
    
    public static boolean isWindows() {
        return getPlatformName().startsWith("Windows");
    }
    
    public static boolean isMac() {
        return getPlatformName().startsWith("macOS");
    }
    
    public static boolean isLinux() {
        return getPlatformName().startsWith("Linux");
    }

    public static boolean isGoogleCloud() {
        return System.getenv("GAE_APPLICATION") != null ||
            System.getProperty("com.google.appengine.runtime.version") != null;
    }

    /**
     * @deprecated Google App Engine is no longer a separate SDK and has
     *             been integrated into the Google Cloud SDK.
     *             Use {@link #isGoogleCloud()} instead.
     */
    @Deprecated
    public static boolean isGoogleAppEngine() {
        return isGoogleCloud();
    }

    public static boolean isAWS() {
        try {
            Class.forName("software.amazon.awssdk.services.ec2.Ec2Client");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAndroid() {
        return getPlatformName().startsWith("Android");
    }

    private static boolean isLimitedPlatform() {
        return isGoogleCloud() || isAWS() || isAndroid();
    }

    /**
     * Marks the current platform as running in the browser via TeaVM. Unlike
     * other platforms, it is not possible to detect this automatically, so
     * applications must call this method manually in their {@code main}
     * method in order to enable TeaVM support.
     */
    public static void enableTeaVM() {
        teaVM.set(true);
    }

    public static boolean isTeaVM() {
        return teaVM.get();
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
        } else if (isAndroid()) {
            return getAndroidJavaVersion();
        } else {
            return MIN_REQUIRED_JAVA_VERSION;
        }
    }
    
    private static Version getAndroidJavaVersion() {
        Version androidVersion = getAndroidVersion();
        Version nougat = Version.parse("7.0"); // API level 24
        
        if (androidVersion.isAtLeast(nougat)) {
            return Version.parse("1.8.0-android");
        } else {
            return Version.parse("1.7.0-android");
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
        } else if (isLimitedPlatform()) {
            throw new UnsupportedOperationException();
        } else {
            return new File(getUserHomeDir(), "." + app);
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
        if (path.trim().isEmpty() || path.startsWith("/")) {
            throw new IllegalArgumentException("Invalid path: " + path);
        }
        
        File appDataDir = getApplicationDataDirectory(app);
        try {
            FileUtils.mkdir(appDataDir);
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot create application data directory", e);
        }
        
        return new File(appDataDir.getAbsolutePath() + "/" + path);
    }

    /**
     * Loads application data that is stored in a {@code .properties} file within
     * the platform's directory for storing application data, for the application
     * with the specified name. If the application data directory does not already
     * exist it will be created.
     *
     * @param app Name of the application for which to create the directory.
     *
     * @throws IOException if the file does not exist or cannot be parsed.
     * @throws IllegalArgumentException If the application name cannot be used
     *         as a directory name, if the path is empty, or if an absolute path
     *         is used instead of a relative path.
     * @throws UnsupportedOperationException if the platform does not allow
     *         application data (e.g. Google App Engine).
     */
    public static ApplicationData loadApplicationData(String app, String path) throws IOException {
        File file = getApplicationData(app, path);
        return new ApplicationData(file);
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
        } else if (isLimitedPlatform()) {
            throw new UnsupportedOperationException();
        } else {
            return getUserHomeDir();
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
}
