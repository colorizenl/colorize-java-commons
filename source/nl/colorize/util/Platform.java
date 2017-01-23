//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;

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
 *   <li>Android (mobile)</li>
 * </ul>
 * <p>
 * Note that the above list refers to platform <em>families</em>, not individual
 * platform versions. For example, "Windows" includes support from Windows XP to
 * Windows Server 2008 to Windows 10.
 * <p>
 * If a platform is not explicitly supported this class can still be used, but
 * the platform's conventions might not be followed.
 * <p>
 * Support for additional platforms can be added by applications. A new platform
 * can be registered by calling {@link #register(String, Platform)}. Be aware
 * that any calls made <em>before</em> the new platform is registered will not
 * actually make use of this support. In other words: make sure you call this
 * methods before using any functionality from this class.
 */
public abstract class Platform {
	
	private static Map<String, Platform> supportedPlatforms = new ConcurrentHashMap<>();
	static {
		supportedPlatforms.put("Windows", new WindowsPlatform());
		supportedPlatforms.put("macOS", new MacPlatform());
		supportedPlatforms.put("OS X", new MacPlatform());
		supportedPlatforms.put("Linux", new LinuxPlatform());
		supportedPlatforms.put("Google App Engine", new GoogleCloudPlatform());
	}
	
	private static final Map<String, String> MACOS_VERSION_NAMES = new ImmutableMap.Builder<String, String>()
			.put("10.4", "Tiger")
			.put("10.5", "Leopard")
			.put("10.6", "Snow Leopard")
			.put("10.7", "Lion")
			.put("10.8", "Mountain Lion")
			.put("10.9", "Mavericks")
			.put("10.10", "Yosemite")
			.put("10.11", "El Capitan")
			.put("10.12", "Sierra")
			.build();
	
	private static final Version MIN_REQUIRED_JAVA_VERSION = Version.parse("1.6.0");
	private static final Version UNKNOWN_ANDROID_VERSION = Version.parse("0.0");
	private static final Logger LOGGER = LogHelper.getLogger(Platform.class);
	
	/**
	 * Creates a new platform implementation. Applications should use the static
	 * methods in this class to access platform-specific information, this
	 * constructor only exists to create subclasses to support additional
	 * platforms. 
	 */
	protected Platform() {
	}
	
	/**
	 * Opens the resource file located at the specified path.
	 * @throws IOException if the file cannot be located.
	 */
	protected InputStream openResourceFile(String path) throws IOException {
		InputStream inClassPath = Platform.class.getClassLoader().getResourceAsStream(path);
		if (inClassPath != null) {
			return inClassPath;
		}
		
		File inFileSystem = new File(path);
		if (inFileSystem.exists() && !inFileSystem.isDirectory()) {
			return new FileInputStream(inFileSystem);
		}
		
		throw new FileNotFoundException("Resource file not found: " + path);
	}
	
	/**
	 * Returns the platform's directory for storing application data, for the
	 * application with the specified name.
	 * @throws IllegalArgumentException If the application name cannot be used
	 *         as a directory name.
	 * @throws UnsupportedOperationException if the platform does not allow
	 *         application data (e.g. Google App Engine).
	 */
	protected abstract File getApplicationDataDirectory(String app);
	
	/**
	 * Returns the platform's standard directory for storing user data (e.g. "My
	 * Documents" on Windows). 
	 * @throws UnsupportedOperationException if the platform does not allow access
	 *         to user files (e.g. Google App Engine).
	 */
	protected abstract File getUserDataDirectory();
	
	/**
	 * Registers support for a platform. The specified implementation will be
	 * used if either the platform name (see {@link #getPlatformName()} or
	 * the platform family (see {@link #getPlatformFamily()} matches the value
	 * from {@code platformName}. The platform name is checked before the
	 * platform family, so it is possible to have "generic" support for a
	 * platform family and then override that for a particular version.
	 */
	public static void register(String platform, Platform impl) {
		if (supportedPlatforms.containsKey(platform)) {
			LOGGER.warning("Replacing platform support for " + platform);
		}
		supportedPlatforms.put(platform, impl);
	}
	
	/**
	 * Returns the implementation that should be used for the current platform.
	 * This will first check for the specific platform version, then for the
	 * platform family. If the current platform is not explicitly supported a
	 * reasonable default implementation will be used.
	 */
	public static Platform getCurrentPlatform() {
		Platform impl = supportedPlatforms.get(getPlatformName());
		if (impl == null) {
			impl = supportedPlatforms.get(getPlatformFamily());
			if (impl == null) {
				// The "default implementation" assumes a Linux-based
				// platform. In reality almost all non-desktop platforms
				// except Windows use basically the same conventions.
				LOGGER.warning("No explicit support for platform " + getPlatformName() + 
						", using default support");
				impl = new LinuxPlatform();
			}
		}
		return impl;
	}
	
	/**
	 * Returns the platform's human-readable name, such as "Windows 10" or
	 * "macOS Sierra". Note this returns the <em>full</em> platform name, use
	 * {@link #getPlatformFamily()} for the platform family name (e.g. "Windows"
	 * or "macOS").
	 */
	public static String getPlatformName() {	
		String os = System.getProperty("os.name");
		String vendor = System.getProperty("java.vendor");
		
		String platformName = os;
		// Handle cases where the os.name system property does not return
		// the commonly used name of the OS. For example, on Android the
		// value of this system property is "Linux", which is technically
		// correct but not very useful.
		if (vendor.toLowerCase().contains("android")) {
			platformName = "Android " + getAndroidVersion();
		} else if (System.getProperty("com.google.appengine.runtime.version") != null) {
			platformName = "Google App Engine";
		} else if (os.toLowerCase().contains("os x") || os.toLowerCase().contains("macos")) {
			platformName = "macOS " + getMacOSVersionName();
		}
		
		return platformName;
	}
	
	private static String getMacOSVersionName() {
		String version = System.getProperty("os.version");
		for (Map.Entry<String, String> entry : MACOS_VERSION_NAMES.entrySet()) {
			if (version.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		LOGGER.warning("Unknown macOS version: " + version);
		return version;
	}
	
	private static Version getAndroidVersion() {
		try {
			Class<?> buildClass = Class.forName("android.os.Build");
			for (Class<?> innerClass : buildClass.getDeclaredClasses()) {
				if (innerClass.getName().endsWith("VERSION")) {
					String release = (String) innerClass.getField("RELEASE").get(null);
					return Version.parse(release).truncate(2);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Exception while attempting to determine Android version", e);
		}
		LOGGER.warning("Cannot determine Android version");
		return UNKNOWN_ANDROID_VERSION;
	}
	
	/**
	 * Returns the platform's "family" name, such as "Windows" or "macOS". Note
	 * this family name does not include the specific version of the platform
	 * (e.g. Windows XP, macOS Sierra), for that use {@link #getPlatformName()}. 
	 * If the platform is not part of any known platform family this method
	 * will return the platform name instead.
	 */
	public static String getPlatformFamily() {
		if (isWindows()) return "Windows";
		if (isMac()) return "macOS";
		if (isLinux()) return "Linux";
		if (isGoogleAppEngine()) return "Google App Engine";
		if (isAndroid()) return "Android";
		return getPlatformName();
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
	
	public static boolean isGoogleAppEngine() {
		return getPlatformName().startsWith("Google App Engine");
	}
	
	public static boolean isAndroid() {
		return getPlatformName().startsWith("Android");
	}
	
	/**
	 * Returns true if the application is running in Java Web Start. 
	 * @deprecated Java Web Start is no longer supported by this library. As
	 *             support by modern browsers is also far from guaranteed it
	 *             is recommended to no use Web Start as a deployment method.
	 */
	@Deprecated
	public static boolean isWebstartEnabled() {
		String webstartVersion = System.getProperty("javawebstart.version");
		return webstartVersion != null && !webstartVersion.isEmpty();
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
		//     this is not mandatory. AppBundler sets the SandboxEnabled
		//     system property, but this does not appear to work for Developer
		//     ID applications.
		return isMacAppStore();
	}
	
	/**
	 * Returns true if the application was downloaded from the Mac App Store.
	 * Note that App Store applications are always sandboxed (see
	 * {@link isMacAppSandboxEnabled()} for more information).
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
	 * @deprecated Applications should not depend on processor architecture.
	 */
	@Deprecated
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
			LOGGER.warning("Cannot determine Java version");
			return MIN_REQUIRED_JAVA_VERSION;
		}
	}
	
	private static Version getAndroidJavaVersion() {
		Version androidVersion = getAndroidVersion();
		Version froyo = Version.parse("2.2"); // API level 8
		Version kitkat = Version.parse("4.4"); // API level 19
		Version nougat = Version.parse("7.0"); // API level 24
		
		if (androidVersion.isAtLeast(nougat)) {
			return Version.parse("1.8.0-android");
		} else if (androidVersion.isAtLeast(kitkat)) {
			return Version.parse("1.7.0-android");
		} else if (androidVersion.isAtLeast(froyo)) {
			return Version.parse("1.6.0-android");
		} else {
			return Version.parse("1.5.0-android");
		}
	}
	
	/**
	 * Returns a {@code File} that points to the current working directory.
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
	 * Returns a {@code File} handle that points to a file inside the the platform's
	 * directory for storing application data, for the application with the specified
	 * name. If the application data directory does not already exist it will be
	 * created.
	 * @param app Name of the application for which to create the directory.
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
		
		File appDataDir = getCurrentPlatform().getApplicationDataDirectory(app);
		try {
			LoadUtils.mkdir(appDataDir);
		} catch (IOException e) {
			throw new UnsupportedOperationException("Cannot create application data directory", e);
		}
		
		return new File(appDataDir.getAbsolutePath() + "/" + path);
	}
	
	/**
	 * Returns the platform's standard directory for storing user data (e.g. "My
	 * Documents" on Windows). This method is identical to 
	 * {@link #getUserDataDirectory()} but allows for static access.
	 * @throws UnsupportedOperationException if the platform does not allow 
	 *         access to user files (e.g. Google App Engine).
	 */
	public static File getUserDataDir() {
		return getCurrentPlatform().getUserDataDirectory();
	}
	
	/**
	 * Returns a {@code File} handle that points to a file in the platform's 
	 * standard directory for storing user data (e.g. "My Documents" on Windows).
	 * @throws IllegalArgumentException if the path is absolute. 
	 * @throws UnsupportedOperationException if the platform does not allow access
	 *         to user files (e.g. Google App Engine).
	 */
	public static File getUserData(String path) {
		if (path.isEmpty() || path.startsWith("/")) {
			throw new IllegalArgumentException("Invalid path: " + path);
		}
		
		File dir = getCurrentPlatform().getUserDataDirectory();	
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
	 */
	public static String getLineSeparator() {
		return System.lineSeparator();
	}
	
	/**
	 * Support for Windows, including all desktop and server versions from
	 * Windows XP to Windows 10, but not the mobile version of Windows 10.
	 */
	private static class WindowsPlatform extends Platform {
		
		@Override
		protected File getApplicationDataDirectory(String app) {
			File applicationDataRoot = new File(System.getenv("APPDATA"));
			return new File(applicationDataRoot, app);
		}
		
		@Override
		protected File getUserDataDirectory() {
			// There is no API for this other than using the Swing file dialog. 
			// This has to be done through reflection since this class is also 
			// used on Android and Google App Engine.
			try {
				Class<?> fileSystemViewClass = Class.forName("javax.swing.filechooser.FileSystemView");
				Object fileSystemView = fileSystemViewClass.getMethod("getFileSystemView").invoke(null);
				return (File) fileSystemView.getClass().getMethod("getDefaultDirectory").invoke(fileSystemView);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Attempt to locate My Documents failed", e);
				return getUserHomeDir();
			}
		}
	}
	
	/**
	 * Support for all versions of macOS (formerly known as OS X). Behavior
	 * will be different depending on how the application is distributed
	 * (non-signed application, Developer ID, Mac App Store). 
	 */
	private static class MacPlatform extends Platform {
		
		@Override
		protected File getApplicationDataDirectory(String app) {
			if (isMacAppSandboxEnabled()) {
				File sandboxContainer = new File(System.getenv("HOME"));
				return sandboxContainer;				
			} else {
				// These directory names are always in English, regardless
				// of the language of the user interface.
				File applicationSupport = new File(System.getenv("HOME") + "/Library/Application Support");
				return new File(applicationSupport, app);
			}
		}
		
		@Override
		protected File getUserDataDirectory() {
			if (isMacAppSandboxEnabled()) {
				File sandboxContainer = new File(System.getenv("HOME"));
				return sandboxContainer;
			} else {
				// The "Documents" directory has the same name on non-English 
				// versions of macOS, according to Apple's documentation.
				return new File(System.getenv("HOME") + "/Documents");
			}
		}
	}
	
	/**
	 * Support for Linux. Because Linux is used as the foundation for a wide
	 * variety of desktop, server, cloud, and mobile operating systems, this
	 * class can only assume readonable default behavior. Subclasses should
	 * be created to respect the conventions of specific platforms.  
	 */
	private static class LinuxPlatform extends Platform {
		
		@Override
		protected File getApplicationDataDirectory(String app) {
			return new File(getUserHomeDir(), "." + app);
		}
		
		@Override
		protected File getUserDataDirectory() {
			return getUserHomeDir();
		}
	}
	
	/**
	 * Support for Google Cloud Platform (including Google App Engine).
	 */
	private static class GoogleCloudPlatform extends LinuxPlatform {
		
		@Override
		protected File getApplicationDataDirectory(String app) {
			throw new UnsupportedOperationException();
		}
		
		@Override
		protected File getUserDataDirectory() {
			throw new UnsupportedOperationException();
		}
	}
}
