//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

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

    /**
     * Creates a {@link TranslationBundle} based on the specified default
     * translation. Additional translations can be added afterward.
     * <p>
     * In most cases, translation data will be stored in {@code .properties}
     * files. Use the {@link #from(Properties)} or {@link #from(ResourceFile)}
     * factory methods to create a {@link TranslationBundle} instance from the
     * contents of such a file.
     */
    private TranslationBundle(Map<String, String> defaultTranslation,
                              Map<Locale, TranslationBundle> translations) {
        this.defaultTranslation = Map.copyOf(defaultTranslation);
        this.translations = Map.copyOf(translations);
    }

    /**
     * Returns a new {@link TranslationBundle} that adds the specified
     * translation. This {@link TranslationBundle}'s default translation
     * will act as a fallback for any keys that are not included in the
     * translation.
     *
     * @throws IllegalArgumentException if this {@link TranslationBundle}
     *         already includes a translation for the same locale.
     */
    public TranslationBundle withTranslation(Locale locale, TranslationBundle translation) {
        Preconditions.checkArgument(!translations.containsKey(locale),
            "Translation for locale already exists: " + locale);

        Map<Locale, TranslationBundle> newTranslations = new HashMap<>();
        newTranslations.putAll(translations);
        newTranslations.put(locale, translation);

        return new TranslationBundle(defaultTranslation, newTranslations);
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

    /**
     * Returns all translation keys that exist for the translation for the
     * specified locale, using the default translation as a fallback where
     * necessary. The keys will be returned in random order.
     */
    public Set<String> getKeys(Locale locale) {
        Set<String> keys = new HashSet<>();
        keys.addAll(defaultTranslation.keySet());
        if (translations.containsKey(locale)) {
            keys.addAll(translations.get(locale).defaultTranslation.keySet());
        }
        return keys;
    }

    /**
     * Returns all translation keys that exist for the default translation.
     * The keys will be returned in random order.
     */
    public Set<String> getKeys() {
        return defaultTranslation.keySet();
    }

    /**
     * Returns a {@link TranslationBundle} that will use the specified
     * {@link Properties} as its default translation. Additional translations
     * can be added afterward.
     */
    public static TranslationBundle from(Properties defaultTranslation) {
        return new TranslationBundle(PropertyUtils.toMap(defaultTranslation),
            Collections.emptyMap());
    }

    /**
     * Loads the specified {@code .properties} file, then uses the resulting
     * properties as the default translation for a {@link TranslationBundle}.
     * Additional translations can be added afterward.
     * <p>
     * {@link PropertyUtils#loadProperties(ResourceFile)} is used to load the
     * file. Refer to the documentation of that method for more information on
     * supported character encodings for different platforms.
     */
    public static TranslationBundle from(ResourceFile propertiesFile) {
        return from(PropertyUtils.loadProperties(propertiesFile));
    }

    /**
     * Returns a {@link TranslationBundle} that loads the <em>contents</em> of
     * a {@code .properties} file, then uses the resulting properties as the
     * default translation. Additional translations can be added afterward.
     * <p>
     * {@link PropertyUtils#loadProperties(String)} is used to load the file.
     * Refer to the documentation of that method for more information on
     * supported character encodings for different platforms.
     */
    public static TranslationBundle fromPropertiesFile(String propertiesFileContents) {
        return from(PropertyUtils.loadProperties(propertiesFileContents));
    }

    /**
     * Returns a {@link TranslationBundle} that will use the key/value
     * properties in the specified map as its default translation. Additional
     * translations can be added afterward.
     */
    public static TranslationBundle fromMap(Map<String, String> defaultTranslation) {
        return new TranslationBundle(defaultTranslation, Collections.emptyMap());
    }
}
