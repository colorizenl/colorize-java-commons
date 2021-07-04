//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Extension of {@link Properties} that can be used to load and save application
 * data. Storage is limited to simple key/value pairs, but convenience methods
 * are available to parse the values as common data types.
 */
public class ApplicationData {

    private Properties properties;

    private static final Splitter LIST_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();
    private static final Joiner LIST_JOINER = Joiner.on(",").skipNulls();
    private static final Logger LOGGER = LogHelper.getLogger(ApplicationData.class);

    /**
     * Creates an {@code ApplicationData} instance by wrapping existing
     * properties.
     */
    public ApplicationData(Properties properties) {
        this.properties = properties;
    }

    /**
     * Creates an {@code ApplicationData} instance by wrapping an existing map.
     */
    public ApplicationData(Map<String, String> properties) {
        this.properties = new Properties();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            this.properties.setProperty(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Loads application data from the specified file.
     * @throws IOException if the file could not be found or read.
     */
    public ApplicationData(File file) throws IOException {
        this.properties = LoadUtils.loadProperties(file, Charsets.UTF_8);
    }

    /**
     * Loads application data from the specified file.
     */
    public ApplicationData(ResourceFile file) {
        this.properties = LoadUtils.loadProperties(file, Charsets.UTF_8);
    }

    /**
     * Loads application data from the specified {@code .properties} file
     * contents.
     */
    public ApplicationData(String properties) {
        try {
            this.properties = LoadUtils.loadProperties(new StringReader(properties));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void remove(String key) {
        properties.remove(key);
    }

    public void clear() {
        properties.clear();
    }

    /**
     * Saves this application data back to a {@code .properties} file.
     * @throws IOException if the file could not be saved.
     */
    public void save(File file) throws IOException {
        LoadUtils.saveProperties(properties, file, Charsets.UTF_8);
    }

    /**
     * Serializes this application data back to a {@code .properties} file.
     */
    public String serialize() {
        StringWriter buffer = new StringWriter();
        try {
            LoadUtils.saveProperties(properties, buffer);
            return buffer.toString();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    // Convenience methods

    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public int get(String key, int defaultValue) {
        return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    }

    public void set(String key, long value) {
        set(key, String.valueOf(value));
    }

    public long get(String key, long defaultValue) {
        return Long.parseLong(get(key, String.valueOf(defaultValue)));
    }

    public void set(String key, float value) {
        set(key, String.valueOf(value));
    }

    public float get(String key, float defaultValue) {
        return Float.parseFloat(get(key, String.valueOf(defaultValue)));
    }

    public void set(String key, double value) {
        set(key, String.valueOf(value));
    }

    public double get(String key, double defaultValue) {
        return Double.parseDouble(get(key, String.valueOf(defaultValue)));
    }

    public void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    public boolean get(String key, boolean defaultValue) {
        return get(key, String.valueOf(defaultValue)).equals("true");
    }

    public void set(String key, Date date) {
        set(key, String.valueOf(date.getTime()));
    }

    public Date get(String key, Date defaultValue) {
        long timestamp = Long.parseLong(get(key, String.valueOf(defaultValue.getTime())));
        return new Date(timestamp);
    }

    public void set(String key, Date date, String dateFormat) {
        String value = new SimpleDateFormat(dateFormat).format(date);
        set(key, value);
    }

    public Date get(String key, Date defaultValue, String dateFormat) {
        String value = get(key, "");
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return new SimpleDateFormat(dateFormat).parse(value);
        } catch (ParseException e) {
            LOGGER.warning("Cannot parse '" + value + "' using date format " + dateFormat);
            return defaultValue;
        }
    }

    public void set(String key, List<String> value) {
        set(key, LIST_JOINER.join(value));
    }

    public List<String> get(String key, List<String> defaultValue) {
        return LIST_SPLITTER.splitToList(get(key, LIST_JOINER.join(defaultValue)));
    }

    public Map<String, String> toMap() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String property : properties.stringPropertyNames()) {
            result.put(property, properties.getProperty(property));
        }
        return result;
    }
}
