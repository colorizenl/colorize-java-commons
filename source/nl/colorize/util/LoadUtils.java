//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class to load and save data of various types. Data can be loaded from
 * a number of different sources. Typically, all methods in this class that work
 * with binary data use a {@link java.io.InputStream} and methods that work with
 * text use a {@link java.io.Reader}.
 */
public final class LoadUtils {

    private static final CharMatcher PROPERTY_NAME = CharMatcher.anyOf("=:\n\\");

    private LoadUtils() { 
    }

    /**
     * Reads the first N lines. Depending on the size of the input the number of
     * lines might not be reached. The stream is closed afterwards.
     *
     * @throws IOException if an I/O error occurs while reading.
     * @throws IllegalArgumentException if {@code n} is less than 1.
     */
    public static String readFirstLines(Reader source, int n) throws IOException {
        Preconditions.checkArgument(n >= 1, "Invalid number of lines: " + n);

        String lineSeparator = System.lineSeparator();

        try (BufferedReader buffer = toBufferedReader(source)) {
            return buffer.lines()
                .limit(n)
                .collect(Collectors.joining(lineSeparator));
        }
    }

    private static BufferedReader toBufferedReader(Reader reader) {
        if (reader instanceof BufferedReader) {
            return (BufferedReader) reader;
        } else {
            return new BufferedReader(reader);
        }
    }
    
    /**
     * Loads a properties file from a reader and returns the resulting
     * {@link Properties} object.
     * <p>
     * For {@code .properties} files containing non-ASCII characters, the
     * behavior of this method is different depending on the platform.
     * Originally, {@code .properties} files were required to use the ISO-8859-1
     * character encoding, with non-ASCII characters respresented by Unicode
     * escape sequences in the form {@code uXXXX}. Modern versions of the
     * {@code .properties} file format allow any character encoding to be used,
     * but this is still not supported on all platforms by all Java
     * implementations.
     * <p>
     * This method will load the {@code .properties} file from a reader. If the
     * current platform supports {@code .properties} files with any character
     * encoding, the reader's character encoding is used to load the file. If
     * the platform only supports {@code .properties} files with the ISO-8859-1
     * character encoding, this encoding is used as a fallback, regardless of
     * the reader's actual character encoding. On such platforms, files that
     * contain non-ASCII characters will not be loaded correctly.
     * <p>
     * The reader is closed by this method after the {@code .properties} file
     * has been loaded.
     *
     * @throws ResourceException if an I/O error occurs while reading the
     *         properties file.
     */
    public static Properties loadProperties(Reader source) {
        Properties properties = new Properties();

        try (source) {
            if (isUnicodePropertiesFilesSupported()) {
                properties.load(source);
            } else {
                String contents = CharStreams.toString(source);
                emulateUnicodeProperties(contents, properties);
            }
        } catch (IOException e) {
            throw new ResourceException("I/O error while reading .properties file", e);
        }

        return properties;
    }

    private static boolean isUnicodePropertiesFilesSupported() {
        return !Platform.isTeaVM();
    }

    @VisibleForTesting
    protected static void emulateUnicodeProperties(String contents, Properties properties) {
        Splitter lineSplitter = Splitter.on("\n").trimResults();
        List<String> trimmedLines = lineSplitter.splitToList(contents);
        String merged = Joiner.on("\n").join(trimmedLines).replace("\\\n", "");
        List<String> lines = lineSplitter.splitToList(merged);

        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.startsWith("#") && line.contains("=")) {
                String name = line.substring(0, line.indexOf("="));
                String value = line.substring(line.indexOf("=") + 1);
                properties.setProperty(name, value);
            }
        }
    }

    /**
     * Loads a {@code .properties file} using the specified character encoding.
     * Parsing the file is done using {@link LoadUtils#loadProperties(Reader)}.
     *
     * @throws ResourceException if an I/O error occurs while reading the
     *         properties file.
     */
    public static Properties loadProperties(ResourceFile file, Charset charset) {
        try (Reader reader = file.openReader(charset)) {
            return loadProperties(reader);
        } catch (IOException e) {
            throw new ResourceException("Cannot read properties file from " + file, e);
        }
    }

    /**
     * Loads a {@code .properties file} that is assumed to use the UTF-8 character
     * encoding. Parsing the file is done using
     * {@link LoadUtils#loadProperties(Reader)}.
     *
     * @throws ResourceException if an I/O error occurs while reading the
     *         properties file.
     */
    public static Properties loadProperties(ResourceFile file) {
        return loadProperties(file, UTF_8);
    }

    /**
     * Loads a properties file from a local file. Parsing the file is done using
     * {@link LoadUtils#loadProperties(Reader)}.
     *
     * @throws IOException if an I/O error occurs while reading the properties
     *         file.
     */
    public static Properties loadProperties(File source, Charset charset) throws IOException {
        try (Reader reader = Files.newReader(source, charset)) {
            return loadProperties(reader);
        }
    }

    /**
     * Loads a properties file from a string containing the file contents.
     * Parsing the file is done using {@link LoadUtils#loadProperties(Reader)}.
     * <p>
     * This method will load the {@code .properties} file from a reader. If the
     * current platform supports {@code .properties} files with any character
     * encoding, the reader's character encoding is used to load the file. If
     * the platform only supports {@code .properties} files with the ISO-8859-1
     * character encoding, this encoding is used as a fallback, regardless of
     * the reader's actual character encoding. On such platforms, files that
     * contain non-ASCII characters will not be loaded correctly.
     */
    public static Properties loadProperties(String contents) {
        if (isUnicodePropertiesFilesSupported()) {
            try (StringReader reader = new StringReader(contents)) {
                return loadProperties(reader);
            }
        } else {
            Properties properties = new Properties();
            emulateUnicodeProperties(contents, properties);
            return properties;
        }
    }

    /**
     * Creates a {@link Properties} object from a map. Any {@code null} keys
     * and/or values in the map will not be registered as properties.
     */
    public static Properties toProperties(Map<String, String> data) {
        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                properties.setProperty(entry.getKey(), entry.getValue());
            }
        }

        return properties;
    }

    /**
     * Creates a map from a {@link Properties} object. Any {@code null} or empty
     * property values will not be included in the map.
     */
    public static Map<String, String> toMap(Properties properties) {
        Map<String, String> result = new HashMap<>();

        for (String property : properties.stringPropertyNames()) {
            String value = properties.getProperty(property, null);
            if (value != null && !value.isEmpty()) {
                result.put(property, value);
            }
        }

        return result;
    }

    /**
     * Returns a new {@link Properties} object that only includes properties
     * from the original that have a name matching the specified prefix.
     * <p>
     * The property names in the result instance will have the prefix removed
     * from their name. For example, if the original included a property "a.x",
     * and the prefix is "a.", the result will include a property named "x".
     */
    public static Properties filterPrefix(Properties original, String prefix) {
        Preconditions.checkArgument(!prefix.isEmpty(), "Empty prefix");

        Properties filtered = new Properties();

        for (String name : original.stringPropertyNames()) {
            if (name.startsWith(prefix) && !name.equals(prefix)) {
                String nameWithoutPrefix = name.substring(prefix.length());
                String value = original.getProperty(name);
                filtered.put(nameWithoutPrefix, value);
            }
        }

        return filtered;
    }

    /**
     * Serializes a {@code Properties} object to the specified writer. This method
     * differs from the standard {@link java.util.Properties#store(Writer, String)}
     * in that it writes the properties in a deterministic (alphabetical) order.
     * The writer is closed afterward.
     *
     * @throws IOException if an I/O error occurs while writing. 
     */
    public static void saveProperties(Properties properties, Writer dest) throws IOException {
        List<String> names = properties.stringPropertyNames().stream()
            .sorted()
            .toList();

        try (PrintWriter writer = new PrintWriter(dest)) {
            for (String name : names) {
                String encodedName = PROPERTY_NAME.removeFrom(name);
                String value = properties.getProperty(name, "");
                writer.println(encodedName + "=" + value);
            }
        }
    }
    
    /**
     * Serializes a {@code Properties} object to the specified file. This method
     * is different from the standard {@link java.util.Properties#store(Writer, String)}
     * in that it writes the properties in a consistent and predictable order.
     *
     * @throws IOException if an I/O error occurs while writing. 
     */
    public static void saveProperties(Properties properties, File dest, Charset charset)
            throws IOException {
        PrintWriter writer = new PrintWriter(dest, charset.displayName());
        saveProperties(properties, writer);
    }

    /**
     * Seralizes a {@code Properties} object to its text representation, which
     * would normally serve as the contents of the {@code .properties} file.
     */
    public static String serializeProperties(Properties properties) {
        try (StringWriter buffer = new StringWriter()) {
            saveProperties(properties, buffer);
            return buffer.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
