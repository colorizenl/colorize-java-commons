//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for loading data from and saving data to files. Unless stated
 * otherwise, all methods in this class will throw an {@code IOException} if
 * an I/O error occurs while interacting with the file.
 * <p>
 * When using Java 7 or newer, some of the convenience methods in this class
 * are no longer needed and can be replaced with {@link java.nio.file.Files}.
 */
public final class FileUtils {

    private static final Logger LOGGER = LogHelper.getLogger(FileUtils.class);

    private FileUtils() {
    }
    
    /**
     * Reads a file's binary contents and returns them as a byte array.
     */
    public static byte[] read(File source) throws IOException {
        return Files.readAllBytes(source.toPath());
    }
    
    /**
     * Reads a file's textual contents using the specified character encoding.
     */
    public static String read(File source, Charset encoding) throws IOException {
        return new String(read(source), encoding);
    }
    
    /**
     * Reads a file's textual contents using the specified character encoding,
     * and returns a list of lines.
     */
    public static List<String> readLines(File source, Charset encoding) throws IOException {
        return Files.readAllLines(source.toPath(), encoding);
    }
    
    /**
     * Reads a file's first N lines, or all lines if the file contains less.
     */
    public static List<String> readFirstLines(File source, Charset encoding, int n) throws IOException {
        Preconditions.checkArgument(n >= 1, "Invalid number of lines: " + n);
        
        List<String> lines = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(source), encoding))) {
            while (true) {
                String line = reader.readLine();
                if (line != null && lines.size() < n) {
                    lines.add(line);
                } else {
                    break;
                }
            }
        }
        
        return lines;
    }
    
    /**
     * Writes the specified data to a file. If the file already exists its contents
     * will be replaced.
     */
    public static void write(byte[] data, File dest) throws IOException {
        Files.write(dest.toPath(), data);
    }
    
    /**
     * Writes the specified text to a file. If the file already exists its contents
     * will be replaced.
     */
    public static void write(String text, Charset encoding, File dest) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(dest), encoding)) {
            writer.write(text);
            writer.flush();
        }
    }
    
    /**
     * Writes the specified lines to a file. If the file already exists its contents
     * will be replaced.
     */
    public static void write(List<String> lines, Charset encoding, File dest) throws IOException {
        Files.write(dest.toPath(), lines, encoding);
    }

    /**
     * Reads all lines in a file, then used the provided callback function to
     * rewrite the lines. The result is then used to overwrite the original file.
     */
    public static void rewrite(File file, Charset charset, Function<String, String> lineProcessor)
            throws IOException {
        List<String> originalLines = FileUtils.readLines(file, charset);

        try (PrintWriter writer = new PrintWriter(file, charset)) {
            for (String line : originalLines) {
                writer.println(lineProcessor.apply(line));
            }
        }
    }

    /**
     * Reads all lines in a file, then replaced all matching lines with the
     * specified replacement. The result is then used to overwrite the original
     * file.
     */
    public static void rewriteLine(File file, String original, String replacement, Charset charset)
            throws IOException {
        rewrite(file, charset, line -> {
            if (line.trim().equalsIgnoreCase(original.trim())) {
                return replacement;
            } else {
                return line;
            }
        });
    }

    /**
     * Creates a directory. Unlike {@link File#mkdir()}, an exception will be
     * thrown if the directory could not be created. If the directory already 
     * exists this method does nothing.
     */
    public static void mkdir(File dir) throws IOException {
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new IOException("Cannot create directory " + dir.getAbsolutePath());
            }
        }
    }
    
    /**
     * Deletes a file. Unlike {@link File#delete()}, an exception will be thrown
     * if the file could not be deleted. If the file does not exist this method
     * does nothing.
     */
    public static void delete(File file) throws IOException {
        if (file.exists()) {
            if (!file.delete()) {
                throw new IOException("Cannot delete file " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Copies a directory to the specified location.
     *
     * @throws IllegalStateException if the target directory already exists.
     * @throws IOException if an I/O error occurs while copying the files.
     */
    public static void copyDirectory(File source, File target) throws IOException {
        Preconditions.checkState(!target.exists(),
            "Target directory already exists: " + target.getAbsolutePath());

        List<Path> contents = Files.walk(source.toPath())
            .collect(Collectors.toList());

        for (Path childPath : contents) {
            Path relativePath = source.toPath().relativize(childPath);
            Path targetPath = target.toPath().resolve(relativePath);
            Files.copy(childPath, targetPath, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /**
     * Deletes a directory and all of its contents. If the directory does not
     * exist this method does nothing.
     *
     * @throws IOException if an I/O error occurs while deleting.
     * @throws IllegalArgumentException if the argument is not a directory.
     */
    public static void deleteDirectory(File dir) throws IOException {
        Preconditions.checkArgument(!dir.isFile(),
            dir.getAbsolutePath() + " is not a directory");

        // Always log when deleting directories, for traceability
        // reasons.
        LOGGER.info("Deleting directory " + dir.getAbsolutePath());

        for (File child : getDirectoryContents(dir)) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            } else {
                delete(child);
            }
        }

        delete(dir);
    }

    private static List<File> getDirectoryContents(File dir) {
        File[] contents = dir.listFiles();
        if (contents == null) {
            return Collections.emptyList();
        }
        return ImmutableList.copyOf(contents);
    }

    /**
     * Shorthand for {@code Files.walk} to iterate over files in a directory,
     * with only files matching the filter being returned.
     */
    public static List<File> walkFiles(File dir, Predicate<File> filter) throws IOException {
        return Files.walk(dir.toPath())
            .map(Path::toFile)
            .filter(file -> !file.isDirectory() && filter.test(file))
            .collect(Collectors.toList());
    }

    /**
     * Returns {@code file}'s path relative to {@code base}. For example, the path
     * "/a/b/c/d.txt" relative to "/a/b" would return "c/d.txt". If both paths do
     * not share a common base {@code file}'s absolute path will be returned.
     *
     * @deprecated Use {@code Path.relativize} instead. This can be used even when
     *             when working with APIs still using {@link File} instances:
     *             {@code base.toPath().relativize(file.toPath)}.
     */
    @Deprecated
    public static String getRelativePath(File file, File base) {
        String path = file.getAbsolutePath();
        String basePath = base.getAbsolutePath();
        
        if (path.equals(basePath) || !path.startsWith(basePath)) {
            return path;
        }
        
        String relativePath = path.substring(basePath.length());
        if (relativePath.charAt(0) == '/' || relativePath.charAt(0) == '\\') {
            relativePath = relativePath.substring(1);
        }
        
        return relativePath;
    }

    /**
     * Returns the file extension of the specified file, excluding the dot. So
     * the file "test.png" would return "png". Note that the file extension will
     * be returned as lowercase, even if the original file name was not. If the
     * file does not have a file extension this will return an aempty string.
     */
    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.indexOf('.') <= 0) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Creates a temporary file without any contents. The file will be created in
     * the platform's default location for storing temporary files.
     */
    private static File createTempFile() throws IOException {
        return Files.createTempFile("temp-" + System.currentTimeMillis(), ".tmp").toFile();
    }
    
    /**
     * Creates a temporary file with the specified contents. The file will be
     * created in the platform's default location for storing temporary files.
     */
    public static File createTempFile(byte[] data) throws IOException {
        File tempFile = createTempFile();
        write(data, tempFile);
        return tempFile;
    }
    
    /**
     * Creates a temporary file with the specified text as contents. The file 
     * will be created in the platform's default location for storing temporary
     * files.
     */
    public static File createTempFile(String text, Charset encoding) throws IOException {
        File tempFile = createTempFile();
        write(text, encoding, tempFile);
        return tempFile;
    }
    
    /**
     * Creates a temporary directory within the platform's default location for
     * storing temporary files.
     */
    public static File createTempDir() throws IOException {
        return Files.createTempDirectory("temp-" + System.currentTimeMillis()).toFile();
    }

    /**
     * Creates a temporary directory with the specified name. The directory will
     * be created in the platform's default location for storing temporary files.
     */
    public static File createTempDir(String name) throws IOException {
        File parentDir = createTempDir();
        File tempDir = new File(parentDir, name);
        mkdir(tempDir);
        return tempDir;
    }
}
