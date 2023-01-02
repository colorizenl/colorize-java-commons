//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Wrapper around {@link Properties}, representing text-based name/value
 * properties that can be persisted from or to a {@code .properties} file.
 * <p>
 * The wrapper adds a number of methods to deserialize the text-based property
 * values to different data types. Since property values are text-based, empty
 * strings and {@code null} valued are considered equivalent.
 * <p>
 * The wrapper provides a read-only interface, even though the underlying
 * {@link Properties} object is mutable. {@code .properties} files are usually
 * used for configuration, localization, or application data. The number of
 * scenarios where properties need to be read/serialized/parsed therefore
 * greatly outnumbers the scenarios where properties need to be changed.
 */
public interface AppProperties {

    /**
     * Accesses the underlying {@link Properties}.
     */
    public Properties getProperties();

    /**
     * Returns the names of all properties. This will also include properties
     * that are present, but do not have a value.
     */
    default Set<String> getPropertyNames() {
        return getProperties().stringPropertyNames();
    }

    /**
     * Returns {@code true} if a property with the specified name exists, and
     * that property has a non-{@code null} and non-empty value.
     * <p>
     * <strong>Implementation note:</strong> The default implementation of this
     * method calls {@link #get(String, String)} to obtain the property value.
     */
    default boolean hasProperty(String name) {
        String value = get(name, null);
        return value != null && !value.isEmpty();
    }

    /**
     * Returns the value of the property with the specified name. Unlike
     * {@link #get(String, String)}, there is no default value and trying to
     * retrieve a non-existent property will result in an exception being
     * thrown.
     * <p>
     * <strong>Implementation note:</strong> The default implementation of this
     * method calls {@link #get(String, String)} to obtain the property value.
     *
     * @throws IllegalArgumentException if no property with the specified name
     *         exists, or if the property's value is {@code null} or empty.
     */
    default String get(String name) {
        String value = get(name, null);
        Preconditions.checkArgument(name != null && !name.isEmpty(),
            "Missing value for required property '" + name + "')");
        return value;
    }

    /**
     * Returns the value of the property with the specified name. If the
     * property is missing or empty, the default value will be returned
     * instead.
     */
    default String get(String name, String defaultValue) {
        String value = getProperties().getProperty(name, null);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    default int getInt(String name) {
        return Integer.parseInt(get(name));
    }

    default int getInt(String name, int defaultValue) {
        return hasProperty(name) ? getInt(name) : defaultValue;
    }

    default long getLong(String name) {
        return Long.parseLong(get(name));
    }

    default long getLong(String name, long defaultValue) {
        return hasProperty(name) ? getLong(name) : defaultValue;
    }

    default float getFloat(String name) {
        return Float.parseFloat(get(name));
    }

    default float getFloat(String name, float defaultValue) {
        return hasProperty(name) ? getFloat(name) : defaultValue;
    }

    default double getDouble(String name) {
        return Double.parseDouble(get(name));
    }

    default double getDouble(String name, double defaultValue) {
        return hasProperty(name) ? getDouble(name) : defaultValue;
    }

    default boolean getBoolean(String name) {
        return get(name).equalsIgnoreCase("true");
    }

    default boolean getBoolean(String name, boolean defaultValue) {
        return hasProperty(name) ? getBoolean(name) : defaultValue;
    }

    default boolean getBool(String name) {
        return getBoolean(name);
    }

    default boolean getBool(String name, boolean defaultValue) {
        return getBoolean(name, defaultValue);
    }

    default Date getDate(String name, String dateFormat) {
        String value = get(name);
        try {
            return new SimpleDateFormat(dateFormat).parse(value);
        } catch (ParseException e) {
            throw new IllegalArgumentException(value + " does not match date format " + dateFormat);
        }
    }

    default Date getDate(String name, Date defaultValue, String dateFormat) {
        return hasProperty(name) ? getDate(name, dateFormat) : defaultValue;
    }

    default UUID getUUID(String name) {
        return UUID.fromString(get(name));
    }

    default UUID getUUID(String name, UUID defaultValue) {
        return hasProperty(name) ? getUUID(name) : defaultValue;
    }

    default File getFile(String name) {
        return FileUtils.expandUser(get(name));
    }

    default File getFile(String name, File defaultValue) {
        return hasProperty(name) ? getFile(name) : defaultValue;
    }

    default File getDir(String name) {
        File dir = getFile(name);
        Preconditions.checkArgument(dir.isDirectory(),
            "Not a directory: " + dir.getAbsolutePath());
        return dir;
    }

    default File getDir(String name, File defaultValue) {
        return hasProperty(name) ? getDir(name) : defaultValue;
    }

    /**
     * Returns all available properties as an immutable map. The map will not
     * include properties that do not have a value, i.e. only properties for
     * which {@link #hasProperty(String)} returns true will be included.
     */
    default Map<String, String> toPropertyMap() {
        return getPropertyNames().stream()
            .filter(this::hasProperty)
            .collect(Collectors.toMap(name -> name, this::get));
    }

    /**
     * Invokes the specified callback function for each property.
     */
    default void forEach(BiConsumer<AppProperties, String> callback) {
        for (String property : getPropertyNames()) {
            callback.accept(this, property);
        }
    }

    /**
     * Creates a new {@link AppProperties} instance that only returns properties
     * with a name matching the specified prefix.
     * <p>
     * The property names in the created instance will have the prefix removed
     * from their name. For example, if the original included a property "a.x",
     * and the prefix is "a.", the result will include a property named "x".
     */
    default AppProperties filterPrefix(String prefix) {
        Preconditions.checkArgument(!prefix.isEmpty(), "Empty prefix");

        Map<String, String> filtered = new HashMap<>();

        for (String name : getPropertyNames()) {
            if (name.startsWith(prefix) && !name.equals(prefix) && hasProperty(name)) {
                String nameWithoutPrefix = name.substring(prefix.length());
                String value = get(name);
                filtered.put(nameWithoutPrefix, value);
            }
        }

        return fromMap(filtered);
    }

    /**
     * Factory method that creates an {@link AppProperties} instance that wraps
     * around the specified {@link Properties}.
     */
    public static AppProperties from(Properties properties) {
        return () -> properties;
    }

    /**
     * Factory method that creates an {@link AppProperties} instance from the
     * specified map.
     */
    public static AppProperties fromMap(Map<String, String> data) {
        Properties properties = LoadUtils.toProperties(data);
        return from(properties);
    }

    /**
     * Factory method that loads a {@code .properties} file and then wraps the
     * resulting {@link Properties} in an {@link AppProperties} instance. The
     * file is loaded using {@link LoadUtils#loadProperties(Reader)}.
     *
     * @throws ResourceFileException if an I/O error occurs while reading the
     *         properties file.
     */
    public static AppProperties fromPropertiesFile(Reader reader) {
        Properties properties = LoadUtils.loadProperties(reader);
        return from(properties);
    }

    /**
     * Factory method that loads a {@code .properties} file and then wraps the
     * resulting {@link Properties} in an {@link AppProperties} instance. The
     * file is loaded using {@link LoadUtils#loadProperties(ResourceFile)}, and
     * is assumed to use the UTF-8 character encoding.
     *
     * @throws ResourceFileException if an I/O error occurs while reading the
     *         properties file.
     */
    public static AppProperties fromPropertiesFile(ResourceFile file) {
        Properties properties = LoadUtils.loadProperties(file);
        return from(properties);
    }

    /**
     * Factory method that loads a {@code .properties} file and then wraps the
     * resulting {@link Properties} in an {@link AppProperties} instance. The
     * file is loaded using {@link LoadUtils#loadProperties(Reader)}.
     *
     * @throws ResourceFileException if an I/O error occurs while reading the
     *         properties file.
     */
    public static AppProperties fromPropertiesFile(File file) {
        try {
            Properties properties = LoadUtils.loadProperties(file, Charsets.UTF_8);
            return from(properties);
        } catch (IOException e) {
            throw new ResourceFileException("Unable to load " + file.getAbsolutePath(), e);
        }
    }
}
