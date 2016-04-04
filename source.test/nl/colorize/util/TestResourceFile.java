//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import nl.colorize.util.LoadUtils;
import nl.colorize.util.ResourceFile;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code ResourceFile} class.
 */
public class TestResourceFile {

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
		String expected = Files.toString(testFile, Charsets.UTF_8);
		
		ResourceFile resourceFile = new ResourceFile("build.gradle");
		String actual = LoadUtils.readToString(resourceFile.openStream(), Charsets.UTF_8);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testAbsolutePath() throws IOException {
		File tempFile = LoadUtils.getTempFile(".txt");
		assertTrue(tempFile.getAbsolutePath().startsWith("/"));
		Files.write("test\n", tempFile, Charsets.UTF_8);
		
		ResourceFile resourceFile = new ResourceFile(tempFile);
		String contents = LoadUtils.readToString(resourceFile.openReader(Charsets.UTF_8));
		assertEquals("test\n", contents);
	}
	
	@Test
	public void testFallbackToClasspath() throws Exception {
		File localFile = new File("nl/colorize/util/TestResourceFile.class");
		assertFalse(localFile.exists());
		
		ResourceFile resourceFile = new ResourceFile("nl/colorize/util/TestResourceFile.class");
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
}
