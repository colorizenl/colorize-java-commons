//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import nl.colorize.util.DirectoryWalker;
import nl.colorize.util.LoadUtils;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for the {@code DirectoryWalker} class. It also tests filtering its
 * results using a {@code PatternFileFilter}.
 */
public class TestDirectoryWalker {
	
	private File rootDir;

	@Before
	public void before() throws IOException {
		rootDir = LoadUtils.createTempDir();
		File firstSubDir = createTempDir(rootDir, "first");
		createTempDir(rootDir, "second");
		createTempDir(rootDir, ".third");
		
		Files.write("test", new File(rootDir, "a.txt"), Charsets.UTF_8);
		Files.write("test 2", new File(firstSubDir, "b.txt"), Charsets.UTF_8);
		Files.write("test 3", new File(firstSubDir, "c"), Charsets.UTF_8);
		Files.write("test 4", new File(firstSubDir, ".d"), Charsets.UTF_8);
	}
	
	@Test
	public void testVisitAll() throws IOException {
		DirectoryWalker directoryWalker = new DirectoryWalker();
		directoryWalker.setVisitHiddenFiles(true);
		
		File[] visitedFiles = sort(directoryWalker.walk(rootDir));
		assertEquals(4, visitedFiles.length);
		assertEquals("a.txt", visitedFiles[0].getName());
		assertEquals("b.txt", visitedFiles[1].getName());
		assertEquals("c", visitedFiles[2].getName());
		assertEquals(".d", visitedFiles[3].getName());
	}
	
	@Test
	public void testIgnoreHiddenFiles() throws IOException {
		DirectoryWalker directoryWalker = new DirectoryWalker();
		directoryWalker.setVisitHiddenFiles(false);
		
		File[] visitedFiles = sort(directoryWalker.walk(rootDir));
		assertEquals(3, visitedFiles.length);
		assertEquals("a.txt", visitedFiles[0].getName());
		assertEquals("b.txt", visitedFiles[1].getName());
		assertEquals("c", visitedFiles[2].getName());
	}
	
	@Test
	public void testIncludeSubdirectories() throws IOException {
		DirectoryWalker directoryWalker = new DirectoryWalker();
		directoryWalker.setIncludeSubdirectories(true);
		directoryWalker.setVisitHiddenFiles(true);
		
		Collection<File> visitedFiles = directoryWalker.walk(rootDir);
		assertEquals(7, visitedFiles.size());
		assertTrue(visitedFiles.contains(new File(rootDir, "first")));
		assertTrue(visitedFiles.contains(new File(rootDir, "second")));
		assertTrue(visitedFiles.contains(new File(rootDir, ".third")));
		
		directoryWalker.setVisitHiddenFiles(false);
		
		visitedFiles = directoryWalker.walk(rootDir);
		assertEquals(5, visitedFiles.size());
		assertTrue(visitedFiles.contains(new File(rootDir, "first")));
		assertTrue(visitedFiles.contains(new File(rootDir, "second")));
		assertFalse(visitedFiles.contains(new File(rootDir, ".third")));
	}
	
	@Test
	public void testFilterOnFileExtension() throws IOException {
		DirectoryWalker directoryWalker = new DirectoryWalker();
		directoryWalker.setFileFilter(LoadUtils.getGlobFilter("*.txt"));
		
		File[] visitedFiles = sort(directoryWalker.walk(rootDir));
		assertEquals(2, visitedFiles.length);
		assertEquals("a.txt", visitedFiles[0].getName());
		assertEquals("b.txt", visitedFiles[1].getName());
	}
	
	@Test
	public void testRecursive() throws IOException {
		DirectoryWalker directoryWalker = new DirectoryWalker();
		directoryWalker.setRecursive(false);
		
		File[] visitedFiles = sort(directoryWalker.walk(rootDir));
		assertEquals(1, visitedFiles.length);
		assertEquals("a.txt", visitedFiles[0].getName());
	}
	
	@Test
	public void testFilterSubdirectories() throws Exception {
		File tempDir = LoadUtils.createTempDir();
		File firstSubDir = createTempDir(tempDir, ".abc");
		LoadUtils.createTempFile(firstSubDir, "a.txt", "a", Charsets.UTF_8);
		File secondSubDir = createTempDir(tempDir, ".svn");
		LoadUtils.createTempFile(secondSubDir, "b.txt", "b", Charsets.UTF_8);
		
		DirectoryWalker dirWalker = new DirectoryWalker();
		dirWalker.setVisitHiddenFiles(true);
		assertEquals(2, dirWalker.walk(tempDir).size());
		
		dirWalker.setDirectoryFilter(LoadUtils.getGlobFilter(".svn"));
		assertEquals(1, dirWalker.walk(tempDir).size());
		assertEquals("b.txt", dirWalker.walk(tempDir).get(0).getName());
	}
	
	@Test
	public void testMaxDepth() throws Exception {
		File tempDir = LoadUtils.createTempDir();
		File firstSubDir = createTempDir(tempDir, "a");
		createTempDir(firstSubDir, "c");
		createTempDir(firstSubDir, "d");
		File secondSubDir = createTempDir(tempDir, "b");
		createTempDir(secondSubDir, "e");
		
		DirectoryWalker dirWalker = new DirectoryWalker();
		dirWalker.setIncludeSubdirectories(true);
		
		assertEquals(5, dirWalker.walk(tempDir).size());
		
		dirWalker.setMaxDepth(1);
		List<File> results = dirWalker.walk(tempDir);
		
		assertEquals(2, results.size());
		assertEquals("a", results.get(0).getName());
		assertEquals("b", results.get(1).getName());
	}
	
	private File createTempDir(File parent, String name) {
		File dir = new File(parent, name);
		dir.mkdir();
		dir.deleteOnExit();
		return dir;
	}

	private File[] sort(Collection<File> files) {
		File[] sorted = files.toArray(new File[0]);
		Arrays.sort(sorted, new Comparator<File>() {
			public int compare(File a, File b) {
				return a.getName().replace(".", "").compareTo(b.getName().replace(".", ""));
			}
		});
		return sorted;
	}
}
