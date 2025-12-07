//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
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
import java.util.logging.Logger;

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

    private static final Splitter PROPERTY_LINE_SPLITTER = Splitter.on(CharMatcher.anyOf("=:"))
        .trimResults()
        .omitEmptyStrings()
        .limit(2);

    private static final CharMatcher PROPERTY_SPECIAL_CHARS = CharMatcher.anyOf("=:\n\\");
    private static final Logger LOGGER = LogHelper.getLogger(PropertyUtils.class);

    private PropertyUtils() {
    }

    /**
     * Parses the contents of a {@code .properties} file and returns the
     * resulting {@link Properties} object.
     *
     * @throws ResourceException if an I/O error occurs while reading the file.
     *
     * @deprecated This method was created to offer a cross-platform way
     *             of loading {@code .properties} file with UTF-8 character
     *             encoding. TeaVM now supports {@link Properties#load(Reader)},
     *             meaning this method is no longer necessary.
     */
    @Deprecated
    public static Properties loadProperties(Reader source) {
        try (source) {
            Properties properties = new Properties();
            properties.load(source);
            return properties;
        } catch (IOException e) {
            throw new ResourceException("I/O error while reading .properties file", e);
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
     * {@code .properties} file using the UTF-8 character encoding.
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
        return loadProperties(new StringReader(contents));
    }

    /**
     * Serializes a {@code Properties} object to the specified writer. This method
     * differs from the standard {@link Properties#store(Writer, String)}, in that
     * it writes the properties in a deterministic (alphabetical) order.
     *
     * @throws IOException if an I/O error occurs while writing. 
     */
    public static void saveProperties(Properties data, Writer dest) throws IOException {
        List<String> names = data.stringPropertyNames().stream()
            .sorted()
            .toList();

        try (PrintWriter writer = new PrintWriter(dest)) {
            for (String name : names) {
                String encodedName = PROPERTY_SPECIAL_CHARS.removeFrom(name);
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
