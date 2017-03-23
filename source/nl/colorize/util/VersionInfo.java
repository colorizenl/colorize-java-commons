//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

/**
 * Utility class to obtain the application's version number. For this to work
 * the version number has to be stored (during the build) in a location that
 * matches one of the following conventions:
 * <ul>
 *   <li>A property 'version' in a classpath file called 'version.properties'</li>
 *   <li>The Implementation-Version attribute in the JAR file's manifest</li>
 *   <li>The Android app version declared in AndroidManifest.xml</li>
 *   <li>The property 'version' in the Gradle build script (during development)</li>
 * </ul>
 */
public final class VersionInfo {
	
	private static final Pattern GRADLE_VERSION_PATTERN = Pattern.compile(
			"\\s*(version|def appVersion|ext.appVersion)\\s*[=]\\s*['\"](\\S+?)['\"]");
	private static final Logger LOGGER = LogHelper.getLogger(VersionInfo.class);

	private VersionInfo() {
	}
	
	/**
	 * Returns the application's version number by searching the locations
	 * described in the class documentation. The locations are searched in
	 * order until a match is found. If the version number cannot be located
	 * in any of the locations this method will return {@code null}.
	 */
	public static Version getVersion() {
		String versionNumber = locateVersionNumber();
		if (versionNumber != null) {
			try {
				return Version.parse(versionNumber);
			} catch (IllegalArgumentException e) {
				LOGGER.warning(versionNumber + " is not a valid version number");
			}
		}
		return null;
	}
	
	private static String locateVersionNumber() {
		String versionNumber = getPropertyFileVersion();
		if (versionNumber == null) {
			versionNumber = getImplementationVersionFromJAR();
			if (versionNumber == null) {
				versionNumber = getAndroidAppVersion();
				if (versionNumber == null) {
					versionNumber = getGradleBuildScriptVersion();
				}
			}
		}
		return versionNumber;
	}

	private static String getPropertyFileVersion() {
		ResourceFile propertyFile = new ResourceFile("version.properties");
		if (propertyFile.exists()) {
			Properties props = LoadUtils.loadProperties(propertyFile, Charsets.UTF_8);
			if (props.getProperty("version") != null) {
				return props.getProperty("version");
			}
		}
		return null;
	}
	
	private static String getImplementationVersionFromJAR() {
		if (!Platform.isAndroid()) {
			try {
				URL codeLocation = VersionInfo.class.getProtectionDomain().getCodeSource().getLocation();
				URL manifestLocation = new URL("jar:" + codeLocation + "!/META-INF/MANIFEST.MF");
				Manifest manifest = new Manifest(manifestLocation.openStream());
				return manifest.getMainAttributes().getValue("Implementation-Version");
			} catch (FileNotFoundException e) {
				// JAR file does not have a manifest, return null later on.
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Cannot read Implementation-Version from manifest");
			}
		}
		return null;
	}
	
	private static String getAndroidAppVersion() {
		if (Platform.isAndroid()) {
			//TODO this is a hideous hack. Support for reading the app
			//     version from AndroidManifest.xml is implemented in 
			//     AndroidPlatform, which extends the regular Platform 
			//     class through Platform.register(...). That class is
			//     located in MultimediaLib, a completely separate
			//     library. The way this is currently implemented is by
			//     checking whether AndroidPlatform has been registered
			//     and then accessing the method via reflection. This is
			//     of course poor design and also error prone.
			Platform platformImpl = Platform.getCurrentPlatform();
			Class<?> platformClass = platformImpl.getClass();
			if (platformClass.getName().contains("AndroidPlatform")) {
				try {
					return platformClass.getMethod("getAppVersion").invoke(platformImpl).toString();
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Accessing Android app version failed", e);
				}
			}
		}
		return null;
	}
	
	private static String getGradleBuildScriptVersion() {
		File projectDir = new File("build.gradle").getAbsoluteFile().getParentFile();
		String version = getGradleBuildScriptVersion(projectDir);
		if (version == null) {
			version = getGradleBuildScriptVersion(projectDir.getParentFile());
		}
		return version;
	}
	
	private static String getGradleBuildScriptVersion(File projectDir) {
		File gradleBuildFile = new File(projectDir, "build.gradle");
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
