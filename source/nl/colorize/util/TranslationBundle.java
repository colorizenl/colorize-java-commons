//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Used to access text with different translations for different locale. Each
 * text string is identified by a key rather than the actual text. The bundle
 * will then return the matching text for the requested locale, with a fallback
 * to the bundle's default translation if no specific translation is available
 * for the requested locale. The text can contain placeholders that follow the
 * {@link MessageFormat} notation.
 * <p>
 * The purpose of this class is similar to {@link java.util.ResourceBundle}.
 * Older versions of this library used to contain a direct ResourceBundle
 * subclass, but ResourceBundle is very old and contains a lot of obscure
 * functionality. This class provides a similar API to ResourceBundle, and
 * supports loading text from {@code properties} files. However, it is more
 * flexible and allows to load text from various other sources. It also allows
 * custom file names and locations, instead of the predefined naming convention
 * required by {@link java.util.ResourceBundle}.
 */
public final class TranslationBundle {

    private Map<String, String> defaultTranslation;
    private Map<Locale, TranslationBundle> translations;

    public TranslationBundle(Map<String, String> defaultTranslation) {
        this.defaultTranslation = ImmutableMap.copyOf(defaultTranslation);
        this.translations = new HashMap<>();
    }

    public void addTranslation(Locale locale, TranslationBundle translation) {
        translations.put(locale, translation);
    }

    /**
     * Returns the text string with the specified key, for the requested locale.
     * If no suitable translation exists, the default translation is used as a
     * fallback. If the default translation is also not available, the key is
     * returned as a last resort.
     */
    public String getText(Locale locale, String key, Object... params) {
        String text = null;

        if (locale != null && translations.containsKey(locale)) {
            text = translations.get(locale).defaultTranslation.get(key);
        }

        if (text == null) {
            text = defaultTranslation.getOrDefault(key, key);
        }

        if (params.length == 0) {
            return text;
        }

        // MessageFormat uses a single quote (') for syntax and requires two
        // quotes ('') if you want to have a quote in your text. People tend
        // to forget this, so make an attempt to auto-correct.
        if (text.contains("'") && !text.contains("''")) {
            text = text.replaceAll("'", "''");
        }

        return MessageFormat.format(text, params);
    }

    /**
     * Returns the text string with the specified key for the default translation.
     * If no translation is also available, the key is returned as a fallback.
     */
    public String getText(String key, Object... params) {
        return getText(null, key, params);
    }

    /**
     * Provided for API compatibility with {@code ResourceBundle} and the
     * {@code DynamicResourceBundle} subclass that used to be included in
     * previous versions of this library. This method will redirect to
     * {@link #getText(Locale, String, Object...)} and will use the default
     * translation.
     */
    public String getString(String key, Object... params) {
        return getText(null, key, params);
    }

    public Set<String> getKeys(Locale locale) {
        HashSet<String> keys = new HashSet<>();
        keys.addAll(defaultTranslation.keySet());
        if (translations.containsKey(locale)) {
            keys.addAll(translations.get(locale).defaultTranslation.keySet());
        }
        return keys;
    }

    public Set<String> getKeys() {
        return ImmutableSet.copyOf(defaultTranslation.keySet());
    }

    public static TranslationBundle fromProperties(Properties defaultTranslation) {
        Map<String, String> converted = new HashMap<>();
        for (Object keyObject : defaultTranslation.keySet()) {
            String key = (String) keyObject;
            converted.put(key, defaultTranslation.getProperty(key));
        }

        return new TranslationBundle(converted);
    }

    public static TranslationBundle fromFile(ResourceFile file) {
        return fromProperties(LoadUtils.loadProperties(file, Charsets.UTF_8));
    }

    /**
     * Loads the translation bundle from the contents of a {@code .properties}
     * file.
     *
     * @deprecated Prefer {@link #fromProperties(Properties)}, this method is
     *             only provided for platforms where loading {@link Properties}
     *             is not available, so only a limited subset of the
     *             {@code .properties} file format is supported.
     */
    @Deprecated
    public static TranslationBundle fromProperties(String properties) {
        Map<String, String> text = new HashMap<>();

        for (String line : Splitter.on("\n").trimResults().split(properties)) {
            int index = line.indexOf('=');
            if (index != -1) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim().replace("\\n", "\n");
                text.put(key, value);
            }
        }

        return new TranslationBundle(text);
    }

    /**
     * Returns a bundle that, unless additional translations for specific locales
     * are added, will not actually translate anything and just use the keys.
     */
    public static TranslationBundle empty() {
        return new TranslationBundle(Collections.emptyMap());
    }
}
