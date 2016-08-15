//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;

import javax.servlet.http.HttpServletResponse;

import nl.colorize.util.Platform;
import nl.colorize.util.Version;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code Platform} class.
 */
public class TestPlatform {
	
	@Test
	public void testJavaVersion() {
		Version version = Platform.getJavaVersion();
		String versionString = System.getProperty("java.version");
		assertEquals(versionString, version.toString());
		assertEquals(1, version.getDigit(0));
		assertTrue(version.getDigit(1) >= 5);
		assertEquals(0, version.getDigit(2));
	}

	@Test
	public void testLineSeparator() {
		assertEquals("\n", Platform.getLineSeparator());
	}
	
	@Test
	public void testWorkingDirectory() {
		assertTrue(Platform.getWorkingDirectory().exists());
		assertTrue(Platform.getWorkingDirectory().isDirectory());
	}
	
	@Test
	public void testUserHome() {
		assertTrue(Platform.getUserAccount().length() >= 2);
		assertTrue(Platform.getUserHome().exists());
		assertTrue(Platform.getUserHome().isDirectory());
	}
	
	@Test
	public void testApplicationData() {
		File appdir = Platform.getApplicationDataDirectory("TestResources");
		assertTrue(appdir.exists());
		assertTrue(appdir.isDirectory());
		if (Platform.isMac()) {
			assertEquals("TestResources", appdir.getName());
		} else {
			assertEquals(".TestResources", appdir.getName());
		}
		appdir.deleteOnExit();

		File file = Platform.getApplicationData("TestResources", "test.txt");
		assertFalse(file.exists());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyAppDir() {
		Platform.getApplicationData(" ", " ");
		fail();
	}
	
	@Test
	public void testUserData() {
		File file = Platform.getUserData("test.txt");
		assertFalse(file.exists());
		
		File userdir = file.getParentFile();
		assertTrue(userdir.exists());
		assertTrue(userdir.isDirectory());
		
		File userhome = Platform.getUserHome();
		assertTrue(userhome.exists());
		assertTrue(userhome.isDirectory());
	}
	
	@Test
	public void testTempDir() {
		File tempDir = Platform.getTempDirectory();
		assertTrue(tempDir.exists());
		assertTrue(tempDir.isDirectory());
	}
	
	@Test
	public void testDevelopmentImplementationVersion() {
		assertEquals("2016.4", Platform.getImplementationVersion(Platform.class).toString());
	}
	
	@Test
	public void testJarImplementationVersion() {
		assertEquals(Version.parse("2.5"), Platform.getImplementationVersion(HttpServletResponse.class));
	}
}
