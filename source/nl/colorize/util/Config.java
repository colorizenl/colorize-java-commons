//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Generalized mechanism for parsing text-based configuration properties to
 * various data typse, including support for optional properties with default
 * values.
 * <p>
 * There are many cases where configuration properties are defined in a
 * text-based format but need to be parsed into the appropriate data type.
 * Examples are command line arguments, application preferences, and file
 * formats that do not support different data types (with CSV files and
 * {@code .properties} files being commonly used examples). In these
 * situations, this class can be used to standardize how text-based property
 * values are deserialized, rather than applications having to parse
 * every property individually. This use case is strictly about <em>read</em>
 * access, write access or dynamic configurations are not supported.
 * <p>
 * By default, this class provides deserialization for the following types:
 * <p>
 * <ul>
 *   <li>int / Integer</li>
 *   <li>long / Long</li>
 *   <li>float / Float</li>
 *   <li>double / Double</li>
 *   <li>boolean / Boolean</li>
 *   <li>String</li>
 *   <li>{@link java.util.Date} (date format detection based on {@link DateParser})</li>
 *   <li>{@link java.time.LocalDate} (date format detection based on {@link DateParser})</li>
 *   <li>{@link java.time.LocalDateTime} (date format detection based on {@link DateParser})</li>
 *   <li>{@link java.io.File}</li>
 *   <li>{@link java.nio.file.Path}</li>
 *   <li>{@link java.util.UUID}</li>
 *   <li>{@link nl.colorize.util.Version}</li>
 * </ul>
 * <p>
 * Support for additional types can be added by providing a custom
 * deserialization function for each type.
 * <p>
 * This class does <em>not</em> use reflection, which means it can also be used
 * in environments where reflection is not allowed or not supported. However,
 * this class can also be embedded in other mechanisms that <em>do</em> rely on
 * reflection, allowing for a less low-level and more convenient usage in
 * situations where reflection is allowed.
 */
public abstract class Config {

    private Map<Class<?>, Function<String, ?>> typeMappers;

    private static final Logger LOGGER = LogHelper.getLogger(Config.class);

    public Config() {
        this.typeMappers = Platform.isTeaVM() ? new HashMap<>() : new ConcurrentHashMap<>();
        registerDefaultTypeMappers();
    }

    private void registerDefaultTypeMappers() {
        registerTypeMapper(String.class, value -> value);
        registerTypeMapper(boolean.class, value -> value.equalsIgnoreCase("true"));
        registerTypeMapper(Boolean.class, value -> value.equalsIgnoreCase("true"));
        registerTypeMapper(int.class, Integer::parseInt);
        registerTypeMapper(Integer.class, Integer::parseInt);
        registerTypeMapper(long.class, Long::parseLong);
        registerTypeMapper(Long.class, Long::parseLong);
        registerTypeMapper(float.class, Float::parseFloat);
        registerTypeMapper(Float.class, Float::parseFloat);
        registerTypeMapper(double.class, Double::parseDouble);
        registerTypeMapper(Double.class, Double::parseDouble);
        registerTypeMapper(Date.class, DateParser::parse);
        registerTypeMapper(File.class, FileUtils::expandUser);
        registerTypeMapper(Path.class, value -> Path.of(FileUtils.expandUser(value).getAbsolutePath()));
        registerTypeMapper(UUID.class, UUID::fromString);
        registerTypeMapper(LocalDate.class, value -> DateParser.parseLocalDate(value));
        registerTypeMapper(LocalDateTime.class, value -> DateParser.parseLocalDateTime(value));
        registerTypeMapper(Version.class, Version::parse);
    }

    /**
     * Adds support for deserializing configuration properties to the data
     * type described by {@code type}. The type needs to be an exact match,
     * registering for an entire type hierarchy is not supported.
     *
     * @throws IllegalStateException if a type mapper for the same type
     *         has already been registered.
     */
    public <T> void registerTypeMapper(Class<T> type, Function<String, T> typeMapper) {
        Preconditions.checkState(!typeMappers.containsKey(type),
            "Type mapper already registered for type: " + type);

        typeMappers.put(type, typeMapper);
    }

    /**
     * Returns the raw value for the property with the specified key from the
     * underlying configuration. May return {@code null} or an empty string if
     * the property is not available or undefined.
     */
    protected abstract String getRawValue(String key);

    /**
     * Attempts to deserialize the property with the specified key into the
     * data type described by {@code type}. Returns an empty optional if the
     * property does not exist, if its value is {@code null}, or if its value
     * is an empty string.
     * <p>
     * If the type mapper throws an exception, this exception is forwarded to
     * the caller of this method. For example, integers are parsed using
     * {@link Integer#parseInt(String)}. If calling this method results in a
     * {@link NumberFormatException}, that exception will be rethrown by this
     * method.
     *
     * @throws NoSuchElementException if the property with the specified key
     *         does not exist, if its value is {@code null}, or if its value
     *         is an empty string.
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     * @throws NullPointerException if the property key is {@code null}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<T> parse(String key, Class<T> type) {
        Preconditions.checkNotNull(key, "Null property key");

        String rawValue = getRawValue(key);
        Function<String, ?> typeMapper = typeMappers.get(type);

        if (type.isEnum()) {
            return Optional.of((T) Enum.valueOf((Class) type, rawValue.toUpperCase()));
        } else if (typeMapper == null) {
            throw new UnsupportedOperationException("No type mapper registered for " + type);
        } else if (rawValue == null || rawValue.isEmpty()) {
            return Optional.empty();
        } else {
            T parsedValue = (T) typeMapper.apply(rawValue);
            return Optional.of(parsedValue);
        }
    }

    /**
     * Deserializes the property with the specified key into the data type
     * described by {@code type}. The property is assumed to be required,
     * an exception will be thrown if the property does not exist, if its
     * value is {@code null}, or if its value is an empty string.
     * <p>
     * If the type mapper throws an exception, this exception is forwarded to
     * the caller of this method. For example, integers are parsed using
     * {@link Integer#parseInt(String)}. If calling this method results in a
     * {@link NumberFormatException}, that exception will be rethrown by this
     * method.
     *
     * @throws NoSuchElementException if the property with the specified key
     *         does not exist, if its value is {@code null}, or if its value
     *         is an empty string.
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     * @throws NullPointerException if the property key is {@code null}.
     */
    public <T> T get(String key, Class<T> type) {
        return parse(key, type)
            .orElseThrow(() -> new NoSuchElementException("Missing property: " + key));
    }

    /**
     * Deserializes the property with the specified key into the data type
     * described by {@code type}. The property is assumed to be optional,
     * the default value will be returned if the property does not exist,
     * if its value is {@code null}, or if its value is an empty string.
     * <p>
     * If the type mapper throws an exception, the default value will be
     * returned. The exception will <em>not</em> be forwarded to the caller
     * of this method.
     *
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     * @throws NullPointerException if the property key is {@code null}.
     */
    public <T> T get(String key, Class<T> type, T defaultValue) {
        try {
            return parse(key, type).orElse(defaultValue);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid value for property '" + key + "': " + e.getMessage());
            return defaultValue;
        }
    }

    //--------------------------------------------
    // Convenience methods for standard data types
    //--------------------------------------------

    public String getString(String key) {
        return get(key, String.class);
    }

    public String getString(String key, String defaultValue) {
        return get(key, String.class, defaultValue);
    }

    public int getInt(String key) {
        return get(key, int.class);
    }

    public int getInt(String key, int defaultValue) {
        return get(key, int.class, defaultValue);
    }

    public long getLong(String key) {
        return get(key, long.class);
    }

    public long getLong(String key, long defaultValue) {
        return get(key, long.class, defaultValue);
    }

    public double getDouble(String key) {
        return get(key, double.class);
    }

    public double getDouble(String key, double defaultValue) {
        return get(key, double.class, defaultValue);
    }

    public boolean getBool(String key) {
        return get(key, boolean.class);
    }

    public boolean getBool(String key, boolean defaultValue) {
        return get(key, boolean.class, defaultValue);
    }

    //----------------------------------------
    // Factory methods
    //----------------------------------------

    /**
     * Creates a {@link Config} that directly parses the input string, without
     * needing to retrieve the property value from anywhere. Using this
     * factory method is equivalent to {@code Config.from(key -> key)}.
     */
    public static Config raw() {
        return from(key -> key);
    }

    /**
     * Creates a {@link Config} from a function that returns a text-based
     * value for the specified key. The function can return either
     * {@code null} or an empty string to indicate missing properties.
     * The function is also allowed to throw exceptions when retrieving
     * a property, the exceptions will be rethrown when retrieving the
     * property from the resulting {@link Config} instance.
     */
    public static Config from(Function<String, String> provider) {
        return new Config() {
            @Override
            protected String getRawValue(String key) {
                return provider.apply(key);
            }
        };
    }

    /**
     * Creates a {@link Config} from the specified map. Entries in the map
     * that are {@code null} or empty are treated as absent.
     */
    public static Config from(Map<String, String> map) {
        return from(map::get);
    }

    /**
     * Creates a {@link Config} from the specified properties. Entries that
     * are {@code null} or empty are treated as absent.
     */
    public static Config from(Properties properties) {
        return from(properties::getProperty);
    }

    /**
     * Creates a {@link Config} that will consider every property to be
     * nonexistent. This means that every required property will result in
     * an exception, and every optional property will result in the default
     * value.
     */
    public static Config empty() {
        return from(_ -> null);
    }
}
