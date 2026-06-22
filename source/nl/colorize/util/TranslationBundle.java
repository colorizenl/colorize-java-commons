//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Translation mechanism for applications that is more flexible than the
 * standard {@link ResourceBundle} implementation(s).
 * <p>
 * A {@link TranslationBundle} instance contains the translation data for
 * one or multiple locales. Translations can be defined programmatically, or
 * loaded from a {@code .properties} file. Translations are not limited to
 * static text, they may contain {@link MessageFormat} notation in order to
 * support parameterized messages. Once a {@link TranslationBundle} instance
 * has been constructed, {@link #select(Locale)} can be used to obtain an
 * instance for the requested locale. If no matching translation is available,
 * the translation for the default locale will be used instead.
 * <p>
 * Instances of this class are immutable, and therefore thread-safe. The same
 * instance can safely be accessed from multiple threads.
 */
public final class TranslationBundle extends ResourceBundle {

    private List<Translation> availableTranslations;
    private Translation defaultTranslation;
    private Translation selectedTranslation;

    /**
     * Predefined constant for the Dutch language locale, similar to other
     * constants like {@link Locale#US}.
     */
    //TODO This cannot use Locale.of because it's not yet supported by TeaVM.
    @SuppressWarnings("deprecation")
    public static final Locale NL = new Locale("nl");

    /**
     * Creates a new {@link TranslationBundle} that contains translations for
     * the specified locale(s). The first translation in the list is considered
     * to be the default locale.
     * <p>
     * This is an internal constructor, applications should use one of the
     * static factory methods.
     *
     * @throws IllegalArgumentException if the list of translations is empty.
     * @throws IllegalStateException if the selected translation is missing
     *         from the available translations.
     */
    private TranslationBundle(List<Translation> translations, Translation selected) {
        Preconditions.checkArgument(!translations.isEmpty(), "No translations provided");
        Preconditions.checkState(translations.contains(selected), "Unknown selected translation");

        this.availableTranslations = List.copyOf(translations);
        this.defaultTranslation = translations.getFirst();
        this.selectedTranslation = selected;
    }

    /**
     * Returns a new {@link TranslationBundle} instance that adds the
     * specified translation to the list of available translations. The
     * translation data is loaded from a {@code .properties} file.
     * <p>
     * Adding a translation will <em>not</em> automatically select that
     * translation, this needs to be done using explicitly by calling
     * {@link #select(Locale)}.
     *
     * @throws IllegalArgumentException if this {@link TranslationBundle}
     *         already contains a translation for the same locale.
     */
    public TranslationBundle withTranslation(Locale locale, Properties contents) {
        Preconditions.checkArgument(!hasTranslation(locale),
            "TranslationBundle already contains a translation for locale: " + locale);

        List<Translation> combinedTranslations = new ArrayList<>();
        combinedTranslations.addAll(availableTranslations);
        combinedTranslations.add(new Translation(locale, contents));

        return new TranslationBundle(combinedTranslations, selectedTranslation);
    }

    /**
     * Returns a new {@link TranslationBundle} instance that adds the
     * specified translation to the list of available translations. The
     * translation data is loaded from a {@code .properties} file.
     * <p>
     * Adding a translation will <em>not</em> automatically select that
     * translation, this needs to be done using explicitly by calling
     * {@link #select(Locale)}.
     *
     * @throws IllegalArgumentException if this {@link TranslationBundle}
     *         already contains a translation for the same locale.
     */
    public TranslationBundle withTranslation(Locale locale, ResourceFile propertiesFile) {
        Properties contents = PropertyUtils.loadProperties(propertiesFile);
        return withTranslation(locale, contents);
    }

    private boolean hasTranslation(Locale locale) {
        return availableTranslations.stream()
            .anyMatch(translation -> translation.locale().equals(locale));
    }

    /**
     * Returns a new {@link TranslationBundle} instance that is based on the
     * available translations in this instance, selecting the locale that is
     * considered the closest match to the requested locale. If no suitable
     * translation is available, the default locale will be used.
     */
    public TranslationBundle select(Locale requested) {
        Translation matchingTranslation = matchCountry(requested)
            .or(() -> matchLanguage(requested))
            .orElse(defaultTranslation);

        return new TranslationBundle(availableTranslations, matchingTranslation);
    }

    private Optional<Translation> matchCountry(Locale requested) {
        return availableTranslations.stream()
            .filter(translation -> translation.locale != null)
            .filter(translation -> translation.locale.getLanguage().equals(requested.getLanguage()))
            .filter(translation -> translation.locale.getCountry().equals(requested.getCountry()))
            .findFirst();
    }

    private Optional<Translation> matchLanguage(Locale requested) {
        return availableTranslations.stream()
            .filter(translation -> translation.locale != null)
            .filter(translation -> translation.locale.getLanguage().equals(requested.getLanguage()))
            .findFirst();
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
        String text = selectedTranslation.contents().getOrDefault(key, key);

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
    protected Object handleGetObject(String key) {
        return selectedTranslation.contents().getOrDefault(key, key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(selectedTranslation.contents().keySet());
    }

    @Override
    public Set<String> keySet() {
        // This overrides the parent class implementation because
        // this method would otherwise not be available in TeaVM.
        return selectedTranslation.contents().keySet();
    }

    /**
     * Creates a new {@link TranslationBundle} based with the specified
     * default translation and default locale.
     */
    public static TranslationBundle from(Locale locale, Properties contents) {
        if (locale == null) {
            locale = Locale.US;
        }

        Translation defaultTranslation = new Translation(locale, contents);
        return new TranslationBundle(List.of(defaultTranslation), defaultTranslation);
    }

    /**
     * Creates a new {@link TranslationBundle} based with the specified
     * default translation and with {@link Locale#US} as the default locale.
     */
    public static TranslationBundle from(Properties contents) {
        return from(Locale.US, contents);
    }

    /**
     * Creates a new {@link TranslationBundle} based with the specified
     * default translation and default locale. The translation data is loaded
     * from a {@code .properties} file.
     */
    public static TranslationBundle from(Locale locale, ResourceFile propertiesFile) {
        Properties contents = PropertyUtils.loadProperties(propertiesFile);
        return from(locale, contents);
    }

    /**
     * Creates a new {@link TranslationBundle} based with the specified
     * default translation and with {@link Locale#US} as the default locale.
     * The translation data is loaded from a {@code .properties} file.
     */
    public static TranslationBundle from(ResourceFile propertiesFile) {
        return from(Locale.US, propertiesFile);
    }

    /**
     * Represents one of the translations in this {@link TranslationBundle},
     * which connects a resource bundle to a locale.
     */
    private record Translation(Locale locale, Map<String, String> contents) {

        public Translation(Locale locale, Properties contents) {
            this(locale, Map.copyOf(PropertyUtils.toMap(contents)));
        }
    }
}
