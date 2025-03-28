//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
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

    @Test
    void formatPlaceholders() {
        Map<String, String> text = Map.of(
            "key.a", "value",
            "key.b", "this is {0} parameter",
            "key.c", "this is {0}'s MessageFormat"
        );

        TranslationBundle bundle = TranslationBundle.fromMap(text);

        assertEquals("value", bundle.getText("key.a"));
        assertEquals("this is 1 parameter", bundle.getText("key.b", "1"));
        assertEquals("this is 2's MessageFormat", bundle.getText("key.c", "2"));
        assertEquals("key.d", bundle.getText("key.d"));
    }

    @Test
    void fromUTF8() {
        Map<String, String> text = Map.of("key.e", "привет{0}");
        TranslationBundle bundle = TranslationBundle.fromMap(text);

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

        TranslationBundle bundle = TranslationBundle.fromMap(textEN);
        bundle = bundle.withTranslation(new Locale("nl"), TranslationBundle.fromMap(textNL));

        assertEquals("waarde", bundle.getText(new Locale("nl"), "key.a"));
        assertEquals("dit is 1 parameter", bundle.getText(new Locale("nl"), "key.b", "1"));
        assertEquals("this is not translated", bundle.getText(new Locale("nl"), "key.c"));
        assertEquals("key.d", bundle.getText(new Locale("nl"), "key.d"));
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

        TranslationBundle bundle = TranslationBundle.fromMap(textEN);
        bundle = bundle.withTranslation(new Locale("nl"), TranslationBundle.fromMap(textNL));

        assertEquals(Set.of("key.a", "key.b", "key.c"), bundle.getKeys(new Locale("nl")));
        assertEquals(Set.of("key.a", "key.b"), bundle.getKeys());
        assertEquals(Set.of("key.a", "key.b"), bundle.getKeys(new Locale("it")));
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
        TranslationBundle bundle = TranslationBundle.fromMap(text);

        assertEquals("first\nsecond 2\nthird", bundle.getText("key.a", "2"));
    }
}
