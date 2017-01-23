//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;

import nl.colorize.util.Platform;
import nl.colorize.util.Version;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code Platform} class.
 */
public class TestPlatform {
	
	@Test
	public void testPlatformSupport() {
		Class<?> platformImplClass = Platform.getCurrentPlatform().getClass();
		if (Platform.isMac()) {
			assertTrue(platformImplClass.getName().contains("MacPlatform"));
		} else {
			assertTrue(platformImplClass.getName().contains("LinuxPlatform"));
		}
	}
	
	@Test
	public void testJavaVersion() {
		Version version = Platform.getJavaVersion();
		String versionString = System.getProperty("java.version");
		assertEquals(versionString, version.toString());
		assertEquals(1, version.getDigit(0));
		assertEquals(8, version.getDigit(1));
		assertEquals(0, version.getDigit(2));
	}

	@Test
	public void testLineSeparator() {
		assertEquals("\n", Platform.getLineSeparator());
	}
	
	@Test
	public void testWorkingDirectory() {
		assertTrue(Platform.getUserWorkingDirectory().exists());
		assertTrue(Platform.getUserWorkingDirectory().isDirectory());
	}
	
	@Test
	public void testUserHome() {
		assertTrue(Platform.getUserAccount().length() >= 2);
		assertTrue(Platform.getUserHomeDir().exists());
		assertTrue(Platform.getUserHomeDir().isDirectory());
	}
	
	@Test
	public void testApplicationData() {
		File testFile = Platform.getApplicationData("Test", "test.txt");
		File appDir = testFile.getParentFile();
		assertTrue(appDir.exists());
		assertTrue(appDir.isDirectory());
		if (Platform.isMac()) {
			assertEquals("Test", appDir.getName());
		} else {
			assertEquals(".Test", appDir.getName());
		}
		assertFalse(testFile.exists());
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
		
		File userhome = Platform.getUserHomeDir();
		assertTrue(userhome.exists());
		assertTrue(userhome.isDirectory());
	}
	
	@Test
	public void testTempDir() {
		File tempDir = Platform.getTempDir();
		assertTrue(tempDir.exists());
		assertTrue(tempDir.isDirectory());
	}
}
