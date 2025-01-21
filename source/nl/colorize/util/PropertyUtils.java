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
import nl.colorize.util.stats.Tuple;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
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
     * <p>
     * This method will use a different implementation depending on the
     * platform. On platforms that do not support the standar
     * {@link Properties#load(Reader)}, such as TeaVM, a custom
     * implementation is used in order to support UTF-8 property files.
     *
     * @throws ResourceException if an I/O error occurs while reading the file.
     */
    public static Properties loadProperties(Reader source) {
        try (source) {
            if (Platform.isTeaVM()) {
                return emulateLoadProperties(source);
            } else {
                return loadPropertiesReflection(source);
            }
        } catch (IOException e) {
            throw new ResourceException("I/O error while reading .properties file", e);
        }
    }

    /**
     * Loads a {@code .properties} file from a {@link Reader} instead of from
     * an {@link InputStream} (which only supports ISO-8859-1). This needs to
     * use reflection, as TeaVM is otherwise unable to transpile this class.
     */
    private static Properties loadPropertiesReflection(Reader source) {
        try {
            Properties properties = new Properties();
            Method loadMethod = properties.getClass().getMethod("load", Reader.class);
            loadMethod.invoke(properties, source);
            return properties;
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Properties.load(Reader) not supported", e);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ResourceException("Error while calling Properties.load(Reader)", e);
        }
    }

    /**
     * Custom implementation for parsing {@code .properties} files. See the
     * documentation for {@link #loadProperties(Reader)} for more information
     * on when and why this is used.
     */
    protected static Properties emulateLoadProperties(Reader source) throws IOException {
        Properties properties = new Properties();

        try (BufferedReader buffer = new BufferedReader(source)) {
            List<String> rawLines = buffer.lines().toList();
            List<String> processedLines = mergeMultiLineStrings(rawLines);

            processedLines.stream()
                .map(line -> parsePropertyFileLine(line))
                .filter(property -> property != null)
                .forEach(property -> properties.setProperty(property.left(), property.right()));
        }

        return properties;
    }

    private static Tuple<String, String> parsePropertyFileLine(String line) {
        List<String> fields = PROPERTY_LINE_SPLITTER.splitToList(line);
        if (fields.size() < 2) {
            LOGGER.warning("Invalid property file line: " + line.trim());
            return null;
        }
        return Tuple.of(fields.get(0), fields.get(1));
    }

    private static boolean filterPropertyFileLine(String line) {
        return !line.trim().isEmpty() &&
            !line.startsWith("#") &&
            !line.startsWith("!");
    }

    private static String normalizePropertyFileLine(String line) {
        return TextUtils.removeTrailing(line.trim(), "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\f", "\f");
    }

    private static List<String> mergeMultiLineStrings(List<String> raw) {
        List<String> processed = new ArrayList<>();
        boolean inMultiLine = false;

        for (String line : raw) {
            if (filterPropertyFileLine(line)) {
                if (inMultiLine) {
                    String lastLine = processed.removeLast();
                    line = lastLine + line.trim();
                }

                processed.add(normalizePropertyFileLine(line));
                inMultiLine = line.trim().endsWith("\\");
            }
        }

        return processed;
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
