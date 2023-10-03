//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {

    @Test
    public void testWriteLines() throws Exception {
        List<String> lines = ImmutableList.of("first", "second", "third");
        File tempFile = FileUtils.createTempFile("", UTF_8);
        FileUtils.write(lines, UTF_8, tempFile);

        assertEquals(lines, Files.readAllLines(tempFile.toPath(), UTF_8));
    }
    
    @Test
    public void testReadFirstLines() throws Exception {
        File tempFile = FileUtils.createTempFile("a\nb\nc\nd\ne\n", UTF_8);
        
        assertEquals(ImmutableList.of("a", "b", "c"), 
                FileUtils.readFirstLines(tempFile, UTF_8, 3));
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e"), 
                FileUtils.readFirstLines(tempFile, UTF_8, 5));
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e"), 
                FileUtils.readFirstLines(tempFile, UTF_8, 6));
    }
    
    @Test
    public void testMkDir(@TempDir File tempDir) throws IOException {
        File testDir = new File(tempDir, "test-" + System.currentTimeMillis());
        FileUtils.mkdir(testDir);
        
        assertTrue(testDir.exists());
        assertTrue(testDir.isDirectory());
    }
    
    @Test
    public void testDelete() throws IOException {
        File tempFile = FileUtils.createTempFile("test", UTF_8);
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

    @Test
    public void testRewriteWithFunction() throws IOException {
        File tempFile = FileUtils.createTempFile("a\nb\nc\nd", UTF_8);

        FileUtils.rewrite(tempFile, UTF_8, line -> "2" + line);

        assertEquals(ImmutableList.of("2a", "2b", "2c", "2d"),
            Files.readAllLines(tempFile.toPath(), UTF_8));
    }

    @Test
    void copyDirectory(@TempDir File tempDir) throws IOException {
        Files.writeString(new File(tempDir, "a.txt").toPath(), "test", UTF_8);
        new File(tempDir, "b").mkdir();
        Files.writeString(new File(tempDir.getAbsolutePath() + "/b/c.txt").toPath(),
            "test", UTF_8);

        File outputDir = FileUtils.createTempDir();
        outputDir = new File(outputDir, "target");
        FileUtils.copyDirectory(tempDir, outputDir);

        assertTrue(outputDir.exists());
        assertTrue(new File(outputDir, "a.txt").exists());
        assertTrue(new File(outputDir, "b").exists());
        assertTrue(new File(outputDir.getAbsolutePath() + "/b/c.txt").exists());
    }

    @Test
    void deleteDirectory() throws IOException {
        File tempDir = FileUtils.createTempDir();
        Files.writeString(new File(tempDir, "a.txt").toPath(), "test", UTF_8);
        new File(tempDir, "b").mkdir();
        Files.writeString(new File(tempDir.getAbsolutePath() + "/b/c.txt").toPath(),
            "test", UTF_8);

        FileUtils.deleteDirectory(tempDir);

        assertFalse(tempDir.exists());
    }

    @Test
    void createTempDirWithName() throws IOException {
        File tempDir = FileUtils.createTempDir("abc");

        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
        assertEquals("abc", tempDir.getName());
    }

    @Test
    void rewriteWithReplacement() throws IOException {
        File tempFile = FileUtils.createTempFile("a\nb\nc", UTF_8);
        FileUtils.rewriteLine(tempFile, "b", "d", UTF_8);

        assertEquals("a\nd\nc\n", Files.readString(tempFile.toPath(), UTF_8));
    }

    @Test
    void expandFilePath() {
        assertEquals("/tmp/test.txt", FileUtils.expandUser("/tmp/test.txt").getAbsolutePath());
        assertEquals(System.getProperty("user.home") + "/Desktop/test.txt",
            FileUtils.expandUser("~/Desktop/test.txt").getAbsolutePath());
    }

    @Test
    public void testFileExtensionFilter() {
        FilenameFilter filter = FileUtils.getFileExtensionFilter("jpg", "png");

        assertTrue(filter.accept(new File("/tmp"), "test.jpg"));
        assertTrue(filter.accept(new File("/tmp"), "test.png"));
        assertTrue(filter.accept(new File("/tmp"), "test.PNG"));
        assertFalse(filter.accept(new File("/tmp"), "test.txt"));
        assertFalse(filter.accept(new File("/tmp"), "test.jpg.txt"));
    }

    @Test
    void countDirectorySize(@TempDir File tempDir) throws IOException {
        Files.writeString(new File(tempDir, "1.txt").toPath(), "some text", UTF_8);
        Files.writeString(new File(tempDir, "2.txt").toPath(), "some more text", UTF_8);

        assertEquals(23L, FileUtils.countDirectorySize(tempDir));
    }

    @Test
    void mkdirWithName(@TempDir File tempDir) throws IOException {
        File dir = FileUtils.mkdir(tempDir, "test");

        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
        assertEquals("test", dir.getName());
    }

    @Test
    void formatFileSize() {
        assertEquals("0 bytes", FileUtils.formatFileSize(0L));
        assertEquals("500 bytes", FileUtils.formatFileSize(500L));
        assertEquals("1 KB", FileUtils.formatFileSize(1100L));
        assertEquals("1.0 MB", FileUtils.formatFileSize(1_000_000L));
        assertEquals("1.2 MB", FileUtils.formatFileSize(1_234_000L));
        assertEquals("19.4 MB", FileUtils.formatFileSize(19_400_000L));
        assertEquals("123.4 MB", FileUtils.formatFileSize(123_400_000L));
        assertEquals("1.2 GB", FileUtils.formatFileSize(1_234_000_000L));
        assertEquals("123.5 GB", FileUtils.formatFileSize(123_456_789_000L));
    }

    @Test
    void walkFiles(@TempDir File tempDir) throws IOException {
        FileUtils.write("a", UTF_8, new File(tempDir, "a.txt"));
        FileUtils.write("b", UTF_8, new File(tempDir, "b.txt"));
        FileUtils.write("c", UTF_8, new File(tempDir, "c.txt"));
        File d = FileUtils.mkdir(tempDir, "d");
        FileUtils.write("d1", UTF_8, new File(tempDir, "d1.txt"));

        List<File> files = FileUtils.walkFiles(tempDir, f -> !f.getName().startsWith("c"));

        assertEquals(3, files.size());
        assertEquals("a.txt", files.get(0).getName());
        assertEquals("b.txt", files.get(1).getName());
        assertEquals("d1.txt", files.get(2).getName());
    }
}
