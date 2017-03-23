//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code LoadUtils} class.
 */
public class TestLoadUtils {
	
	@Test
	public void testReadToBytes() throws IOException {
		byte[] bytes = "test".getBytes(Charsets.UTF_8.displayName());
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		assertArrayEquals(bytes, LoadUtils.readToByteArray(stream));
	}

	@Test
	public void testReadToString() throws IOException {
		StringReader reader = new StringReader("test");
		assertEquals("test", LoadUtils.readToString(reader));
	}
	
	@Test
	public void testReadLines() throws IOException {
		StringReader reader = new StringReader("a\nb\r\n\nc\n");
		List<String> lines = LoadUtils.readLines(reader);
		assertEquals(4, lines.size());
		assertEquals("a", lines.get(0));
		assertEquals("b", lines.get(1));
		assertEquals("", lines.get(2));
		assertEquals("c", lines.get(3));
	}
	
	@Test
	public void testReadFirstLines() throws IOException {
		assertEquals("", LoadUtils.readFirstLines(new StringReader(""), 2));
		assertEquals("a\nb\n", LoadUtils.readFirstLines(new StringReader("a\nb\n"), 2));
		assertEquals("a\nb\n", LoadUtils.readFirstLines(new StringReader("a\nb"), 2));
		assertEquals("a\nb\n", LoadUtils.readFirstLines(new StringReader("a\nb\nc\n"), 2));
		assertEquals("a\n", LoadUtils.readFirstLines(new StringReader("a\n"), 2));
	}
	
	@Test
	public void testReadLastLines() throws IOException {
		assertEquals("", LoadUtils.readLastLines(new StringReader(""), 2));
		assertEquals("a\n", LoadUtils.readLastLines(new StringReader("a\n"), 2));
		assertEquals("a\nb\n", LoadUtils.readLastLines(new StringReader("a\nb\n"), 2));
		assertEquals("b\nc\n", LoadUtils.readLastLines(new StringReader("a\nb\nc\n"), 2));
	}
	
	@Test
	@SuppressWarnings("deprecation")
	public void testLoadProperties() throws IOException {
		String str = "a=value\nb=valu\u00C9 2";
		
		Properties p1 = LoadUtils.loadProperties(new StringReader(str));
		assertEquals("value", p1.getProperty("a"));
		assertEquals("valu\u00C9 2", p1.getProperty("b"));
		
		Properties p2 = LoadUtils.loadProperties(new ByteArrayInputStream(str.getBytes("ISO-8859-1")));
		assertEquals("value", p2.getProperty("a"));
		assertEquals("valu\u00C9 2", p2.getProperty("b"));
		
		Properties p3 = LoadUtils.loadProperties(new ByteArrayInputStream(str.getBytes("UTF-8")), 
				Charsets.UTF_8);
		assertEquals("value", p3.getProperty("a"));
		assertEquals("valu\u00C9 2", p3.getProperty("b"));
	}
	
	@Test
	public void testToProperties() {
		Properties props = LoadUtils.toProperties("key", "value");
		assertEquals("value", props.get("key"));
		
		props = LoadUtils.toProperties("key", "value", "key2", "value2", "key3", "value3");
		assertEquals("value", props.get("key"));
		assertEquals("value2", props.get("key2"));
		assertEquals("value3", props.get("key3"));
	}
	
	@Test
	public void testSaveProperties() throws IOException {
		File tempFile = LoadUtils.getTempFile(".properties");
		Properties props = LoadUtils.toProperties("a", "something", "b", "test", "a.x", "2");
		LoadUtils.saveProperties(props, tempFile, Charsets.UTF_8);
		
		String expected = "";
		expected += "a=something\n";
		expected += "a.x=2\n";
		expected += "b=test\n";
		
		assertTrue(tempFile.exists());
		assertEquals(expected, Files.toString(tempFile, Charsets.UTF_8));
	}
	
	@Test
	public void testCopyFile() throws IOException {
		File source = LoadUtils.getTempFile(".txt");
		File dest = LoadUtils.getTempFile(".txt");
		Files.write("test\n", source, Charsets.UTF_8);
		Files.copy(source, dest);
		assertTrue(dest.exists());
		assertEquals("test\n", LoadUtils.readToString(fileReader(dest)));
	}
	
	@Test
	public void testRelativePath() {
		assertEquals("c/d.txt", LoadUtils.getRelativePath(new File("/a/b/c/d.txt"), new File("/a/b")));
		assertEquals("c/d.txt", LoadUtils.getRelativePath(new File("/a/b/c/d.txt"), new File("/a/b/")));
		assertEquals("c.txt", LoadUtils.getRelativePath(new File("/a/b/c.txt"), new File("/a/b/")));
		
		File base = new File("/Developer/Test");
		assertEquals("/Developer/Test", LoadUtils.getRelativePath(base, base));
		assertEquals("/Bla/Test", LoadUtils.getRelativePath(new File("/Bla/Test"), base));
		
		File windowsPath = new File("C:\\Program Files");
		assertEquals("test.txt", LoadUtils.getRelativePath(
				new File("C:\\Program Files\\test.txt"), windowsPath));
		assertEquals("Bla\\test.txt", LoadUtils.getRelativePath(
				new File("C:\\Program Files\\Bla\\test.txt"), windowsPath));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testToURL() throws MalformedURLException, URISyntaxException {
		assertEquals(new URL("http://www.colorize.nl/"), LoadUtils.toURL("http://www.colorize.nl/"));
		assertEquals(new URL("file:///a.txt"), LoadUtils.toURL(new File("/a.txt")));
		LoadUtils.toURL("kees://invalid");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testToURI() throws URISyntaxException {
		assertEquals(new URI("file:///a.txt"), LoadUtils.toURI("file:///a.txt"));
		LoadUtils.toURI("\\non/sense!");
	}
	
	@Test
	public void testMkdir() throws IOException {
		File tempDir = LoadUtils.createTempDir();
		File subDir = new File(tempDir, "sub");
		LoadUtils.mkdir(subDir);
		assertTrue(subDir.exists());
		assertTrue(subDir.isDirectory());
	}
	
	@Test
	public void testDeleteFile() throws IOException {
		File tempFile = LoadUtils.createTempFile("test", Charsets.UTF_8);
		assertTrue(tempFile.exists());
		LoadUtils.delete(tempFile);
		assertFalse(tempFile.exists());
		LoadUtils.delete(tempFile);
	}
	
	@Test
	public void testRenameFile() throws Exception {
		File from = LoadUtils.createTempFile("test", Charsets.UTF_8);
		File to = new File(from.getParentFile(), from.getName() + "_2");
		LoadUtils.rename(from, to);
		assertFalse(from.exists());
		assertTrue(to.exists());
		assertEquals("test", Files.toString(to, Charsets.UTF_8));
	}
	
	@Test
	public void testTouch() throws Exception {
		File tempFile = LoadUtils.createTempFile("test", Charsets.UTF_8);
		long oldValue = tempFile.lastModified();
		Thread.sleep(1100);
		LoadUtils.touch(tempFile);
		long newValue = tempFile.lastModified();
		assertTrue(newValue > oldValue);
	}
	
	@Test
	public void testFileExtensionFilter() throws Exception {
		FilenameFilter filter = LoadUtils.getFileExtensionFilter("jpg", "png");
		assertTrue(filter.accept(new File("/tmp"), "test.jpg"));
		assertTrue(filter.accept(new File("/tmp"), "test.png"));
		assertTrue(filter.accept(new File("/tmp"), "test.PNG"));
		assertFalse(filter.accept(new File("/tmp"), "test.txt"));
		assertFalse(filter.accept(new File("/tmp"), "test.jpg.txt"));
	}
	
	@Test
	public void testGlobFilter() throws Exception {
		FilenameFilter filter = LoadUtils.getGlobFilter("*.txt");
		assertTrue(filter.accept(new File("/tmp"), "test.txt"));
		assertTrue(filter.accept(new File("/tmp"), "test.TXT"));
		assertFalse(filter.accept(new File("/tmp"), "test.png"));
	}
	
	private InputStreamReader fileReader(File f) throws IOException {
		return new InputStreamReader(new FileInputStream(f), Charsets.UTF_8);
	}
}
