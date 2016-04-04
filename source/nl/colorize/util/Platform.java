//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

/**
 * Provides access to platform-specific information. This information includes
 * the platform's name, family, and the version of the JVM. This information can
 * also be obtained from system properties, but manually parsing these properties 
 * is mistake-prone, and does not take inconsistenties between platforms into account 
 * (for example, the {@code java.version} system property is always "0" on Android). 
 * <p>
 * Different platforms also have different conventions for storing application
 * data and user data. The {@code java.util.prefs} API can be used to store and 
 * read user preferences, but this does not cover all types of application data
 * (such as storing files). This class therefore provides methods for storing
 * application data in the appropriate location for the current platform.
 * <p>
 * {@code PlatformAccessProvider}s can be registered and are used as an extension
 * mechanism to introduce or improve support for additional platforms. 
 */
public final class Platform {
	
	private static List<PlatformAccessProvider> accessProviders = Lists.newCopyOnWriteArrayList(
			ImmutableList.<PlatformAccessProvider>of(new StandardPlatformAccessProvider()));
	
	private static final Map<String, String> OS_X_VERSION_NAMES = new ImmutableMap.Builder<String, String>()
			.put("10.4", "Tiger")
			.put("10.5", "Leopard")
			.put("10.6", "Snow Leopard")
			.put("10.7", "Lion")
			.put("10.8", "Mountain Lion")
			.put("10.9", "Mavericks")
			.put("10.10", "Yosemite")
			.put("10.11", "El Capitan")
			.build();
	
	private static final Version MIN_REQUIRED_JAVA_VERSION = Version.parse("1.6.0");
	private static final Version UNKNOWN_ANDROID_VERSION = Version.parse("0.0");
	private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile(
			"\\s*(version|def appVersion)\\s*[=]\\s*['\"](\\S+?)['\"]");
	private static final Logger LOGGER = LogHelper.getLogger(Platform.class);
	
	private Platform() {
	}
	
	/**
	 * Returns the platform's name. Examples of returned values are "Windows 8"
	 * and "OS X Mavericks".
	 * <p>
	 * Also see {@link #getPlatformFamily()} to obtain the platform's "family"
	 * (i.e. Windows/OS X/Linux) and the {@code isX()} convenience methods for 
	 * testing against a specific platform (e.g. {@link #isWindows()}).
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
		} else if (os.toLowerCase().contains("os x")) {
			platformName = "OS X " + getOSXVersionName();
		}
		
		return platformName;
	}
	
	private static String getOSXVersionName() {
		String version = System.getProperty("os.version");
		for (Map.Entry<String, String> entry : OS_X_VERSION_NAMES.entrySet()) {
			if (version.startsWith(entry.getKey())) {
				return entry.getValue();
			}
		}
		LOGGER.warning("Unknown OS X version: " + version);
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
	 * Returns the platform's "family" name. For example, Windows XP, Windows 7,
	 * and Windows 8 will all return "Windows" as their platform family.
	 * <p>
	 * Also see {@link #getPlatformName()} to obtain the platform's full name,
	 * and the {@code isX()} convenience methods for testing against a specific
	 * platform (e.g. {@link #isWindows()}).
	 */
	public static String getPlatformFamily() {
		if (isWindows()) {
			return "Windows";
		} else if (isOSX()) {
			return "OS X";
		} else if (isLinux()) {
			return "Linux";
		} else if (isGoogleAppEngine()) {
			return "Google App Engine";
		} else {
			return "Unknown";
		}
	}
	
	public static boolean isWindows() {
		return getPlatformName().startsWith("Windows");
	}
	
	public static boolean isOSX() {
		return getPlatformName().contains("OS X");
	}
	
	public static boolean isLinux() {
		return getPlatformName().startsWith("Linux");
	}
	
	public static boolean isAndroid() {
		return getPlatformName().startsWith("Android");
	}
	
	public static boolean isGoogleAppEngine() {
		return getPlatformName().startsWith("Google App Engine");
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
	 * Returns true if the application is running inside of the OS X app sandbox.
	 * When running inside of the sandbox access to system resources is limited 
	 * to the entitlements specified when signing the app.
	 */
	public static boolean isOSXAppSandboxEnabled() {
		if (!isOSX()) {
			return false;
		}
		
		String sandboxContainer = System.getenv("APP_SANDBOX_CONTAINER_ID");
		return sandboxContainer != null && !sandboxContainer.isEmpty();
	}
	
	/**
	 * Returns true if the application was downloaded from the Mac App Store.
	 * Note that App Store applications are always sandboxed (see
	 * {@link isOSXAppSandboxEnabled() for more information).
	 */
	public static boolean isMacAppStore() {
		return isOSXAppSandboxEnabled();
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
	 * Returns the amount of heap memory that is currently being used by the JVM
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
		Version androidN = Version.parse("6.1"); // API level 24
		
		if (androidVersion.isAtLeast(androidN)) {
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
	public static File getWorkingDirectory() {
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
	public static File getUserHome() {
		String userHome = System.getProperty("user.home");
		if (userHome == null || userHome.isEmpty()) {
			throw new UnsupportedOperationException("Platform does not support user accounts");
		}
		return new File(userHome);
	}
	
	/**
	 * Opens a stream to a resource file (a file included with the application).
	 * This will search both the classpath and the local file system. 
	 * @param path Relative path of the resource file to open.
	 * @throws IOException if an I/O error occurs while reading the file.
	 * @throws FileNotFoundException if the resource file cannot be located.
	 * @throws IllegalArgumentException if the path is empty.
	 */
	public static InputStream openResourceFile(String path) throws IOException {
		if (path == null || path.isEmpty()) {
			throw new IllegalArgumentException("Empty path");
		}
		
		return getAccessProvider().openResourceFile(path);
	}
	
	/**
	 * Returns the platform's directory for storing application data, for the
	 * application with the specified name. If the directory does not already
	 * exist it will be created.
	 * @param app Name of the application for which to create the directory.
	 * @throws IllegalArgumentException if the application name is empty.
	 * @throws UnsupportedOperationException if the platform does not allow
	 *         application data (e.g. Google App Engine).
	 */
	public static File getApplicationDataDirectory(String app) {
		if (app == null || app.trim().isEmpty()) {
			throw new IllegalArgumentException("Invalid application name");
		}
		
		return getAccessProvider().getApplicationDataDirectory(app);
	}

	/**
	 * Returns a {@code File} handle that points to a file inside the the platform's
	 * directory for storing application data, for the application with the specified
	 * name.
	 * @param app Name of the application for which to create the directory.
	 * @throws IllegalArgumentException if the application name is empty, or if the
	 *         path is absolote.
	 * @throws UnsupportedOperationException if the platform does not allow
	 *         application data (e.g. Google App Engine).
	 */
	public static File getApplicationData(String app, String path) {
		if (path.trim().isEmpty() || path.startsWith("/")) {
			throw new IllegalArgumentException("Invalid path: " + path);
		}
		
		File dir = getApplicationDataDirectory(app);
		return new File(dir.getAbsolutePath() + "/" + path);
	}
	
	/**
	 * Returns the platform's standard directory for storing user data (e.g. "My
	 * Documents" on Windows). 
	 * @throws UnsupportedOperationException if the platform does not allow access
	 *         to user files (e.g. Google App Engine).
	 */
	public static File getUserDataDirectory() {
		File userDir = getAccessProvider().getUserDataDirectory();
		if (userDir.exists() && userDir.isDirectory()) {
			return userDir;
		} else {
			return getUserHome();
		}
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
		
		File dir = getUserDataDirectory();	
		return new File(dir.getAbsolutePath() + "/" + path);
	}
	
	/**
	 * Returns the platform's directory for temporary files.
	 * @throws UnsupportedOperationException if the platform has no writable
	 *         file system. An example is Google App Engine.
	 */
	public static File getTempDirectory() {
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
		return System.getProperty("line.separator");
	}

	/**
	 * Returns the implementation version of the package containing the specified
	 * class. The default {@code PlatformAccessProvider} will check a number of
	 * locations, in the following order:
	 *
	 * <ul>
	 *   <li>The {@code Implementation-Version} of the JAR file.</li>
	 *   <li>A property file called {@code version.properties} in the classpath.</li>
	 *   <li>The Gradle build file (for development environments).</li>
	 * </ul>
	 * 
	 * If none of these locations contains a version number {@code null} is returned.
	 * <p>
	 * Changing this list or adding addtional locations can be done by registering
	 * a new {@code PlatformAccessProvider}.
	 */
	public static Version getImplementationVersion(Class<?> classInJarFile) {
		return getAccessProvider().getImplementationVersion(classInJarFile);
	}
	
	/**
	 * Registers a {@code PlatformAccessProvider} to introduce or improve support
	 * for additional platforms.
	 */
	public static void registerAccessProvider(PlatformAccessProvider accessProvider) {
		accessProviders.add(0, accessProvider);
	}
	
	public static void unregisterAccessProvider(PlatformAccessProvider accessProvider) {
		accessProviders.remove(accessProvider);
	}
	
	/**
	 * Returns the {@code PlatformAccessProvider} that is currently active. 
	 * Additional providers can be registered by using 
	 * {@link #registerAccessProvider(PlatformAccessProvider)}.
	 * @throws IllegalStateException if all providers have been unregistered.
	 */
	public static PlatformAccessProvider getAccessProvider() {
		for (PlatformAccessProvider accessProvider : accessProviders) {
			if (accessProvider.supports()) {
				return accessProvider;
			}
		}
		throw new IllegalStateException("No providers registered");
	}
	
	/**
	 * Returns the default implementation of {@code PlatformAccessProvider} that
	 * is always available, regardless of how many custom providers have been
	 * registered.
	 */
	public static PlatformAccessProvider getDefaultAccessProvider() {
		return accessProviders.get(accessProviders.size() - 1);
	}
	
	/**
	 * Exposes platform-specific information and resources. This is used as an
	 * extension mechanim to introduce or improve support for additional platforms.
	 * If the provider's {@link #supports()} returns true it will be used as 
	 * implementation for the corresponding methods in {@link Platform}.
	 */
	public static interface PlatformAccessProvider {
		
		/**
		 * Returns true if this provider supports this platform. If this returns
		 * false the provider will not be used.
		 */
		public boolean supports();
		
		public InputStream openResourceFile(String path) throws IOException;
		
		public File getApplicationDataDirectory(String app);
		
		public File getUserDataDirectory();
		
		public Version getImplementationVersion(Class<?> classInJarFile);
	}
	
	/**
	 * Standard implementation of a {@code PlatformAccessProvider} that attempts to
	 * support as many platforms as possible, using standard Java SE classes, system
	 * properties, and environment variables. 
	 */
	private static class StandardPlatformAccessProvider implements PlatformAccessProvider {
		
		private static AtomicBoolean developmentEnvironmentWarningShown = new AtomicBoolean(false);

		public boolean supports() {
			return true;
		}
		
		public InputStream openResourceFile(String path) throws IOException {
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
		
		public File getApplicationDataDirectory(String app) {
			File appDir = null;
			if (isWindows()) { 
				File applicationData = new File(System.getenv("APPDATA"));
				appDir = new File(applicationData, app);
			} else if (isOSX()) {
				appDir = getOSXApplicationDataDirectory(app);
			} else {
				appDir = new File(getUserHome(), "." + app);
			}
			
			try {
				LoadUtils.mkdir(appDir);
			} catch (IOException e) {
				throw new UnsupportedOperationException("Cannot create application data directory", e);
			}
			
			return appDir;
		}
		
		private File getOSXApplicationDataDirectory(String app) {
			if (isOSXAppSandboxEnabled()) {
				File sandboxContainer = new File(System.getenv("HOME"));
				return sandboxContainer;				
			} else {
				File applicationSupport = new File(System.getenv("HOME") + "/Library/Application Support");
				return new File(applicationSupport, app);
			}
		}
		
		public File getUserDataDirectory() {
			if (isWindows()) { 
				return getWindowsMyDocumentsDirectory();
			} else if (isOSX()) {
				return getOSXDocumentsDirectory();
			} else {
				return getUserHome();
			}
		}
		
		private File getWindowsMyDocumentsDirectory() {
			// There is no API for this other than using the Swing file dialog. 
			// This has to be done through reflection since this class is also 
			// used on Android and Google App Engine.
			try {
				Class<?> fileSystemViewClass = Class.forName("javax.swing.filechooser.FileSystemView");
				Object fileSystemView = fileSystemViewClass.getMethod("getFileSystemView").invoke(null);
				return (File) fileSystemView.getClass().getMethod("getDefaultDirectory").invoke(fileSystemView);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Attempt to locate My Documents failed", e);
				return getUserHome();
			}
		}
		
		private File getOSXDocumentsDirectory() {
			if (isOSXAppSandboxEnabled()) {
				// Mac App Store has a sandbox container per application.
				return new File(System.getenv("HOME"));
			} else {
				// The "Documents" directory has the same name on non-English 
				// versions of OS X, according to Apple's documentation.
				return new File(System.getenv("HOME") + "/Documents");
			}
		}
		
		public Version getImplementationVersion(Class<?> classInJarFile) {
			String implementationVersion = readImplementationVersionFromManifest(classInJarFile);
			if (implementationVersion == null) {
				implementationVersion = readVersionFromPropertyFile();
			}
			if (implementationVersion == null) {
				if (!developmentEnvironmentWarningShown.get()) {
					developmentEnvironmentWarningShown.set(true);
					LOGGER.warning("Using development environment");
				}
				implementationVersion = readVersionFromGradleBuildFile();
			}
			
			if (implementationVersion != null) {
				return Version.parse(implementationVersion);
			} else {
				return null;
			}
		}

		private String readImplementationVersionFromManifest(Class<?> classInJarFile) {
			try {
				URL codeLocation = classInJarFile.getProtectionDomain().getCodeSource().getLocation();
				URL manifestLocation = new URL("jar:" + codeLocation + "!/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(manifestLocation.openStream());
				return manifest.getMainAttributes().getValue("Implementation-Version");
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Cannot read Implementation-Version from manifest");
				return null;
			}
		}
		
		private String readVersionFromPropertyFile() {
			ResourceFile propertyFile = new ResourceFile("version.properties");
			if (propertyFile.exists()) {
				Properties props = LoadUtils.loadProperties(propertyFile, Charsets.UTF_8);
				if (props.getProperty("version") != null) {
					return props.getProperty("version");
				}
			}
			return null;
		}

		private String readVersionFromGradleBuildFile() {
			File gradleBuildFile = new File("build.gradle");
			if (!gradleBuildFile.exists()) {
				gradleBuildFile = new File("../build.gradle");
			}
			if (gradleBuildFile.exists()) {
				try {
					String contents = Files.toString(gradleBuildFile, Charsets.UTF_8);
					return TextUtils.matchFirst(contents, GRADLE_VERSION_PATTERN, 2).orNull();
				} catch (IOException e) {
					LOGGER.log(Level.WARNING, "Cannot read version from Gradle", e);
				}
			}
			return null;
		}
	}
}
