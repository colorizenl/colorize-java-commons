//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Utility class to load and save data of various types. Data can be loaded from
 * a number of different sources. Typically all methods in this class that work 
 * with binary data use a {@link java.io.InputStream} and methods that work with
 * text use a {@link java.io.Reader}. Unless specifically mentioned in the method 
 * documentation, the stream is closed directly after reading or writing from it. 
 */
public final class LoadUtils {
	
	private LoadUtils() { 
	}
	
	/**
	 * Reads all bytes from a stream and closes the stream afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static byte[] readToByteArray(InputStream stream) throws IOException {
		byte[] result = null;
		try {
			result = ByteStreams.toByteArray(stream);
		} finally {
			Closeables.close(stream, true);
		}
		return result;
	}
	
	/**
	 * Reads all characters from a stream and closes the stream afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static String readToString(InputStream stream, Charset charset) throws IOException {
		InputStreamReader reader = new InputStreamReader(stream, charset);
		return readToString(reader);
	}
	
	/**
	 * Reads all characters from a reader and closes the reader afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static String readToString(Reader reader) throws IOException {
		String result = null;
		try {
			result = CharStreams.toString(reader);
		} finally {
			Closeables.close(reader, true);
		}
		return result;
	}
	
	/**
	 * Reads all lines from a reader and closes the reader afterwards. If the
	 * text ends with a newline the last element in the returned list will be
	 * the line before that newline.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static List<String> readLines(Reader reader) throws IOException {
		List<String> result = null;
		try {
			result = CharStreams.readLines(reader);
		} finally {
			Closeables.close(reader, true);
		}
		return result;
	}
	
	/**
	 * Reads all lines from the specified resource file.
	 * @throws RuntimeException if an I/O error occurs while reading.
	 */
	public static List<String> readLines(ResourceFile file, Charset charset) {
		try {
			return readLines(file.openReader(charset));
		} catch (IOException e) {
			throw new RuntimeException("I/O error while reading file " + file, e);
		}
	}
	
	/**
	 * Reads the first N lines. Depending on the size of the input the number of
	 * lines might not be reached. The stream is closed afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 * @throws IllegalArgumentException if {@code n} is less than 1.
	 */
	public static String readFirstLines(Reader source, int n) throws IOException {
		if (n < 1) {
			throw new IllegalArgumentException("Invalid number of lines: " + n);
		}
		
		BufferedReader reader = toBufferedReader(source);
		List<String> lines = new ArrayList<String>();
		String line = null;
		
		try {
			while ((line = reader.readLine()) != null && lines.size() < n) {
				lines.add(line);
			}
		} finally {
			Closeables.close(reader, true);
		}
		
		return joinLines(lines);
	}
	
	/**
	 * Returns the last N lines. The other lines will be read (in order to locate
	 * the end) but not kept in memory. Depending on the size of the input the 
	 * number of lines might not be reached. The stream is closed afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 * @throws IllegalArgumentException if {@code n} is less than 1.
	 */
	public static String readLastLines(Reader source, int n) throws IOException {
		if (n < 1) {
			throw new IllegalArgumentException("Invalid number of lines: " + n);
		}

		BufferedReader reader = toBufferedReader(source);
		// Uses a LinkedList because an ArrayList has to be reshuffled
		// each time remove(0) is called.
		LinkedList<String> lines = new LinkedList<String>();
		String line = null;
		
		try {
			while ((line = reader.readLine()) != null) {
				if (lines.size() >= n) {
					lines.removeFirst();
				}
				lines.add(line);
			}
		} finally {
			Closeables.close(reader, true);
		}
		
		return joinLines(lines);
	}
	
	private static String joinLines(List<String> lines) {
		if (!lines.isEmpty()) {
			// Ensure that the the last line ends with a newline
			lines.add("");
		}
		return Joiner.on(Platform.getLineSeparator()).skipNulls().join(lines);
	}
	
	private static BufferedReader toBufferedReader(Reader reader) {
		if (reader instanceof BufferedReader) {
			return (BufferedReader) reader;
		} else {
			return new BufferedReader(reader);
		}
	}
	
	/**
	 * Loads a properties file from a reader. The reader is closed afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static Properties loadProperties(Reader source) throws IOException {
		Properties properties = new Properties();
		try {
			properties.load(source);
		} finally {
			Closeables.close(source, true);
		}
		return properties;
	}
	
	/**
	 * Loads a properties file from a stream, using the specified character encoding.
	 * The stream is closed afterwards.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static Properties loadProperties(InputStream stream, Charset charset) throws IOException {
		return loadProperties(new InputStreamReader(stream, charset));
	}
	
	/**
	 * Loads a properties file with the default charset of ISO-8859-1.
	 * @deprecated Use {@link #loadProperties(Reader)} instead.
	 */
	@Deprecated
	public static Properties loadProperties(InputStream input) throws IOException {
		Properties properties = new Properties();
		properties.load(input);
		return properties;
	}
	
	/**
	 * Loads a properties file from a resource file.
	 * @throws RuntimeException if the resource file could not be parsed.
	 */
	public static Properties loadProperties(ResourceFile file, Charset charset) {
		try {
			return loadProperties(file.openReader(charset));
		} catch (IOException e) {
			throw new RuntimeException("Cannot parse properties file", e);
		}
	}
	
	/**
	 * Loads a properties file from a local file.
	 * @throws IOException if the file could not be read.
	 */
	public static Properties loadProperties(File source, Charset charset) throws IOException {
		return loadProperties(Files.newReader(source, charset));
	}
	
	/**
	 * Creates a {@code Properties} object directly from a number of key/value
	 * pairs.
	 * @throws IllegalArgumentException if {@code rest.length} is not even.
	 */
	public static Properties toProperties(String key, String value, String... rest) {
		if (rest.length % 2 != 0) {
			throw new IllegalArgumentException("Needs an even number for key/value pairs");
		}
		
		Properties properties = new Properties();
		properties.setProperty(key, value);
		for (int i = 0; i < rest.length; i += 2) {
			properties.setProperty(rest[i], rest[i + 1]);
		}
		return properties;
	}
	
	/**
	 * Serializes a {@code Properties} object to the specified writer. This method
	 * is different from the standard {@link java.util.Properties#store(Writer, String)}
	 * in that it writes the properties in a consistent and predictable order. 
	 * The writer is closed afterwards. 
	 * @throws IOException if an I/O error occurs while writing. 
	 */
	public static void saveProperties(Properties properties, Writer dest) throws IOException {
		List<String> sortedNames = new ArrayList<String>();
		for (Object name : properties.keySet()) {
			sortedNames.add((String) name);
		}
		
		Collections.sort(sortedNames);
		
		PrintWriter writer = new PrintWriter(dest);
		try {
			for (String name : sortedNames) {
				writer.println(name + "=" + properties.getProperty(name));
			}
		} finally {
			Closeables.close(writer, true);
		}
	}
	
	/**
	 * Serializes a {@code Properties} object to the specified file. This method
	 * is different from the standard {@link java.util.Properties#store(Writer, String)}
	 * in that it writes the properties in a consistent and predictable order. 
	 * @throws IOException if an I/O error occurs while writing. 
	 */
	public static void saveProperties(Properties properties, File dest, Charset charset) throws IOException {
		PrintWriter writer = new PrintWriter(dest, charset.displayName());
		saveProperties(properties, writer);
	}
	
	private static String normalizeFileExtension(String ext) {
		if (ext.startsWith(".")) {
			ext = ext.substring(1);
		}
		return ext.toLowerCase();
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
		
		if ((relativePath.charAt(0) == '/') || (relativePath.charAt(0) == '\\')) {
			relativePath = relativePath.substring(1);
		}
		
		return relativePath;
	}
	
	/**
	 * Concatenates a (absolute or relative) directory path to a relative file 
	 * path. For example, the arguments "/a/b" and "c/d.txt" give "/a/b/c/d.txt".
	 * <p>
	 * This method operates on string level and should only be used when
	 * path concatenation of {@link java.io.File} or {@link java.net.URL} cannot 
	 * be used.
	 */
	public static String concatFilePaths(String dirPath, String relativePath) {
		if (isAbsolutePath(dirPath, relativePath)) {
			return relativePath;
		} else {
			return concatRelativeFilePaths(dirPath, relativePath);
		}
	}
	
	private static String concatRelativeFilePaths(String dirPath, String relativePath) {
		String separator = System.getProperty("file.separator");
		
		if (relativePath.startsWith(separator)) {
			relativePath = relativePath.substring(1);
		}
		
		if (dirPath.contains(separator) && dirPath.indexOf('.') > dirPath.indexOf(separator)) {
			dirPath = dirPath.substring(0, dirPath.lastIndexOf(separator));
		} else if (dirPath.endsWith(separator)) {
			dirPath = dirPath.substring(0, dirPath.length() - 1);
		}
		
		return dirPath + separator + relativePath;
	}

	private static boolean isAbsolutePath(String dirPath, String relativePath) {
		String separator = System.getProperty("file.separator");
		return dirPath.isEmpty() || (!dirPath.contains(separator) && !guessIfDirectory(dirPath));
	}

	private static boolean guessIfDirectory(String name) {
		if (!name.contains(".")) {
			return true;
		}
		String[] parts = name.split("\\.");
		return parts[parts.length - 1].length() != 3;
	}
	
	/**
	 * Returns a file filter that will only accept files that match one of the
	 * specified file extensions. The file extension check is case-insensitive.
	 * @param extensions File extensions to accept (without the leading dot).
	 * @throws IllegalArgumentException if the provided list is empty.
	 */
	public static FilenameFilter getFileExtensionFilter(String... extensions) {
		if (extensions.length == 0) {
			throw new IllegalArgumentException("No file extensions provided");
		}
		
		final List<String> extensionList = ImmutableList.copyOf(extensions);
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				String ext = Files.getFileExtension(name).toLowerCase();
				return extensionList.contains(ext);
			}
		};
	}
	
	/**
	 * Returns a file filter that accepts files with a name matching a glob 
	 * pattern. Refer to http://en.wikipedia.org/wiki/Glob_(programming) for
	 * a list of supported constructs. 
	 */
	public static FilenameFilter getGlobFilter(String globPattern) {
		final Pattern fileNamePattern = convertGlobPattern(globPattern);
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				Matcher nameMatcher = fileNamePattern.matcher(name);
				return nameMatcher.matches();
			}
		};
	}
	
	private static Pattern convertGlobPattern(String globPattern) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("\\Q");
		for (int i = 0; i < globPattern.length(); i++) {
			char c = globPattern.charAt(i);
			switch (c) {
				case '\\' : buffer.append(globPattern.charAt(++i)); break;
				case '?' : buffer.append("\\E.\\Q"); break;
				case '*' : buffer.append("\\E.*?\\Q"); break;
				case '[' : buffer.append("\\E["); break;
				case ']' : buffer.append("]\\Q"); break;
				default : buffer.append(c); break;
			}
		}
		buffer.append("\\E");
		// Strip off empty escape sequences
		String pattern = buffer.toString().replace("\\Q\\E", "");
		return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
	}

	/**
	 * Converts a {@link java.io.FilenameFilter} to a {@link java.io.FileFilter}.
	 */
	public static FileFilter toFileFilter(final FilenameFilter filter) {
		return new FileFilter() {
			public boolean accept(File file) {
				return filter.accept(file.getParentFile(), file.getName());
			}
		};
	}
	
	/**
	 * Converts a {@link java.io.FileFilter} to a {@link java.io.FilenameFilter}.
	 */
	public static FilenameFilter toFilenameFilter(final FileFilter filter) {
		return new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return filter.accept(new File(dir, name));
			}
		};
	}

	/**
	 * Creates a new and empty file at the location described by {@code file}.
	 * This wraps {@link java.io.File#createNewFile()} but uses an exception
	 * instead of a return value to indicate failure.
	 * @throws IOException if the file could not be created.
	 * @throws IllegalArgumentException if the file already exists.
	 */
	public static void createFile(File file) throws IOException {
		if (file.exists()) {
			throw new IllegalArgumentException("File already exists: " + file.getAbsolutePath());
		}
		
		boolean success = file.createNewFile();
		if (!success) {
			throw new IOException("Cannot create file: " + file.getAbsolutePath());
		}
	}
	
	/**
	 * Calls {@link java.io.File#mkdir()} and throws an exception if it fails. If
	 * the directory already exists this method does nothing.
	 * @throws IOException if the directory cannot be created.
	 */
	public static void mkdir(File dir) throws IOException {
		if (!dir.exists()) {
			boolean success = dir.mkdir();
			if (!success) {
				throw new IOException("Cannot create directory '" + dir.getAbsolutePath() + "'");
			}
		}
	}
	
	/**
	 * Calls {@link java.io.File#delete()} and throws an exception if it fails. 
	 * If the file does not exist this method does nothing.
	 * @throws IOException if the file could not be deleted.
	 */
	public static void delete(File file) throws IOException {
		if (file.exists()) {
			boolean success = file.delete();
			if (!success) {
				throw new IOException("Cannot delete file '" + file.getAbsolutePath() + "'");
			}
		}
	}
	
	/**
	 * Calls {@link java.io.File#renameTo(File)} and throws an exception if it
	 * fails.
	 * @throws IOException if the file could not be renamed.
	 * @throws IllegalArgumentException if the file does not exist, or if the
	 *         destination file already exists.
	 */
	public static void rename(File from, File to) throws IOException {
		checkRenamePreconditions(from, to);
		boolean success = from.renameTo(to);
		if (!success) {
			throw new IOException(String.format("Cannot rename '%s' to '%s'",
					from.getAbsolutePath(), to.getAbsolutePath()));
		}
	}
	
	private static void checkRenamePreconditions(File from, File to) {
		if (!from.exists() || from.isDirectory()) {
			throw new IllegalArgumentException("Not a file: " + from.getAbsolutePath());
		}
		
		if (to.exists() && from.getAbsolutePath().equals(to.getAbsolutePath())) {
			throw new IllegalArgumentException("The destination file already exists");
		}
	}

	/**
	 * Updates the "last modified" field of a file to the current date and time.
	 * @throws IllegalArgumentException if the file does not exist, or if updating
	 *         the file fails.
	 */
	public static void touch(File file) {
		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
		}
		
		// This intentionally uses currentTimeMillis() since that is
		// guaranteed to return the system value.
		boolean success = file.setLastModified(System.currentTimeMillis());
		if (!success) {
			throw new IllegalArgumentException("Could not change the last modified date/time: " +
					file.getAbsolutePath());
		}
	}
	
	/**
	 * Creates a URL from the specified string. This method can be used to create
	 * a {@link java.net.URL} without having to try/catch the checked excepton.
	 * @throws IllegalArgumentException if {@code url} is not a valid URL.
	 */
	public static URL toURL(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Malformed URL: " + url, e);
		}
	}
	
	/**
	 * Returns the URL for the specified local file.
	 * @throws IllegalArgumentException if no URL could be created for the file.
	 */
	public static URL toURL(File file) {
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Invalid URL: " + file);
		}
	}
	
	/**
	 * Returns the URI for the specified path and eats the exception.
	 * @throws IllegalArgumentException if {@code uri} is not a valid URI.
	 */
	public static URI toURI(String uri) {
		try {
			return new URI(uri);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid URL: " + uri, e);
		}
	}
	
	/**
	 * Calls {@code Closeables.close()} and logs any {@code IOException}s it 
	 * might throw.
	 */
	public static void closeQuietly(OutputStream stream) {
		try {
			Closeables.close(stream, true);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Calls {@code Closeables.close()} and logs any {@code IOException}s it 
	 * might throw.
	 */
	public static void closeQuietly(Writer writer) {
		try {
			Closeables.close(writer, true);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * Returns a temporary file that will be deleted when the JVM exits. The
	 * file will have a random name.
	 * @param extension Extension to give the file, or {@code null} for none.
	 */
	private static File getTempFile(File dir, String extension) {
		long timestamp = System.currentTimeMillis();
		long random = Math.round(Math.random() * 1000000L);
		
		String name = "temp_" + String.valueOf(timestamp) + String.valueOf(random);
		if ((extension != null) && (extension.length() > 0)) {
			name += "." + normalizeFileExtension(extension);
		}
		
		File tempFile = new File(dir, name);
		tempFile.deleteOnExit();
		return tempFile;
	}
	
	/**
	 * Returns a file in the platform's location for storing temporary files,
	 * that will be deleted when the JVM exits. The file will have a random
	 * name. Note that this method will not actually create the file, it will 
	 * just return a {@code File} object pointing to it.
	 * @throws UnsupportedOperationException if the current platform has no
	 *         directory for storing temporary files.
	 */
	public static File getTempFile(String extension) {
		File tempDir = Platform.getTempDirectory();
		return getTempFile(tempDir, extension);
	}
	
	/**
	 * Writes data to a temporary file that will be deleted when the JVM exits.
	 * @throws IOException if an I/O error occurs while writing.
	 */
	public static File createTempFile(File dir, InputStream data) throws IOException {
		File tempFile = getTempFile(dir, "");
		writeTempFile(tempFile, data);
		return tempFile;
	}
	
	/**
	 * Writes data to a temporary file that will be deleted when the JVM exits.
	 * The file is created in the platform's location for storing temporary files.
	 * @throws IOException if an I/O error occurs while writing.
	 */
	public static File createTempFile(InputStream data) throws IOException {
		File tempFile = getTempFile("");
		writeTempFile(tempFile, data);
		return tempFile;
	}
	
	private static void writeTempFile(File tempFile, InputStream data) throws IOException {
		FileOutputStream out = new FileOutputStream(tempFile);
		try {
			ByteStreams.copy(data, out);
		} finally {
			Closeables.close(data, true);
			Closeables.close(out, true);
		}
	}
	
	/**
	 * Writes text to a temporary file that will be deleted when the JVM exits.
	 * @throws IOException if an I/O error occurs while writing.
	 */
	public static File createTempFile(File dir, String text, Charset encoding) throws IOException {
		File tempFile = getTempFile(dir, "");
		Files.write(text, tempFile, encoding);
		return tempFile;
	}
	
	/**
	 * Writes text to a temporary file that will be deleted when the JVM exits.
	 * @param name Requested name for the file. If a file with that name already
	 *        exists an exception will be thrown.
	 * @throws IOException if an I/O error occurs while writing.
	 * @throws IllegalArgumentException if a file with the requested name already 
	 *         exists.
	 */
	public static File createTempFile(File dir, String name, String text, Charset encoding) 
			throws IOException {
		File tempFile = new File(dir, name);
		if (tempFile.exists()) {
			throw new IllegalArgumentException("File already exists: " + tempFile.getAbsolutePath());
		}
		tempFile.deleteOnExit();
		Files.write(text, tempFile, encoding);
		return tempFile;
	}
	
	/**
	 * Writes text to a temporary file that will be deleted when the JVM exits.
	 * The file is created in the platform's location for storing temporary files.
	 * @throws IOException if an I/O error occurs while writing.
	 */
	public static File createTempFile(String text, Charset encoding) throws IOException {
		File tempFile = getTempFile("");
		Files.write(text, tempFile, encoding);
		return tempFile;
	}
	
	/**
	 * Creates a temporary directory that will be deleted when the JVM exits.
	 * @throws IOException if the directory cannot be created.
	 */
	public static File createTempDir(File parent) throws IOException {
		File tempDir = getTempFile(parent, "");
		if (!tempDir.mkdir()) {
			throw new IOException("Cannot create temp directory");
		}
		return tempDir;
	}
	
	/**
	 * Creates a temporary directory that will be deleted when the JVM exits.
	 * The directory is created in the platform's location for storing temporary
	 * files.
	 * @throws IOException if the directory cannot be created.
	 */
	public static File createTempDir() throws IOException {
		File tempDir = getTempFile("");
		if (!tempDir.mkdir()) {
			throw new IOException("Cannot create temp directory");
		}
		return tempDir;
	}
}
