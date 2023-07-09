//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Generalized mechanism for taking text-based properties and parsing them into
 * various types. There are many cases where property values are defined in a
 * text-based format, but need to be parsed into the appropriate data type.
 * Examples are command line arguments, application preferences, configuration,
 * or file formats that do not support different data types (e.g. CSV files).
 * In these situations, this class can be used to standardize how text-based
 * property values are deserialized, rather than applications having to parse
 * every property individually.
 * <p>
 * By default, this class provides deserialization for the following types:
 * <p>
 * <ul>
 *   <li>int/Integer</li>
 *   <li>long/Long</li>
 *   <li>float/Float</li>
 *   <li>double/Double</li>
 *   <li>boolean/Boolean</li>
 *   <li>String</li>
 *   <li>Date (assuming the ISO 8601 date format)</li>
 *   <li>File</li>
 *   <li>UUID</li>
 *   <li>Version</li>
 * </ul>
 * <p>
 * Support for additional types can be added by defining custom deserialization
 * behavior.
 * <p>
 * This class does <em>not</em> use reflection, which means it can also be used
 * in environments where reflection is not allowed or not supported. However,
 * this class can also be embedded in other mechanisms that <em>do</em> rely on
 * reflection, allowing for a less low-level and more convenient usage in
 * situations where reflection is allowed.
 */
public class PropertyDeserializer {

    private Map<Class<?>, Function<String, ?>> typeMappers;
    private List<Function<String, String>> preprocessors;

    private static final Logger LOGGER = LogHelper.getLogger(PropertyDeserializer.class);

    public PropertyDeserializer() {
        this.typeMappers = new HashMap<>();
        this.preprocessors = new ArrayList<>();

        registerDefaultTypeMappers();
    }

    private void registerDefaultTypeMappers() {
        register(String.class, value -> value);
        register(boolean.class, value -> value.equalsIgnoreCase("true"));
        register(Boolean.class, value -> value.equalsIgnoreCase("true"));
        register(int.class, Integer::parseInt);
        register(Integer.class, Integer::parseInt);
        register(long.class, Long::parseLong);
        register(Long.class, Long::parseLong);
        register(float.class, Float::parseFloat);
        register(Float.class, Float::parseFloat);
        register(double.class, Double::parseDouble);
        register(Double.class, Double::parseDouble);
        register(Date.class, DateParser::parse);
        register(File.class, FileUtils::expandUser);
        register(UUID.class, UUID::fromString);
        register(Version.class, Version::parse);
    }

    /**
     * Registers the specified function as a type mapper. Once registered, this
     * class will use the type mapper when it encounters properties that match
     * the specified type.
     *
     * @throws IllegalArgumentException if a type mapper has already been
     *         registered for the same type.
     */
    public <T> void register(Class<T> type, Function<String, T> typeMapper) {
        Preconditions.checkArgument(!typeMappers.containsKey(type),
            "Cannot override type mapper for " + type);

        typeMappers.put(type, typeMapper);
    }

    /**
     * Registers a function that should be used to pre-process property values
     * <em>before</em> trying to deserialize those values. It is possible to
     * register multiple preprocessors, which will then be used in order.
     */
    public void registerPreprocessor(Function<String, String> preprocessor) {
        preprocessors.add(preprocessor);
    }

    private String preprocessValue(String value) {
        for (Function<String, String> preprocessor : preprocessors) {
            value = preprocessor.apply(value);
        }
        return value;
    }

    /**
     * Deserializes a text-based property to the specified type.
     * <p>
     * If the type mapper throws an exception, this exception is forwarded to
     * the caller of this method. For example, integers are parsed using
     * {@link Integer#parseInt(String)}. If calling this method results in a
     * {@link NumberFormatException}, that exception will be rethrown by this
     * method.
     *
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     * @throws NullPointerException when trying to deserialize a null value.
     */
    public <T> T parse(String value, Class<T> type) {
        Preconditions.checkNotNull(value, "Cannot deserialize null value");

        if (!typeMappers.containsKey(type)) {
            throw new UnsupportedOperationException("No type mapper registered for  " + type);
        }

        String preprocessedValue = preprocessValue(value);
        return (T) typeMappers.get(type).apply(preprocessedValue);
    }

    /**
     * Attempts to deserialize a text-based property to the specified type,
     * but returns a default value when parsing results in an exception or
     * when the value is {@code null}.
     *
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     */
    public <T> T parse(String value, Class<T> type, T defaultValue) {
        return attempt(value, type).orElse(defaultValue);
    }

    /**
     * Attempts to deserialize a text-based property to the specified type,
     * but returns an empty optional when parsing results in an exception or
     * when the value is {@code null}.
     *
     * @throws UnsupportedOperationException if no type mapper has been
     *         registered for the specified type.
     */
    public <T> Optional<T> attempt(String value, Class<T> type) {
        if (value == null || preprocessValue(value) == null) {
            return Optional.empty();
        }

        try {
            T parsedValue = parse(value, type);
            return Optional.of(parsedValue);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Cannot deserialize '" + value + "' to " + type.getSimpleName());
            return Optional.empty();
        }
    }
}
