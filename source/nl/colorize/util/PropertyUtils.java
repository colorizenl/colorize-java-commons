//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
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

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utility class for working with {@link Properties} and {@code .properties}
 * files. Although {@code .properties} files are widely used in Java
 * applications, the standard API for working with them is relatively limited.
 * This class adds a number of convenience methods to more easily load
 * {@code .properties} from multiple sources, to manipulate {@link Properties}
 * instances, and to serialize the results.
 */
public final class PropertyUtils {

    private static final CharMatcher PROPERTY_NAME = CharMatcher.anyOf("=:\n\\");

    private PropertyUtils() {
    }

    /**
     * Loads a properties file from a reader and returns the resulting
     * {@link Properties} object.
     * <p>
     * For {@code .properties} files containing non-ASCII characters, the
     * behavior of this method is different depending on the platform.
     * Originally, {@code .properties} files were required to use the
     * ISO-8859-1 character encoding, with non-ASCII characters respresented
     * by Unicode escape sequences in the form {@code uXXXX}. Modern versions
     * of the {@code .properties} file format allow any character encoding to
     * be used, but this is still not supported on all platforms, and by all
     * Java implementations.
     * <p>
     * This method will load the {@code .properties} file from a reader. If
     * the current platform supports {@code .properties} files with any
     * character encoding, the reader's character encoding is used to load
     * the file. If the platform only supports {@code .properties} files with
     * the ISO-8859-1 character encoding, this encoding is used as a fallback,
     * regardless of the reader's actual character encoding. On such platforms,
     * files that contain non-ASCII characters will not be loaded correctly.
     * <p>
     * The reader is closed by this method after the {@code .properties} file
     * has been loaded.
     *
     * @throws ResourceException if an I/O error occurs while reading the file.
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

    /**
     * Emulates loading {@code .properties} files that contain non-ASCII
     * characters, on platforms that do not support loading such files
     * natively. The documentation {@link #loadProperties(Reader)} contains
     * more information on how and when this is used.
     */
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
     * Uses the logic described in {@link #loadProperties(Reader)} to load a
     * {@code .properties} file, using the specified character encoding.
     *
     * @throws ResourceException if an I/O error occurs while reading the file.
     */
    public static Properties loadProperties(ResourceFile file, Charset charset) {
        try (Reader reader = file.openReader(charset)) {
            return loadProperties(reader);
        } catch (IOException e) {
            throw new ResourceException("Cannot read properties file from " + file, e);
        }
    }

    /**
     * Uses the logic described in {@link #loadProperties(Reader)} to load a
     * {@code .properties} file, using the UTF-8 character encoding.
     *
     * @throws ResourceException if an I/O error occurs while reading the file.
     */
    public static Properties loadProperties(ResourceFile file) {
        return loadProperties(file, UTF_8);
    }

    /**
     * Uses the logic described in {@link #loadProperties(Reader)} to load a
     * {@code .properties} file, using the specified character encoding.
     *
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public static Properties loadProperties(File source, Charset charset) throws IOException {
        try (Reader reader = Files.newReader(source, charset)) {
            return loadProperties(reader);
        }
    }

    /**
     * Uses the logic described in {@link #loadProperties(Reader)} to load a
     * {@code .properties} file, using the UTF-8 character encoding.
     *
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public static Properties loadProperties(File source) throws IOException {
        return loadProperties(source, UTF_8);
    }

    /**
     * Uses the logic described in {@link #loadProperties(Reader)} to load
     * {@code .properties} file contents from a string.
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
     * Serializes a {@code Properties} object to the specified writer. This method
     * differs from the standard {@link java.util.Properties#store(Writer, String)}
     * in that it writes the properties in a deterministic (alphabetical) order.
     * The writer is closed afterward.
     *
     * @throws IOException if an I/O error occurs while writing. 
     */
    public static void saveProperties(Properties data, Writer dest) throws IOException {
        List<String> names = data.stringPropertyNames().stream()
            .sorted()
            .toList();

        try (PrintWriter writer = new PrintWriter(dest)) {
            for (String name : names) {
                String encodedName = PROPERTY_NAME.removeFrom(name);
                String value = data.getProperty(name, "");
                writer.println(encodedName + "=" + value);
            }
        }
    }
    
    /**
     * Uses the logic from {@link #saveProperties(Properties, Writer)} to
     * serialize a {@link Properties} instance to a {@code .properties} file.
     * The file is created using the specified character encoding.
     *
     * @throws IOException if an I/O error occurs while writing the file.
     */
    public static void saveProperties(Properties data, File dest, Charset charset) throws IOException {
        PrintWriter writer = new PrintWriter(dest, charset.displayName());
        saveProperties(data, writer);
    }

    /**
     * Uses the logic from {@link #saveProperties(Properties, Writer)} to
     * serialize a {@link Properties} instance to a {@code .properties} file.
     * The file is created using the UTF-8 character encoding.
     *
     * @throws IOException if an I/O error occurs while writing the file.
     */
    public static void saveProperties(Properties data, File dest) throws IOException {
        saveProperties(data, dest, UTF_8);
    }

    /**
     * Uses the logic from {@link #saveProperties(Properties, Writer)} to
     * serialize a {@link Properties} instance to {@code .properties} file
     * contents.
     */
    public static String serializeProperties(Properties data) {
        try (StringWriter buffer = new StringWriter()) {
            saveProperties(data, buffer);
            return buffer.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
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
     *
     * @throws IllegalArgumentException if the provided prefix is empty.
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
}
