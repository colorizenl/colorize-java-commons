//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourceFileTest {

    @Test
    public void testGetPath() {
        assertEquals("kees.xml", new ResourceFile("kees.xml").getPath()); 
        assertEquals("kees.xml", new ResourceFile("kees.xml/").getPath()); 
        assertEquals("/kees.xml", new ResourceFile("/kees.xml").getPath());
        assertEquals("c:/temp/a.xml", new ResourceFile("c:\\temp\\a.xml").getPath());
        assertEquals("a/b.xml", new ResourceFile(new ResourceFile("a"), "b.xml").getPath());
        assertEquals("/a/b.xml", new ResourceFile(new ResourceFile("/a"), "b.xml").getPath());
        assertEquals("/a/b/c/d.xml", new ResourceFile(new ResourceFile("/a/b/"), "/a/b/c/d.xml").getPath());
    }
    
    @Test
    public void testGetName() {
        assertEquals("a.xml", new ResourceFile("a.xml").getName());
        assertEquals("c.xml", new ResourceFile("a/b/c.xml").getName()); 
        assertEquals("c.xml", new ResourceFile("/a/b/c.xml").getName());
        assertEquals("d.xml", new ResourceFile("c:\\d.xml").getName());
        assertEquals("b", new ResourceFile("/a/b/").getName()); 
    }
    
    @Test
    public void testOpenFile() throws IOException {
        File testFile = new File("build.gradle");
        assertTrue(testFile.exists());
        String expected = FileUtils.read(testFile, Charsets.UTF_8);
        
        ResourceFile resourceFile = new ResourceFile("build.gradle");
        String actual = LoadUtils.readToString(resourceFile.openStream(), Charsets.UTF_8);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testAbsolutePath() throws IOException {
        File tempFile = LoadUtils.getTempFile(".txt");
        assertTrue(tempFile.getAbsolutePath().startsWith("/"));
        FileUtils.write("test\n", Charsets.UTF_8, tempFile);
        
        ResourceFile resourceFile = new ResourceFile(tempFile);
        String contents = LoadUtils.readToString(resourceFile.openReader(Charsets.UTF_8));
        assertEquals("test\n", contents);
    }
    
    @Test
    public void testFallbackToClasspath() throws Exception {
        File localFile = new File("nl/colorize/util/TestResourceFile.class");
        assertFalse(localFile.exists());
        
        ResourceFile resourceFile = new ResourceFile("nl/colorize/util/ResourceFileTest.class");
        assertNotNull(resourceFile.openStream());
    }
    
    @Test(expected = FileNotFoundException.class)
    public void testNotFoundThrowsException() throws Exception {
        ResourceFile resourceFile = new ResourceFile("nonExisting.xml");
        assertFalse(resourceFile.exists());
        resourceFile.openStream();
    }
    
    @Test
    public void testReadBytes() throws Exception {
        byte[] bytes = {1, 2, 3, 4, 1, 4, 0, 6};
        ResourceFile file = new ResourceFile(LoadUtils.createTempFile(new ByteArrayInputStream(bytes)));
        assertArrayEquals(bytes, file.readBytes());
    }
    
    @Test
    public void testReadText() throws Exception {
        ResourceFile file = new ResourceFile(LoadUtils.createTempFile("Test\ntest", Charsets.UTF_8));
        assertEquals("Test\ntest", file.read(Charsets.UTF_8));
    }
    
    @Test
    public void testReadLines() throws Exception {
        ResourceFile file = new ResourceFile(LoadUtils.createTempFile("Test\ntest", Charsets.UTF_8));
        assertEquals(ImmutableList.of("Test", "test"), file.readLines(Charsets.UTF_8));
    }
}
