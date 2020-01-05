//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Splitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Variant of {@link java.util.PropertyResourceBundle} that can load property
 * files from any location and with any character encoding. Also, it supports
 * {@link java.text.MessageFormat} notation in values, and does not throw 
 * exceptions when a key is missing. Finally, it is possible to let the bundle 
 * expire after a certain time, after which it will be reloaded.
 */
public class DynamicResourceBundle extends ResourceBundle {
    
    private Properties bundle;

    private static final Splitter LINE_SPLITTER = Splitter.on("\n").omitEmptyStrings().trimResults();

    /**
     * Looks up the resource bundle with the specified name and locale. If a
     * bundle for that locale is not available the bundle for the "base" locale
     * will be loaded instead. If that also can't be found an exception is thrown.
     * @param name Resource bundle name without the ".properties" or locale suffixes.
     * @throws MissingResourceException if the resource bundle cannot be located.
     */
    public DynamicResourceBundle(String name, ResourceFile dir, Locale locale, Charset charset) {
        ResourceFile countryBundle = new ResourceFile(dir, getCountryBundleName(name, locale));
        ResourceFile languageBundle = new ResourceFile(dir, getLanguageBundleName(name, locale));
        ResourceFile baseBundle = new ResourceFile(dir, getBaseBundleName(name));

        if (countryBundle.exists()) {
            this.bundle = LoadUtils.loadProperties(countryBundle, charset);
        } else if (languageBundle.exists()) {
            this.bundle = LoadUtils.loadProperties(languageBundle, charset);
        } else if (baseBundle.exists()) {
            this.bundle = LoadUtils.loadProperties(baseBundle, charset);
        } else {
            throw new MissingResourceException("Cannot locate resource bundle " + 
                    name, getClass().getName(), "");
        }
    }
    
    private String getCountryBundleName(String name, Locale locale) {
        return name + "_" + locale.toString() + ".properties";
    }
    
    private String getLanguageBundleName(String name, Locale locale) {
        return name + "_" + locale.getLanguage() + ".properties";
    }
    
    private String getBaseBundleName(String name) {
        return name + ".properties";
    }
    
    /**
     * Creates a resource bundle from the properties file located at the specified
     * location. The loaded bundle will initially be set to never expire.
     */
    public DynamicResourceBundle(ResourceFile sourceFile, Charset charset) {
        this.bundle = LoadUtils.loadProperties(sourceFile, charset);
    }
    
    /**
     * Creates a resource bundle based on an existing {@code ResourceBundle}.
     */
    public DynamicResourceBundle(ResourceBundle source) {
        this(toProperties(source));
    }
    
    /**
     * Creates a resource bundle based on an existing {@code Properties} object.
     */
    public DynamicResourceBundle(Properties source) {
        this.bundle = source;
    }
    
    /**
     * Creates a resource bundle by reading in a properties file.
     * @throws IOException if an I/O error occurs while reading.
     */
    public DynamicResourceBundle(Reader reader) throws IOException {
        this(LoadUtils.loadProperties(reader));
    }
    
    /**
     * Creates a resource bundle by reading a properties file from the specified
     * input stream. The stream is closed afterwards.
     * @throws IOException if an I/O error occurs while reading.
     */
    public DynamicResourceBundle(InputStream stream, Charset charset) throws IOException {
        this(LoadUtils.loadProperties(stream, charset));
    }

    /**
     * Creates a resource bundle from text that has been loaded externally.
     */
    public DynamicResourceBundle(String contents) {
        this.bundle = new Properties();

        for (String line : LINE_SPLITTER.split(contents)) {
            int index = line.indexOf('=');
            if (index == -1) {
                index = line.indexOf(':');
            }

            if (index != -1) {
                bundle.setProperty(line.substring(0, index).trim(), line.substring(index + 1).trim());
            }
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        String text = bundle.getProperty(key);
        if (text == null && parent == null) {
            text = key;
        }
        
        return text;
    }
    
    /**
     * Returns the message with the specified key, formatted with parameters. This
     * method is an extended version of {@link #getString(String)} that formats 
     * the string with a number of parameters.
     * <p>
     * Parameters are assumed to be using the {@link java.text.MessageFormat}
     * notation of {0}, {1}, etc. Also, for backward compatibility, an alternative
     * notation is also supported where @ is the only parameter. 
     * @throws MissingResourceException when no message for the key can be found.
     */
    public String getString(String key, Object... params) {
        String message = getString(key);
        
        if (params.length == 0) {
            return message;
        }
        
        // MessageFormat uses a single quote (') for syntax and requires two quotes ('')
        // if you want to have a quote in your text. People tend to forget this, so make
        // an attempt to auto-correct.
        if (message.contains("'") && !message.contains("''")) {
            message = message.replaceAll("'", "''");
        }
        
        return MessageFormat.format(message, params);
    }

    @Override
    public Set<String> keySet() {
        return handleKeySet();
    }

    @Override
    protected Set<String> handleKeySet() {
        return getAll().keySet();
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(keySet());
    }
    
    /**
     * Returns all key/value pairs in this resource bundle as a map. The map is
     * a copy, changes to it will not affect the original.
     */
    public Map<String, String> getAll() {
        Map<String, String> copy = new HashMap<>();
        for (Map.Entry<Object,Object> entry : bundle.entrySet()) {
            copy.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return copy;
    }
    
    @Override
    public void setParent(ResourceBundle parent) {
        super.setParent(parent);
    }
    
    public ResourceBundle getParent() {
        return parent;
    }

    private static Properties toProperties(ResourceBundle bundle) {
        Properties properties = new Properties();
        for (String key : Collections.list(bundle.getKeys())) {
            properties.setProperty(key, bundle.getString(key));
        }
        return properties;
    }
}
