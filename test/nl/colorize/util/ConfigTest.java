//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Splitter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {

    @Test
    void defaultTypeMappers() {
        Config config = Config.raw();

        assertEquals("test", config.get("test", String.class));
        assertEquals(123, config.get("123", int.class));
        assertEquals(123, config.get("123", Integer.class));
        assertEquals(123.4f, config.get("123.4", float.class));
        assertTrue(config.get("true", boolean.class));
        assertEquals(DateParser.parse("20130501"), config.get("2013-05-01", Date.class));
        assertEquals(new File("/tmp"), config.get("/tmp", File.class));
        assertEquals(Version.parse("1.2"), config.get("v1.2", Version.class));
    }

    @Test
    void customTypeMapper() {
        Config config = Config.from(key -> key);
        config.registerTypeMapper(DateRange.class, value -> {
            List<String> parts = Splitter.on(" - ").trimResults().splitToList(value);
            return new DateRange(DateParser.parse(parts.get(0)), DateParser.parse(parts.get(1)));
        });

        assertEquals(new DateRange(DateParser.parse("2013-05-01"), DateParser.parse("2013-06-01")),
            config.get("20130501 - 20130601", DateRange.class));
    }

    @Test
    void forwardExceptionForInvalidProperty() {
        Config config = Config.from(key -> key);

        assertThrows(NumberFormatException.class, () -> config.get("test", int.class));
    }

    @Test
    void returnEmptyOptionalForEmptyButThrowForFailure() {
        Config config = Config.from(key -> key);

        assertTrue(config.parse("123", int.class).isPresent());
        assertFalse(config.parse("", int.class).isPresent());
        assertThrows(NumberFormatException.class, () -> config.parse("test", int.class));
    }

    @Test
    void withDefaultValue() {
        Config config = Config.from(key -> key);

        assertEquals(123, config.get("123", int.class, 999));
        assertEquals(999, config.get("test", int.class, 999));
    }

    @Test
    void fromProperties() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");
        properties.setProperty("b", "wrong");

        Config config = Config.from(properties);

        assertEquals(2, config.get("a", int.class, -1));
        assertEquals(-1, config.get("b", int.class, -1));
        assertEquals(-1, config.get("c", int.class, -1));
    }

    @Test
    void shortHandForCommonDataTypes() {
        Properties properties = new Properties();
        properties.setProperty("a", "2");
        properties.setProperty("b", "3.4");
        properties.setProperty("c", "true");
        properties.setProperty("d", "1234");
        properties.setProperty("e", "5.6");

        Config config = Config.from(properties);

        assertEquals(2, config.getInt("a", -1));
        assertEquals(3.4f, config.getDouble("b", -1f), 0.001f);
        assertTrue(config.getBool("c", false));
        assertEquals(1234L, config.getLong("d", -1L));
        assertEquals(5.6, config.getDouble("e", -1f), 0.001);
        assertEquals("5.6", config.getString("e", ""));
    }

    @Test
    void convertFilePaths() {
        Config config = Config.from(key -> key);
        File file = config.get("/tmp/a.txt", File.class);
        Path path = config.get("/tmp/a.txt", Path.class);

        assertEquals("/tmp/a.txt", file.getAbsolutePath());
        assertEquals("/tmp/a.txt", path.toString());
        assertEquals("/tmp/a.txt", path.toFile().getAbsolutePath());
        assertEquals(file, path.toFile());
    }

    @Test
    void convertDateAndTime() {
        Config config = Config.from(key -> key);
        LocalDate date = config.get("2018-03-18", LocalDate.class);
        LocalDateTime datetime = config.get("2018-03-18 15:30", LocalDateTime.class);

        assertEquals(LocalDate.of(2018, 3, 18), date);
        assertEquals(LocalDateTime.of(2018, 3, 18, 15, 30), datetime);
    }

    @Test
    void fromMap() {
        Map<String, String> properties = Map.of("a", "b", "c", "2");
        Config config = Config.from(properties);

        assertEquals("b", config.getString("a", "?"));
        assertEquals(2, config.getInt("c", -1));
        assertEquals(-1, config.getInt("d", -1));
    }

    @Test
    void fromCSV() {
        CSVRecord record = CSVFormat.SEMICOLON.parseCSV("name;age\njohn;38").getFirst();
        Config config = record.toConfig();

        assertEquals("john", config.getString("name", ""));
        assertEquals(38, config.getInt("age", -1));
    }

    @Test
    void defaultDeserializerForEnum() {
        Config config = Config.from(key -> key);

        assertEquals(RUNTIME, config.get("RUNTIME", RetentionPolicy.class));
        assertEquals(RUNTIME, config.get("runtime", RetentionPolicy.class));
        assertEquals(SOURCE, config.get("invalid", RetentionPolicy.class, SOURCE));
    }

    @Test
    void customDeserializerForEnum() {
        Config config = Config.from(key -> key);
        config.registerTypeMapper(RetentionPolicy.class, RetentionPolicy::valueOf);

        assertEquals(RUNTIME, config.get("RUNTIME", RetentionPolicy.class));
        assertEquals(SOURCE, config.get("invalid", RetentionPolicy.class, SOURCE));
    }

    @Test
    void throwIfRequiredMissing() {
        Config config = Config.from(_ -> null);

        assertThrows(NoSuchElementException.class, () -> config.getString("a"));
        assertThrows(NoSuchElementException.class, () -> config.getInt("a"));
        assertThrows(NoSuchElementException.class, () -> config.getLong("a"));
        assertThrows(NoSuchElementException.class, () -> config.getDouble("a"));
        assertThrows(NoSuchElementException.class, () -> config.getBool("a"));
    }

    @Test
    void emptyConfig() {
        Config config = Config.empty();

        assertEquals("2", config.getString("a", "2"));
        assertThrows(NoSuchElementException.class, () -> config.getString("a"));
    }

    @Test
    void cannotReplaceDefaultTypeMapper() {
        Config config = Config.raw();

        assertThrows(IllegalStateException.class,
            () -> config.registerTypeMapper(String.class, _ -> null));
    }
}
