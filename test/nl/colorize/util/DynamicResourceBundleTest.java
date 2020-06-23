//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Charsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicResourceBundleTest {
    
    private DynamicResourceBundle bundle;
    private DynamicResourceBundle bundleNL;
    
    @BeforeEach
    public void before() {
        bundle = new DynamicResourceBundle(LoadUtils.toProperties(
            "key.a", "value",
            "key.b", "this is {0} parameter",
            "key.c", "these are {0} parameters{1}",
            "key.d", "this is {0}'s MessageFormat",
            "key.e", "привет{0}"));
        
        bundleNL = new DynamicResourceBundle(LoadUtils.toProperties(
            "key.a", "waarde",
            "key.b", "dit is {0} parameter"));
        bundleNL.setParent(bundle);
    }

    @Test
    public void testGetString() {
        assertEquals("value", bundle.getString("key.a"));
        assertEquals("this is 1 parameter", bundle.getString("key.b", "1"));
        assertEquals("these are 2 parameters!", bundle.getString("key.c", "2", "!"));
    }
    
    @Test
    public void testUTF8() {
        assertEquals("this is John's MessageFormat", bundle.getString("key.d", "John"));
        assertEquals("привет!", bundle.getString("key.e", "!"));
    }
    
    @Test
    public void testQuotes() {
        Properties properties = new Properties();
        properties.setProperty("a", "single 'quotes' {0}");
        properties.setProperty("b", "double ''quotes''");
        properties.setProperty("c", "quotes at the end'");
        
        DynamicResourceBundle quoteBundle = new DynamicResourceBundle(properties);
        assertEquals("single 'quotes' {0}", quoteBundle.getString("a"));
        assertEquals("single 'quotes' {0}", quoteBundle.getString("a", new Object[0]));
        assertEquals("single 'quotes' !", quoteBundle.getString("a", "!"));
        assertEquals("double 'quotes'", quoteBundle.getString("b", 2));
        assertEquals("quotes at the end'", quoteBundle.getString("c"));
    }
    
    @Test
    public void testGetAll() {
        assertEquals(5, bundle.keySet().size());
        assertEquals(5, bundle.getAll().keySet().size());
        assertEquals("value", bundle.getString("key.a"));
    }
    
    @Test
    public void testFallback() {
        assertEquals(bundle, bundleNL.getParent());
        assertEquals("waarde", bundleNL.getString("key.a"));
        assertEquals("these are {0} parameters{1}", bundleNL.getString("key.c"));
    }
    
    @Test
    public void testFromExistingBundle() throws IOException {
        String data = "a=test\nb=2\nc=Something else \\\nwith newline";
        byte[] bytes = data.getBytes(Charsets.UTF_8.displayName());
        ResourceBundle source = new PropertyResourceBundle(new ByteArrayInputStream(bytes));
        DynamicResourceBundle clone = new DynamicResourceBundle(source);
        List<String> sourceKeys = Collections.list(source.getKeys());
        List<String> cloneKeys = Collections.list(clone.getKeys());
        assertEquals(sourceKeys, cloneKeys);
    }
    
    @Test
    public void testGetKeys() {
        List<String> expected = Arrays.asList("key.a", "key.b", "key.c", "key.d", "key.e");
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            assertTrue(expected.contains(key));
        }
    }

    @Test
    public void testLookup() throws Exception {
        File tempDir = LoadUtils.createTempDir();
        LoadUtils.createTempFile(tempDir, "test.properties", "a=Hello world", Charsets.UTF_8);
        LoadUtils.createTempFile(tempDir, "test_nl.properties", "a=Hallo wereld", Charsets.UTF_8);
        ResourceFile dirRF = new ResourceFile(tempDir);
        
        DynamicResourceBundle en = new DynamicResourceBundle("test", dirRF, new Locale("en"), Charsets.UTF_8);
        assertEquals("Hello world", en.getString("a"));
        
        DynamicResourceBundle nl = new DynamicResourceBundle("test", dirRF, new Locale("nl"), Charsets.UTF_8);
        assertEquals("Hallo wereld", nl.getString("a"));
        
        DynamicResourceBundle de = new DynamicResourceBundle("test", dirRF, new Locale("de"), Charsets.UTF_8);
        assertEquals("Hello world", de.getString("a"));
    }
    
    @Test
    public void testExceptionIfLookupFails() throws Exception {
        ResourceFile tempDir = new ResourceFile(LoadUtils.createTempDir());

        assertThrows(MissingResourceException.class, () -> {
            new DynamicResourceBundle("NonExisting", tempDir, Locale.getDefault(), Charsets.UTF_8);
        });
    }
    
    @Test
    public void testCreateFromStream() throws Exception {
        ByteArrayInputStream stream = new ByteArrayInputStream("a=Fred\nb=Piet".getBytes("UTF-8"));
        DynamicResourceBundle fromStream = new DynamicResourceBundle(stream, Charsets.UTF_8);
        assertEquals("Fred", fromStream.getString("a"));
        assertEquals("Piet", fromStream.getString("b"));
        
        stream = new ByteArrayInputStream("a=Fred\nb=Piet".getBytes("UTF-8"));
        DynamicResourceBundle fromReader = new DynamicResourceBundle(new InputStreamReader(stream, "UTF-8"));
        assertEquals("Fred", fromReader.getString("a"));
        assertEquals("Piet", fromReader.getString("b"));
    }

    @Test
    public void testLoadFromText() {
        String text = "a=this is a test\nb = this is also a test";

        DynamicResourceBundle bundle = new DynamicResourceBundle(text);

        assertEquals("this is a test", bundle.getString("a"));
        assertEquals("this is also a test", bundle.getString("b"));
    }
}
