//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import org.junit.Test;

public class FileUtilsTest {

    @Test
    public void testReadWriteBinary() throws Exception {
        byte[] contents = {1, 2, 3};
        File tempFile = FileUtils.createTempFile(contents);
        
        assertArrayEquals(contents, FileUtils.read(tempFile));
    }
    
    @Test
    public void testReadWriteText() throws Exception {
        File tempFile = FileUtils.createTempFile("test", Charsets.UTF_8);
        
        assertEquals("test", FileUtils.read(tempFile, Charsets.UTF_8));
    }
    
    @Test
    public void testReadLines() throws Exception {
        File tempFile = FileUtils.createTempFile("first\nsecond\nthird", Charsets.UTF_8);
        
        assertEquals(ImmutableList.of("first", "second", "third"), 
                FileUtils.readLines(tempFile, Charsets.UTF_8));
    }
    
    @Test
    public void testWriteLines() throws Exception {
        List<String> lines = ImmutableList.of("first", "second", "third");
        File tempFile = FileUtils.createTempFile("", Charsets.UTF_8);
        FileUtils.write(lines, Charsets.UTF_8, tempFile);
        
        assertEquals(lines, FileUtils.readLines(tempFile, Charsets.UTF_8));
    }
    
    @Test
    public void testReadFirstLines() throws Exception {
        File tempFile = FileUtils.createTempFile("a\nb\nc\nd\ne\n", Charsets.UTF_8);
        
        assertEquals(ImmutableList.of("a", "b", "c"), 
                FileUtils.readFirstLines(tempFile, Charsets.UTF_8, 3));
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e"), 
                FileUtils.readFirstLines(tempFile, Charsets.UTF_8, 5));
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e"), 
                FileUtils.readFirstLines(tempFile, Charsets.UTF_8, 6));
    }
    
    @Test
    public void testMkDir() throws IOException {
        File tempDir = FileUtils.createTempDir();
        File testDir = new File(tempDir, "test-" + System.currentTimeMillis());
        FileUtils.mkdir(testDir);
        
        assertTrue(testDir.exists());
        assertTrue(testDir.isDirectory());
    }
    
    @Test
    public void testDelete() throws IOException {
        File tempFile = FileUtils.createTempFile("test", Charsets.UTF_8);
        FileUtils.delete(tempFile);
        
        assertFalse(tempFile.exists());
    }
    
    @Test
    public void testRelativePath() {
        assertEquals("c/d.txt", FileUtils.getRelativePath(new File("/a/b/c/d.txt"), new File("/a/b")));
        assertEquals("c/d.txt", FileUtils.getRelativePath(new File("/a/b/c/d.txt"), new File("/a/b/")));
        assertEquals("c.txt", FileUtils.getRelativePath(new File("/a/b/c.txt"), new File("/a/b/")));
        
        File base = new File("/Developer/Test");
        assertEquals("/Developer/Test", FileUtils.getRelativePath(base, base));
        assertEquals("/Bla/Test", FileUtils.getRelativePath(new File("/Bla/Test"), base));
        
        File windowsPath = new File("C:\\Program Files");
        assertEquals("test.txt", FileUtils.getRelativePath(
                new File("C:\\Program Files\\test.txt"), windowsPath));
        assertEquals("Bla\\test.txt", FileUtils.getRelativePath(
                new File("C:\\Program Files\\Bla\\test.txt"), windowsPath));
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("png", FileUtils.getFileExtension(new File("test.png")));
        assertEquals("png", FileUtils.getFileExtension(new File("a/test.png")));
        assertEquals("old", FileUtils.getFileExtension(new File("a/test.png.old")));
        assertEquals("", FileUtils.getFileExtension(new File("a/test")));
        assertEquals("", FileUtils.getFileExtension(new File(".project")));
    }
}