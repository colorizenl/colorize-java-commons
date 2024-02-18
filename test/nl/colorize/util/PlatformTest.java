//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlatformTest {

    @Test
    public void testPlatformDetection() {
        assertTrue(Platform.isWindows() || Platform.isMac() || Platform.isLinux());
        assertFalse(Platform.isTeaVM());
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
    
    @Test
    public void testEmptyAppDir() {
        assertThrows(IllegalArgumentException.class, () -> {
            Platform.getApplicationData(" ", " ");
        });
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
