//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Splitter;
import nl.colorize.util.stats.CSVFormat;
import nl.colorize.util.stats.CSVRecord;
import nl.colorize.util.stats.DateRange;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.annotation.RetentionPolicy;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;
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
        Properties properties = PropertyUtils.loadProperties("a=2");

        PropertyDeserializer withoutPreprocessor = new PropertyDeserializer();
        PropertyDeserializer withPreprocessor = new PropertyDeserializer();
        withPreprocessor.registerPreprocessor(properties::getProperty);

        assertEquals(-1, withoutPreprocessor.parse("a", int.class, -1));
        assertEquals(2, withPreprocessor.parse("a", int.class, -1));
    }

    @Test
    void fromProperties() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");
        properties.setProperty("b", "wrong");

        PropertyDeserializer propertyDeserializer = PropertyDeserializer.fromProperties(properties);

        assertEquals(2, propertyDeserializer.parse("a", int.class, -1));
        assertEquals(-1, propertyDeserializer.parse("b", int.class, -1));
        assertEquals(-1, propertyDeserializer.parse("c", int.class, -1));
    }

    @Test
    void shortHandForCommonDataTypes() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");
        properties.setProperty("b", "3.4");
        properties.setProperty("c", "true");
        properties.setProperty("d", "1234");
        properties.setProperty("e", "5.6");

        PropertyDeserializer propertyDeserializer = PropertyDeserializer.fromProperties(properties);

        assertEquals(2, propertyDeserializer.parseInt("a", -1));
        assertEquals(3.4f, propertyDeserializer.parseFloat("b", -1f), 0.001f);
        assertTrue(propertyDeserializer.parseBool("c", false));
        assertEquals(1234L, propertyDeserializer.parseLong("d", -1L));
        assertEquals(5.6, propertyDeserializer.parseDouble("e", -1f), 0.001);
        assertEquals("5.6", propertyDeserializer.parseString("e", ""));
    }

    @Test
    void convertFilePaths() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();
        File file = propertyDeserializer.parse("/tmp/a.txt", File.class);

        assertEquals("/tmp/a.txt", file.getAbsolutePath());
    }

    @Test
    void convertDateAndTime() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();
        LocalDate date = propertyDeserializer.parse("2018-03-18", LocalDate.class);
        LocalDateTime datetime = propertyDeserializer.parse("2018-03-18 15:30", LocalDateTime.class);

        assertEquals(LocalDate.of(2018, 3, 18), date);
        assertEquals(LocalDateTime.of(2018, 3, 18, 15, 30), datetime);
    }

    @Test
    void fromMap() {
        Map<String, Object> properties = Map.of("a", "b", "c", 2);
        PropertyDeserializer propertyDeserializer = PropertyDeserializer.fromMap(properties);

        assertEquals("b", propertyDeserializer.parseString("a", "?"));
        assertEquals(2, propertyDeserializer.parseInt("c", -1));
        assertEquals(-1, propertyDeserializer.parseInt("d", -1));
    }

    @Test
    void fromCSV() {
        CSVRecord record = CSVFormat.SEMICOLON.parseCSV("name;age\njohn;38").getFirst();
        PropertyDeserializer propertyDeserializer = PropertyDeserializer.fromCSV(record);

        assertEquals("john", propertyDeserializer.parseString("name", ""));
        assertEquals(38, propertyDeserializer.parseInt("age", -1));
    }

    @Test
    void defaultDeserializerForEnum() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();

        assertEquals(RUNTIME, propertyDeserializer.parse("RUNTIME", RetentionPolicy.class));
        assertEquals(RUNTIME, propertyDeserializer.parse("runtime", RetentionPolicy.class));
        assertEquals(SOURCE, propertyDeserializer.parse("invalid", RetentionPolicy.class, SOURCE));
    }

    @Test
    void customDeserializerForEnum() {
        PropertyDeserializer propertyDeserializer = new PropertyDeserializer();
        propertyDeserializer.register(RetentionPolicy.class, RetentionPolicy::valueOf);

        assertEquals(RUNTIME, propertyDeserializer.parse("RUNTIME", RetentionPolicy.class));
        assertEquals(SOURCE, propertyDeserializer.parse("invalid", RetentionPolicy.class, SOURCE));
    }
}
