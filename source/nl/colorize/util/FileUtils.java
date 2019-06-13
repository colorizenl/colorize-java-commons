//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Utility class for loading data from and saving data to files. Unless stated
 * otherwise, all methods in this class will throw an {@code IOException} if
 * an I/O error occurs while interacting with the file.
 * <p>
 * When using Java 7 or newer, most of the convenience methods in this class
 * are obsolete and can be replaced with {@link java.nio.file.Files}.
 */
public final class FileUtils {

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
     * Returns {@code file}'s path relative to {@code base}. For example, the path
     * "/a/b/c/d.txt" relative to "/a/b" would return "c/d.txt". If both paths do
     * not share a common base {@code file}'s absolute path will be returned.
     */
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

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.indexOf('.') <= 0) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    
    private static File createTempFile() throws IOException {
        return Files.createTempFile("temp-" + System.currentTimeMillis(), ".tmp").toFile();
    }
    
    /**
     * Creates a temporary file with the specified contents. The file will be
     * located in the platform's default temp file directory.
     * @return A reference to the temporary file that was created.
     */
    public static File createTempFile(byte[] data) throws IOException {
        File tempFile = createTempFile();
        write(data, tempFile);
        return tempFile;
    }
    
    /**
     * Creates a temporary file with the specified text as contents. The file 
     * will be located in the platform's default temp file directory.
     * @return A reference to the temporary file that was created.
     */
    public static File createTempFile(String text, Charset encoding) throws IOException {
        File tempFile = createTempFile();
        write(text, encoding, tempFile);
        return tempFile;
    }
    
    /**
     * Creates a temporary directory within the platform's default directory
     * for storing temporary files.
     * @return A reference to the directory that was created.
     */
    public static File createTempDir() throws IOException {
        return Files.createTempDirectory("temp-" + System.currentTimeMillis()).toFile();
    }
}
