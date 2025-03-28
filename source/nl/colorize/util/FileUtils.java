//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Utility class for loading data from and saving data to files. Unless stated
 * otherwise, all methods in this class will throw an {@code IOException} if
 * an I/O error occurs while interacting with the file.
 * <p>
 * When using Java 7 or newer, some of the convenience methods in this class
 * are no longer needed and can be replaced with {@link java.nio.file.Files}.
 */
public final class FileUtils {

    // Consistent with the definitions used by Apple in the Finder,
    // see https://discussions.apple.com/thread/7375024.
    private static final long KB = 1000L;
    private static final long MB = 1_000_000L;
    private static final long GB = 1_000_000_000L;
    private static final long TB = 1_000_000_000_000L;

    private static final Logger LOGGER = LogHelper.getLogger(FileUtils.class);

    private FileUtils() {
    }

    /**
     * Reads all lines in a file, then used the provided callback function to
     * rewrite the lines. The result is then used to overwrite the original file.
     */
    public static void rewrite(File file, Charset charset, Function<String, String> mapper)
            throws IOException {
        List<String> originalLines = Files.readAllLines(file.toPath(), charset);

        try (PrintWriter writer = new PrintWriter(file, charset)) {
            for (String line : originalLines) {
                writer.println(mapper.apply(line));
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
     * Expands a file path that contains a reference to the user's home
     * directory. For example, if the path is "~/Desktop/test.txt" and the
     * user home directory is "/home/john", the returned file will reference
     * "/home/john/Desktop/test.txt".
     *
     * @throws IllegalArgumentException if the path is empty.
     * @throws UnsupportedOperationException if the current platform does not
     *         provide or support a user home directory.
     */
    public static File expandUser(String path) {
        Preconditions.checkArgument(path != null && !path.isEmpty(), "Empty file path");

        if (path.startsWith("~/")) {
            try {
                File homeDir = Platform.getUserHomeDir();
                return new File(homeDir.getAbsolutePath() + "/" + path.substring(2));
            } catch (Exception e) {
                throw new UnsupportedOperationException("User home not supported on platform", e);
            }
        } else {
            return new File(path);
        }
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
     * Creates a new directory with the specified name and location, then
     * returns the directory that was just created.
     */
    public static File mkdir(File parentDir, String name) throws IOException {
        File dir = new File(parentDir, name);
        mkdir(dir);
        return dir;
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

        List<Path> contents = Files.walk(source.toPath()).toList();

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

    private static File[] getDirectoryContents(File dir) {
        File[] contents = dir.listFiles();
        if (contents == null) {
            return new File[0];
        }
        return contents;
    }

    /**
     * Shorthand for {@code Files.walk} to iterate over files in a directory,
     * with only files matching the filter being returned.
     */
    public static List<File> walkFiles(File dir, Predicate<File> filter) throws IOException {
        if (!dir.isDirectory()) {
            if (filter.test(dir)) {
                return Collections.singletonList(dir);
            } else {
                return Collections.emptyList();
            }
        }

        return Files.walk(dir.toPath())
            .map(Path::toFile)
            .filter(file -> !file.isDirectory() && filter.test(file))
            .sorted(Comparator.comparing(File::getAbsolutePath))
            .toList();
    }

    /**
     * Returns {@code file}'s path relative to {@code base}. For example, the path
     * "/a/b/c/d.txt" relative to "/a/b" would return "c/d.txt". If both paths do
     * not share a common base {@code file}'s absolute path will be returned.
     *
     * @deprecated Use {@code Path.relativize} instead. This can be used even when
     *             working with APIs still using {@link File} instances:
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
     * Returns the size of the specified directory, in bytes.
     */
    public static long countDirectorySize(File dir) throws IOException {
        Preconditions.checkArgument(dir.exists() && dir.isDirectory(),
            "No such directory: " + dir.getAbsolutePath());

        return Files.walk(dir.toPath())
            .map(Path::toFile)
            .filter(file -> !file.isDirectory())
            .mapToLong(File::length)
            .sum();
    }

    /**
     * Returns a human-readable description of the specified file's size. The
     * level of precision is automatically selected based on the value.
     */
    public static String formatFileSize(File file) {
        return formatFileSize(file.length());
    }

    /**
     * Returns a human-readable description of the specified file size in bytes.
     * The level of precision is automatically selected based on the value.
     */
    public static String formatFileSize(long fileSize) {
        Preconditions.checkArgument(fileSize >= 0L, "Invalid file size: " + fileSize);

        if (fileSize < KB) {
            return fileSize + " bytes";
        } else if (fileSize < MB) {
            return String.format("%.0f KB", fileSize / (double) KB);
        } else if (fileSize < GB) {
            return String.format("%.1f MB", fileSize / (double) MB);
        } else if (fileSize < TB) {
            return String.format("%.1f GB", fileSize / (double) GB);
        } else {
            return String.format("%.1f TB", fileSize / (double) TB);
        }
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
        Files.write(tempFile.toPath(), data);
        return tempFile;
    }
    
    /**
     * Creates a temporary file with the specified text as contents. The file 
     * will be created in the platform's default location for storing temporary
     * files.
     */
    public static File createTempFile(String text, Charset encoding) throws IOException {
        File tempFile = createTempFile();
        Files.writeString(tempFile.toPath(), text, encoding);
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
        return mkdir(parentDir, name);
    }
}
