//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingMap;

import java.io.File;
import java.io.Reader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Interface for accessing one of the properties in a key/value store. The
 * default implementation of the interface assumes properties are stored as
 * strings and provides various methods for deserializing the property to
 * various data types. That said, the interface can also be used for key/value
 * stores that support multiple data types, which can override the default
 * implementation to return the property's "native" data type instead of first
 * having to parse it from a string.
 * <p>
 * When accessing the property value, there is a difference between required
 * properties and optional properties. Accessing a required property (via
 * {@link #getString()}) will throw an exception if the property is missing,
 * {@code null}, or an empty string. Accessing a missing/null/empty optional
 * property (via {@link #getStringOr(String)}) will return the provided default
 * value instead. This example relates to string properties, but the same rules
 * are applied when accessing non-string properties.
 * <p>
 * This interface is for <em>accessing</em> properties. If the key/value store
 * also allows <em>modifying</em> properties, classes that implement the
 * interface need to provide methods for this.
 */
public interface Property {

    /**
     * Represents a missing property. Accessing this property will always
     * result in an exception for required properties, and will always result
     * in the default value for optional properies.
     */
    public static final Property MISSING = () -> Optional.empty();

    /**
     * Returns the (string) value of this property as an {@link Optional}. The
     * optional will be empty if the property value is missing, {@code null},
     * or empty.
     */
    public Optional<String> getValue();

    /**
     * Returns the string value for a required property, or throws an
     * {@link NoSuchElementException} if the property is missing, {@code null},
     * or empty.
     */
    default String getString() {
        return getValue().orElseThrow(() -> new NoSuchElementException("Missing property"));
    }

    /**
     * Returns the string value for an optional property, or the provided
     * default value if the property is missing, {@code null}, or empty.
     */
    default String getStringOr(String defaultValue) {
        return getValue().orElse(defaultValue);
    }

    default int getInt() {
        return Integer.parseInt(getString());
    }

    default int getIntOr(int defaultValue) {
        return getValue().isPresent() ? getInt() : defaultValue;
    }

    default long getLong() {
        return Long.parseLong(getString());
    }

    default long getLongOr(long defaultValue) {
        return getValue().isPresent() ? getLong() : defaultValue;
    }

    default float getFloat() {
        return Float.parseFloat(getString());
    }

    default float getFloatOr(float defaultValue) {
        return getValue().isPresent() ? getFloat() : defaultValue;
    }

    default double getDouble() {
        return Double.parseDouble(getString());
    }

    default double getDoubleOr(double defaultValue) {
        return getValue().isPresent() ? getDouble() : defaultValue;
    }

    default boolean getBoolean() {
        return getString().equalsIgnoreCase("true");
    }

    default boolean getBooleanOr(boolean defaultValue) {
        return getValue().isPresent() ? getBoolean() : defaultValue;
    }

    default boolean getBool() {
        return getBoolean();
    }

    default boolean getBoolOr(boolean defaultValue) {
        return getBooleanOr(defaultValue);
    }

    default Date getDate(String dateFormat) {
        return DateParser.parse(getString(), dateFormat);
    }

    default Date getDateOr(Date defaultValue, String dateFormat) {
        return getValue().isPresent() ? getDate(dateFormat) : defaultValue;
    }

    default UUID getUUID() {
        return UUID.fromString(getString());
    }

    default UUID getUUIDOr(UUID defaultValue) {
        return getValue().isPresent() ? getUUID() : defaultValue;
    }

    default File getFile() {
        return FileUtils.expandUser(getString());
    }

    default File getFileOr(File defaultValue) {
        return getValue().isPresent() ? getFile() : defaultValue;
    }

    default File getDir() {
        File dir = getFile();
        Preconditions.checkArgument(dir.isDirectory(), "Not a directory: " + dir.getAbsolutePath());
        return dir;
    }

    default File getDirOr(File defaultValue) {
        return getValue().isPresent() ? getDir() : defaultValue;
    }

    default Version getVersion() {
        try {
            return Version.parse(getString());
        } catch (IllegalArgumentException e) {
            Logger logger = LogHelper.getLogger(Property.class);
            logger.warning("Unable to parse property value '" + getString() + "' as version");
            return Version.UNKNOWN;
        }
    }

    default Version getVersionOr(Version defaultValue) {
        return getValue().isPresent() ? getVersion() : defaultValue;
    }

    /**
     * Returns a {@link Property} based on the specified value. Providing a
     * {@code null} or empty string will return {@link #MISSING}.
     */
    public static Property of(String value) {
        if (value != null && !value.isEmpty()) {
            return () -> Optional.of(value);
        } else {
            return MISSING;
        }
    }

    /**
     * Returns a {@link Property} that allows {@code null} values, even for
     * required properties.
     *
     * @deprecated This method exists only for backward compatibility with old
     *             code that allows {@code null} values.
     */
    @Deprecated
    public static Property allowNull() {
        return new Property() {
            @Override
            public Optional<String> getValue() {
                return Optional.empty();
            }

            @Override
            public String getString() {
                return null;
            }
        };
    }

    /**
     * Returns a property map based on the underlying map, making it easier to
     * parse non-string properties. The map will return {@link #MISSING} when
     * trying to access a nonexistent property.
     */
    public static Map<String, Property> from(Map<String, String> properties) {
        Map<String, Property> propertyMap = new HashMap<>();
        properties.forEach((key, value) -> propertyMap.put(key, of(value)));

        return new ForwardingMap<>() {
            @Override
            protected Map<String, Property> delegate() {
                return propertyMap;
            }

            @Override
            public Property get(Object key) {
                Property value = delegate().get(key);
                return value != null ? value : MISSING;
            }
        };
    }

    /**
     * Returns a property map based on an underlying {@link Properties}
     * instance, making it easier to parse non-string properties. The map will
     * return {@link #MISSING} when trying to access a nonexistent property.
     */
    public static Map<String, Property> from(Properties properties) {
        Map<String, String> propertyMap = LoadUtils.toMap(properties);
        return from(propertyMap);
    }

    /**
     * Convenience method that parses a {@code .properties} file and then
     * returns a property map based on the resulting {@link Properties}
     * instance. The file is loaded using
     * {@link LoadUtils#loadProperties(ResourceFile)}. The map will return
     * {@link #MISSING} when trying to access a nonexistent property.
     */
    public static Map<String, Property> from(ResourceFile propertiesFile) {
        Preconditions.checkArgument(propertiesFile.getName().endsWith(".properties"),
            propertiesFile + " is not a .properties file");

        Properties properties = LoadUtils.loadProperties(propertiesFile);
        return from(properties);
    }

    /**
     * Convenience method that parses the contents of a {@code .properties}
     * file and then returns a property map based on the resulting
     * {@link Properties} instance. The map will return {@link #MISSING} when
     * trying to access a nonexistent property. The contents of the file are
     * parsed using {@link LoadUtils#loadProperties(Reader)}.
     */
    public static Map<String, Property> from(Reader propertiesFile) {
        Properties properties = LoadUtils.loadProperties(propertiesFile);
        return from(properties);
    }
}
