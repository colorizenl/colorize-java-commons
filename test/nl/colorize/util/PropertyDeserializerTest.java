//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Splitter;
import nl.colorize.util.stats.DateRange;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyDeserializerTest {

    @Test
    void defaultTypeMappers() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();

        assertEquals("test", propertyDeserializer.parse("test", String.class));
        assertEquals(123, propertyDeserializer.parse("123", int.class));
        assertEquals(123, propertyDeserializer.parse("123", Integer.class));
        assertEquals(123.4f, propertyDeserializer.parse("123.4", float.class));
        assertTrue(propertyDeserializer.parse("true", boolean.class));
        assertEquals(DateParser.parse("20130501"), propertyDeserializer.parse("2013-05-01", Date.class));
        assertEquals(new File("/tmp"), propertyDeserializer.parse("/tmp", File.class));
        assertEquals(Version.parse("1.2"), propertyDeserializer.parse("v1.2", Version.class));
    }

    @Test
    void customTypeMapper() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();
        propertyDeserializer.register(DateRange.class, value -> {
            List<String> parts = Splitter.on(" - ").trimResults().splitToList(value);
            return new DateRange(DateParser.parse(parts.get(0)), DateParser.parse(parts.get(1)));
        });

        assertEquals(new DateRange(DateParser.parse("2013-05-01"), DateParser.parse("2013-06-01")),
            propertyDeserializer.parse("20130501 - 20130601", DateRange.class));
    }

    @Test
    void forwardExceptionForInvalidProperty() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();

        assertThrows(NumberFormatException.class, () -> propertyDeserializer.parse("test", int.class));
    }

    @Test
    void returnEmptyOptionalForFailedAttempt() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();

        assertTrue(propertyDeserializer.attempt("123", int.class).isPresent());
        assertFalse(propertyDeserializer.attempt("test", int.class).isPresent());
    }

    @Test
    void withDefaultValue() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();

        assertEquals(123, propertyDeserializer.parse("123", int.class, 999));
        assertEquals(999, propertyDeserializer.parse("test", int.class, 999));
        assertEquals(999, propertyDeserializer.parse(null, int.class, 999));
    }

    @Test
    void preprocessor() {
        Properties properties = LoadUtils.loadProperties("a=2");

        PropertyDeserializer withoutPreprocessor = new PropertyDeserializer();
        PropertyDeserializer withPreprocessor = new PropertyDeserializer();
        withPreprocessor.registerPreprocessor(properties::getProperty);

        assertEquals(-1, withoutPreprocessor.parse("a", int.class, -1));
        assertEquals(2, withPreprocessor.parse("a", int.class, -1));
    }
}