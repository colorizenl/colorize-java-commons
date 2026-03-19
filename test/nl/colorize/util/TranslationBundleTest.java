//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationBundleTest {

    private static final Locale NL = Locale.of("nl");
    private static final Locale IT = Locale.of("it");

    @Test
    void formatPlaceholders() {
        Map<String, String> text = Map.of(
            "key.a", "value",
            "key.b", "this is {0} parameter",
            "key.c", "this is {0}'s MessageFormat"
        );

        TranslationBundle bundle = TranslationBundle.from(PropertyUtils.toProperties(text));

        assertEquals("value", bundle.getText("key.a"));
        assertEquals("this is 1 parameter", bundle.getText("key.b", "1"));
        assertEquals("this is 2's MessageFormat", bundle.getText("key.c", "2"));
        assertEquals("key.d", bundle.getText("key.d"));
    }

    @Test
    void fromUTF8() {
        Map<String, String> text = Map.of("key.e", "привет{0}");
        TranslationBundle bundle = TranslationBundle.from(PropertyUtils.toProperties(text));

        assertEquals("привет!", bundle.getText("key.e", "!"));
    }

    @Test
    void getTranslation() {
        Map<String, String> textEN = Map.of(
            "key.a", "value",
            "key.b", "this is {0} parameter",
            "key.c", "this is not translated"
        );

        Map<String, String> textNL = Map.of(
            "key.a", "waarde",
            "key.b", "dit is {0} parameter"
        );

        TranslationBundle bundle = TranslationBundle.from(PropertyUtils.toProperties(textEN))
            .link(TranslationBundle.from(NL, PropertyUtils.toProperties(textNL)));

        assertEquals("waarde", bundle.select(NL).getText("key.a"));
        assertEquals("dit is 1 parameter", bundle.select(NL).getText("key.b", "1"));
        assertEquals("key.d", bundle.select(NL).getText("key.d"));
    }

    @Test
    void combineKeys() {
        Map<String, String> textEN = Map.of(
            "key.a", "value",
            "key.b", "this is {0} parameter"
        );

        Map<String, String> textNL = Map.of(
            "key.a", "waarde",
            "key.b", "dit is {0} parameter",
            "key.c", "dit is iets anders"
        );

        TranslationBundle bundle = TranslationBundle.from(PropertyUtils.toProperties(textEN))
            .link(TranslationBundle.from(NL, PropertyUtils.toProperties(textNL)));

        assertEquals(Set.of("key.a", "key.b", "key.c"), bundle.select(NL).keySet());
        assertEquals(Set.of("key.a", "key.b"), bundle.keySet());
        assertEquals(Set.of("key.a", "key.b"), bundle.select(IT).keySet());
    }

    @Test
    void loadFromPropertiesFile() throws IOException {
        File tempFile = FileUtils.createTempFile("a=test\nb=multi \\\nline\nc=other", UTF_8);
        Properties properties = PropertyUtils.loadProperties(tempFile, UTF_8);
        TranslationBundle bundle = TranslationBundle.from(properties);

        assertEquals("test", bundle.getText("a"));
        assertEquals("multi line", bundle.getText("b"));
        assertEquals("other", bundle.getText("c"));
    }

    @Test
    void loadFromText() {
        Properties properties = PropertyUtils.loadProperties(new StringReader(
            "a=this is a test\nb = this is also a test"));
        TranslationBundle bundle = TranslationBundle.from(properties);

        assertEquals("this is a test", bundle.getText("a"));
        assertEquals("this is also a test", bundle.getText("b"));
    }

    @Test
    void formatEntryWithLineBreak() {
        Map<String, String> text = Map.of("key.a", "first\nsecond {0}\nthird");
        TranslationBundle bundle = TranslationBundle.from(PropertyUtils.toProperties(text));

        assertEquals("first\nsecond 2\nthird", bundle.getText("key.a", "2"));
    }

    @Test
    void preferCountryMatch() {
        Properties us = PropertyUtils.loadProperties("a=speedster");
        Properties uk = PropertyUtils.loadProperties("a=pace merchant");
        Properties nl = PropertyUtils.loadProperties("a=snelheidsduivel");

        TranslationBundle bundle = TranslationBundle.from(Locale.US, us)
            .link(TranslationBundle.from(Locale.UK, uk))
            .link(TranslationBundle.from(NL, nl));

        assertEquals("speedster", bundle.select(Locale.US).getText("a"));
        assertEquals("pace merchant", bundle.select(Locale.UK).getText("a"));
        assertEquals("speedster", bundle.select(Locale.ENGLISH).getText("a"));
        assertEquals("snelheidsduivel", bundle.select(NL).getText("a"));
    }

    @Test
    void returnKeyIfNoTranslationIsAvailable() {
        TranslationBundle bundle = TranslationBundle.from(new Properties());

        assertEquals("test", bundle.handleGetObject("test"));
        assertEquals("test", bundle.getObject("test"));
        assertEquals("test", bundle.getString("test"));
    }
}
