//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ResourceFileTest {

    @Test
    public void testGetPath() {
        assertEquals("kees.xml", new ResourceFile("kees.xml").path());
        assertEquals("kees.xml", new ResourceFile("kees.xml/").path());
        assertEquals("/kees.xml", new ResourceFile("/kees.xml").path());
        assertEquals("c:/temp/a.xml", new ResourceFile("c:\\temp\\a.xml").path());
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
        String expected = Files.readString(testFile.toPath(), UTF_8);
        
        ResourceFile resourceFile = new ResourceFile("build.gradle");
        String actual = resourceFile.read(UTF_8);

        assertEquals(expected, actual);
    }
    
    @Test
    public void testAbsolutePath(@TempDir File tempDir) throws IOException {
        File tempFile = new File(tempDir, "a.txt");
        assertTrue(tempFile.getAbsolutePath().startsWith("/"));
        Files.writeString(tempFile.toPath(), "test\n", UTF_8);
        
        ResourceFile resourceFile = new ResourceFile(tempFile);
        String contents = resourceFile.read(UTF_8);

        assertEquals("test\n", contents);
    }
    
    @Test
    public void testFallbackToClasspath() {
        File localFile = new File("nl/colorize/util/TestResourceFile.class");
        assertFalse(localFile.exists());
        
        ResourceFile resourceFile = new ResourceFile("nl/colorize/util/ResourceFileTest.class");
        assertNotNull(resourceFile.openStream());
    }
    
    @Test
    public void testNotFoundThrowsException() {
        ResourceFile resourceFile = new ResourceFile("nonExisting.xml");
        assertFalse(resourceFile.exists());

        assertThrows(ResourceException.class, resourceFile::openStream);
    }
    
    @Test
    public void testReadBytes(@TempDir File tempDir) throws Exception {
        File tempFile = new File(tempDir, "test.txt");
        byte[] bytes = {1, 2, 3, 4, 1, 4, 0, 6};
        Files.write(tempFile.toPath(), bytes);

        ResourceFile file = new ResourceFile(tempFile);

        assertArrayEquals(bytes, file.readBytes());
    }
    
    @Test
    public void testReadText(@TempDir File tempDir) throws Exception {
        File tempFile = new File(tempDir, "test.txt");
        Files.writeString(tempFile.toPath(), "Test\ntest", UTF_8);

        ResourceFile file = new ResourceFile(tempFile);

        assertEquals("Test\ntest", file.read(UTF_8));
    }
    
    @Test
    public void testReadLines(@TempDir File tempDir) throws Exception {
        File tempFile = new File(tempDir, "test.txt");
        Files.writeString(tempFile.toPath(), "Test\ntest", UTF_8);

        ResourceFile file = new ResourceFile(tempFile);

        assertEquals(List.of("Test", "test"), file.readLines(UTF_8));
    }

    @Test
    void getSibling() {
        assertEquals("other.txt", new ResourceFile("test.txt").sibling("other.txt").path());
        assertEquals("a/other.txt", new ResourceFile("a/test.txt").sibling("other.txt").path());
    }
}
