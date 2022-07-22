//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Access to application data, configuration, or preferences that is stored as
 * a number of key/value pairs. The purpose of this class is similar to
 * {@link Properties}, and in fact the default implementation wraps around a
 * {@link Properties} instance. However, it provides methods to save and load
 * preferences with data types other than strings, even though all values
 * are eventually still serialized to strings.
 */
public interface Configuration {

    public Set<String> getPropertyNames();

    default boolean hasProperty(String key) {
        return getPropertyNames().contains(key) && !get(key, "").isEmpty();
    }

    public void set(String key, String value);

    public String get(String key, String defaultValue);

    default String get(String key) {
        String value = get(key, "");
        Preconditions.checkArgument(value != null && !value.isEmpty(),
            "Missing required property " + key);
        return value;
    }

    public void remove(String key);

    /**
     * Serializes application data to string form, so that it can be saved to a
     * file or database or some other persistent storage.
     */
    public String serialize();

    /**
     * Deserializes key/value pairs from the specified source and adds them to
     * this instance.
     *
     * @throws IllegalArgumentException if the contents are serialized in a
     *         format that cannot be parsed by this instance.
     */
    public void deserialize(String contents);

    // Convenience methods

    default void setInt(String key, int value) {
        set(key, String.valueOf(value));
    }

    default int getInt(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    default int getInt(String key) {
        return Integer.parseInt(get(key));
    }

    default void setLong(String key, long value) {
        set(key, String.valueOf(value));
    }

    default long getLong(String key, long defaultValue) {
        return Long.parseLong(get(key, String.valueOf(defaultValue)));
    }

    default long getLong(String key) {
        return Long.parseLong(get(key));
    }

    default void setFloat(String key, float value) {
        set(key, String.valueOf(value));
    }

    default float getFloat(String key, float defaultValue) {
        return Float.parseFloat(get(key, String.valueOf(defaultValue)));
    }

    default float getFloat(String key) {
        return Float.parseFloat(get(key));
    }

    default void setDouble(String key, double value) {
        set(key, String.valueOf(value));
    }

    default double getDouble(String key, double defaultValue) {
        return Double.parseDouble(get(key, String.valueOf(defaultValue)));
    }

    default double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    default void setBoolean(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        return get(key, String.valueOf(defaultValue)).equals("true");
    }

    default boolean getBoolean(String key) {
        return get(key).equals("true");
    }

    default void setDate(String key, Date date, String dateFormat) {
        String value = new SimpleDateFormat(dateFormat).format(date);
        set(key, value);
    }

    default Date getDate(String key, Date defaultValue, String dateFormat) {
        String value = get(key, "");
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return new SimpleDateFormat(dateFormat).parse(value);
        } catch (ParseException e) {
            return defaultValue;
        }
    }

    default void setUUID(String key, UUID value) {
        set(key, value.toString());
    }

    default UUID getUUID(String key, UUID defaultValue) {
        return UUID.fromString(get(key, defaultValue.toString()));
    }

    default void setList(String key, List<String> value, char delimiter) {
        Joiner joiner = Joiner.on(delimiter).skipNulls();
        set(key, joiner.join(value));
    }

    default void setList(String key, List<String> value) {
        setList(key, value, ',');
    }

    default List<String> getList(String key, char delimiter) {
        String value = get(key, "");
        if (value == null || value.isEmpty()) {
            return Collections.emptyList();
        }
        Splitter splitter = Splitter.on(delimiter).omitEmptyStrings().trimResults();
        return splitter.splitToList(value);
    }

    default List<String> getList(String key) {
        return getList(key, ',');
    }

    /**
     * Filters all properties, and only returns properties with a name starting
     * with the specified prefix. The returned property keys will have the prefix
     * removed, so that only the part of the key *after* the prefix remains.
     */
    default void filterPrefix(String prefix, BiConsumer<String, String> callback) {
        for (String property : getPropertyNames()) {
            if (property.startsWith(prefix)) {
                String normalizedPropertyName = property.substring(prefix.length());
                String value = get(property);
                callback.accept(normalizedPropertyName, value);
            }
        }
    }

    /**
     * Application data implementation that wraps around an existing
     * {@link Properties} instance. Data can be serialized to and deserialized
     * from {@code .properties} files.
     */
    public static Configuration fromProperties(Properties properties) {
        return new Configuration() {
            @Override
            public Set<String> getPropertyNames() {
                return properties.stringPropertyNames();
            }

            @Override
            public void set(String key, String value) {
                properties.setProperty(key, value);
            }

            @Override
            public String get(String key, String defaultValue) {
                return properties.getProperty(key, defaultValue);
            }

            @Override
            public void remove(String key) {
                properties.remove(key);
            }

            @Override
            public String serialize() {
                return LoadUtils.serializeProperties(properties);
            }

            @Override
            public void deserialize(String contents) {
                Properties loaded = LoadUtils.loadProperties(contents);
                for (String property : loaded.stringPropertyNames()) {
                    properties.setProperty(property, loaded.getProperty(property));
                }
            }
        };
    }

    /**
     * Converts a map to a {@link Properties} instance, then wraps around that
     * instance to create implement this interface. Data can be serialized to
     * and deserialized from {@code .properties} files.
     */
    public static Configuration fromProperties(Map<String, ?> data) {
        return fromProperties(LoadUtils.toProperties(data));
    }

    /**
     * Application data implementation that wraps around a new {@link Properties}
     * instance. Data can be serialized to and deserialized from
     * {@code .properties} files.
     */
    public static Configuration fromProperties() {
        return fromProperties(new Properties());
    }
}
