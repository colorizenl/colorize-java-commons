//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Translation mechanism for applications that is more flexible than the
 * standard {@link ResourceBundle} implementation(s).
 * <p>
 * A {@link TranslationBundle} instance contains the translation data for
 * a specific locale. Translations can be defined programmatically, or
 * loaded from a {@code .properties} file. Translations are not limited to
 * static text, they may contain {@link MessageFormat} notation.
 * <p>
 * Multiple {@link TranslationBundle}s can be linked, to indicate the
 * instances contain translations of the same content but for different
 * locales. Applications can then use {@link #select(Locale)} to retrieve
 * the translation most suitable for the requested locale.
 * <p>
 * Instances of this class are thread-safe and can be accessed from multiple
 * threads.
 */
public final class TranslationBundle extends ResourceBundle {

    private Locale locale;
    private Map<String, String> contents;
    private List<TranslationBundle> translations;

    /**
     * Creates a new {@link TranslationBundle} that contains a translation for
     * the specified locale. This is an internal constructor, applications
     * should use one of the static factory methods.
     */
    private TranslationBundle(Locale locale, Map<String, String> contents) {
        this.locale = locale;
        this.contents = Map.copyOf(contents);

        translations = new CopyOnWriteArrayList<>();
        translations.add(this);
    }

    /**
     * Links two {@link TranslationBundle}s to indicate they represent
     * different translations of the same content. This does not immediately
     * change the behavior of either instance, but it makes them discoverable
     * via {@link #select(Locale)}.
     *
     * @return This instance, for method chaining.
     * @throws IllegalArgumentException if {@code translation} does not
     *         explicitly define a locale.
     */
    public TranslationBundle link(TranslationBundle translation) {
        Preconditions.checkArgument(translation.locale != null, "Missing locale for translation");

        translations.add(translation);
        translation.translations = translations;
        return this;
    }

    /**
     * Returns the {@link TranslationBundle} that is considered the best match
     * for the requested locale. Possible options include the current instance
     * plus all instances linked using {@link #link(TranslationBundle)}.
     */
    public TranslationBundle select(Locale locale) {
        Optional<TranslationBundle> matchCountry = translations.stream()
            .filter(translation -> translation.locale != null)
            .filter(translation -> translation.locale.getLanguage().equals(locale.getLanguage()))
            .filter(translation -> translation.locale.getCountry().equals(locale.getCountry()))
            .findFirst();

        Optional<TranslationBundle> matchLanguage = translations.stream()
            .filter(translation -> translation.locale != null)
            .filter(translation -> translation.locale.getLanguage().equals(locale.getLanguage()))
            .findFirst();

        TranslationBundle defaultTranslation = translations.getFirst();

        return matchCountry.orElse(matchLanguage.orElse(defaultTranslation));
    }

    @Override
    protected Object handleGetObject(String key) {
        return contents.getOrDefault(key, key);
    }

    /**
     * Returns the text string with the specified key. If the text string uses
     * {@link MessageFormat} notation, placeholders are replaced using the
     * values from {@code params}. If no value is available, the key itself is
     * returned instead.
     */
    public String getString(String key, Object... params) {
        return getText(key, params);
    }

    /**
     * Returns the text string with the specified key. If the text string uses
     * {@link MessageFormat} notation, placeholders are replaced using the
     * values from {@code params}. If no value is available, the key itself is
     * returned instead.
     */
    public String getText(String key, Object... params) {
        String text = contents.getOrDefault(key, key);

        if (params.length == 0) {
            return text;
        }

        // MessageFormat uses a single quote (') for syntax and requires two
        // quotes ('') if you want to have a quote in your text. People tend
        // to forget this, so make an attempt to auto-correct.
        if (text.contains("'") && !text.contains("''")) {
            text = text.replace("'", "''");
        }

        return MessageFormat.format(text, params);
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(contents.keySet());
    }

    @Override
    public Set<String> keySet() {
        // This overrides the parent class implementation because
        // this method would otherwise not be available in TeaVM.
        return contents.keySet();
    }

    /**
     * Returns a {@link TranslationBundle} that contains a translation for
     * the specified locale.
     */
    public static TranslationBundle from(Locale locale, Properties contents) {
        return new TranslationBundle(locale, PropertyUtils.toMap(contents));
    }

    /**
     * Returns a {@link TranslationBundle} based on the specified properties.
     * The default locale is assumed to be {@link Locale#US}.
     */
    public static TranslationBundle from(Properties contents) {
        return from(Locale.US, contents);
    }

    /**
     * Returns a {@link TranslationBundle} that loads a {@code .properties}
     * and uses its contents to provide a translation for the specified locale.
     * <p>
     * {@link PropertyUtils#loadProperties(ResourceFile)} is used to load the
     * file. Refer to the documentation of that method for more information on
     * supported character encodings for different platforms.
     */
    public static TranslationBundle from(Locale locale, ResourceFile propertiesFile) {
        Properties properties = PropertyUtils.loadProperties(propertiesFile);
        return new TranslationBundle(locale, PropertyUtils.toMap(properties));
    }

    /**
     * Returns a {@link TranslationBundle} that loads a {@code .properties}
     * and uses its contents to provide a translation. The default locale is
     * assumed to be {@link Locale#US}.
     * <p>
     * {@link PropertyUtils#loadProperties(ResourceFile)} is used to load the
     * file. Refer to the documentation of that method for more information on
     * supported character encodings for different platforms.
     */
    public static TranslationBundle from(ResourceFile propertiesFile) {
        return from(Locale.US, propertiesFile);
    }
}
