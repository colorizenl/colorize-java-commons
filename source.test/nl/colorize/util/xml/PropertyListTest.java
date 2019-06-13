//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import nl.colorize.util.FileUtils;
import nl.colorize.util.LoadUtils;
import nl.colorize.util.mock.MockDataHelper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit test for the {@code PropertyList} class.
 */
public class PropertyListTest {

    @Test
    public void testSimpleProperties() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", "1");
        plist.setProperty("b", 2);
        plist.setProperty("c", 1.23f);
        plist.setProperty("d", true);
        assertEquals("1", plist.getProperty("a", null));
        assertEquals(2, (Integer) plist.getProperty("b", null), 0.001f);
        assertEquals(1.23f, plist.getProperty("c", null), 0.001f);
        assertEquals(true, plist.getProperty("d", null));
        assertNull(plist.getProperty("e", null));
    }
    
    @Test
    public void testDictionary() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", "1");
        plist.setProperty("b", 2);
        plist.setProperty("c", ImmutableMap.of("a", "1", "b", 2, "c", true));
        assertEquals("1", plist.getProperty("a", null));
        assertEquals(2, (Integer) plist.getProperty("b", null), 0.001f);
        assertEquals(ImmutableMap.of("a", "1", "b", 2, "c", true), plist.getProperty("c", null));
    }
    
    @Test
    public void testArray() throws Exception {
        PropertyList plist = new PropertyList();
        List<?> array = ImmutableList.of("1", 2, true);
        plist.setProperty("a", array);
        assertEquals(array, plist.getProperty("a", null));
        array = ImmutableList.of("http://www.colorize.nl", "http://www.nu.nl");
        plist.setProperty("b", array);
        assertEquals(ImmutableList.of("http://www.colorize.nl", "http://www.nu.nl"), 
                plist.getProperty("b", null));
    }
    
    @Test
    public void testSave() throws Exception {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>Author</key>\n");
        xml.append("        <string>William Shakespeare</string>\n");
        xml.append("        <key>Lines</key>\n");
        xml.append("        <array>\n");
        xml.append("            <string>It is a tale told</string>\n");
        xml.append("            <string>by an idiot</string>\n");
        xml.append("        </array>\n");
        xml.append("        <key>Birthdate</key>\n");
        xml.append("        <integer>1564</integer>\n");
        xml.append("        <key>Active</key>\n");
        xml.append("        <false />\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = new PropertyList();
        plist.setProperty("Author", "William Shakespeare");
        plist.setProperty("Lines", ImmutableList.of("It is a tale told", "by an idiot"));
        plist.setProperty("Birthdate", 1564);
        plist.setProperty("Active", false);
        
        assertEquals(xml.toString(), new String(saveToByteArray(plist), Charsets.UTF_8.displayName()));
    }
    
    @Test
    public void testSaveAndLoadArray() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", ImmutableList.of(1, 2));
        byte[] saved = saveToByteArray(plist);
        PropertyList loaded = loadFromByteArray(saved);
        assertEquals(ImmutableList.of(1, 2), loaded.getProperty("a", null));
    }
    
    @Test
    public void testSaveAndLoadDate() {
        Date date = MockDataHelper.asDate("2011-01-20 00:00:00");
        PropertyList plist = new PropertyList();
        plist.setProperty("test", date);
        plist.setProperty("other", false);
        byte[] saved = saveToByteArray(plist);
        PropertyList loaded = loadFromByteArray(saved);
        assertEquals(date, loaded.getProperty("test", null));
    }
    
    @Test
    public void testSaveAndLoadBase64EncodedData() throws Exception {
        PropertyList plist = new PropertyList();
        byte[] testdata = "This is a test".getBytes(Charsets.UTF_8.displayName());
        plist.setProperty("test", testdata);
        byte[] saved = saveToByteArray(plist);
        PropertyList loaded = loadFromByteArray(saved);
        assertArrayEquals(testdata, loaded.getProperty("test", new byte[0]));
    }
    
    @Test
    public void testSaveAndLoadUTF8Characters() {
        PropertyList plist = new PropertyList();
        plist.setProperty("test", "eéuü");
        byte[] saved = saveToByteArray(plist);
        PropertyList loaded = loadFromByteArray(saved);
        assertEquals("eéuü", loaded.getProperty("test", null));
    }
    
    @Test
    public void testSaveAndLoadXMLCharacters() {
        PropertyList plist = new PropertyList();
        plist.setProperty("test", "a < b && c > d");
        byte[] saved = saveToByteArray(plist);
        PropertyList loaded = loadFromByteArray(saved);
        assertEquals("a < b && c > d", loaded.getProperty("test", null));
    }
    
    @Test
    public void testGetArrayProperty() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>Author</key>\n");
        xml.append("        <string>William Shakespeare</string>\n");
        xml.append("        <key>Lines</key>\n");
        xml.append("        <array>\n");
        xml.append("            <string>It is a tale told</string>\n");
        xml.append("            <string>by an idiot</string>\n");
        xml.append("        </array>\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = PropertyList.load(toStream(xml));
        List<?> array = (List<?>) plist.getProperty("Lines", null);
        assertEquals(2, array.size());
        assertEquals("It is a tale told", array.get(0));
        assertEquals("by an idiot", array.get(1));
    }
    
    @Test
    public void testArrayWithMixes() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>array</key>\n");
        xml.append("        <array>\n");
        xml.append("            <string>test</string>\n");
        xml.append("            <integer>2</integer>\n");
        xml.append("            <true />\n");
        xml.append("        </array>\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = PropertyList.load(toStream(xml));
        List<?> array = (List<?>) plist.getProperty("array", null);
        assertEquals(3, array.size());
        assertEquals("test", array.get(0));
        assertEquals(Integer.valueOf(2), array.get(1));
        assertEquals(Boolean.valueOf(true), array.get(2));
    }

    @Test
    public void testGetDictionaryProperty() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>Names</key>\n");
        xml.append("        <dict>\n");
        xml.append("            <key>Name</key>\n");
        xml.append("            <string>Humberto</string>\n");
        xml.append("        </dict>\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = PropertyList.load(toStream(xml));
        Map<?,?> dict = (Map<?,?>) plist.getProperty("Names", null);
        assertEquals(1, dict.size());
        assertEquals("Humberto", dict.get("Name"));
    }
    
    @Test
    public void testGetWithDefault() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>something</key>\n");
        xml.append("        <string>value</string>\n");
        xml.append("        <key>Some array</key>\n");
        xml.append("        <array>\n");
        xml.append("            <string>A</string>\n");
        xml.append("            <string>B</string>\n");
        xml.append("        </array>\n");
        xml.append("        <key>Some dict</key>\n");
        xml.append("        <dict>\n");
        xml.append("            <key>A</key>\n");
        xml.append("            <string>B</string>\n");
        xml.append("        </dict>\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = PropertyList.load(toStream(xml));
        assertEquals("value", plist.getProperty("something", "default"));
        assertEquals("default", plist.getProperty("something2", "default"));
        assertEquals(ImmutableList.of("A", "B"), 
                plist.getProperty("Some array", ImmutableList.of("default")));
        assertEquals(ImmutableList.of("default"), 
                plist.getProperty("Some array 2", ImmutableList.of("default")));
        assertEquals(ImmutableMap.of("A", "B"), 
                plist.getProperty("Some dict", ImmutableMap.of("default", "default")));
        assertEquals(ImmutableMap.of("default", "default"), 
                plist.getProperty("Some dict 2", ImmutableMap.of("default", "default")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCannotStorePropertyDataType() throws Exception {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", new URL("http://www.colorize.nl"));
    }
    
    @Test
    public void testToProperties() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", "test");
        plist.setProperty("b", 2);
        plist.setProperty("c", ImmutableList.of("i", "ii", "iii"));
        plist.setProperty("d", ImmutableMap.of(".", "..", "...", "...."));
        
        Properties properties = plist.toProperties();
        assertEquals(4, properties.size());
        assertEquals("test", properties.getProperty("a"));
        assertEquals("2", properties.getProperty("b"));
        assertEquals("i, ii, iii", properties.getProperty("c"));
        assertEquals("{.=.., ...=....}", properties.getProperty("d"));
    }
    
    @Test
    public void testFromProperties() {
        Properties properties = LoadUtils.toProperties("a", "1", "b", "2");
        PropertyList plist = PropertyList.fromProperties(properties);
        assertEquals("1", plist.getProperty("a", null));
        assertEquals("2", plist.getProperty("b", null));
    }
    
    @Test
    public void testRealExample() throws Exception {
        String xml = "";
        xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        xml += "<plist version=\"1.0\">\n";
        xml += "    <dict>\n";
        xml += "        <key>CFBundleName</key>\n";
        xml += "        <string>Test</string>\n";
        xml += "        <key>CFBundleVersion</key>\n";
        xml += "        <string>1.4.1</string>\n";
        xml += "        <key>Java</key>\n";
        xml += "        <dict>\n";
        xml += "            <key>WorkingDirectory</key>\n";
        xml += "            <string>$APP_PACKAGE/Contents/Resources/Java</string>\n";
        xml += "            <key>ClassPath</key>\n";
        xml += "            <string>$JAVAROOT/checksite.jar</string>\n";
        xml += "        </dict>\n";
        xml += "    </dict>\n";
        xml += "</plist>\n";
        
        PropertyList plist = PropertyList.load(toStream(xml));
        assertEquals("Test", plist.getProperty("CFBundleName", null));
        assertEquals("1.4.1", plist.getProperty("CFBundleVersion", null));
        assertEquals("$APP_PACKAGE/Contents/Resources/Java", plist.getProperty("Java.WorkingDirectory", ""));
        assertEquals("$JAVAROOT/checksite.jar", plist.getProperty("Java.ClassPath", ""));
        
        File tempFile = LoadUtils.getTempFile(".plist");
        plist.save(tempFile);
        assertEquals(xml, FileUtils.read(tempFile, Charsets.UTF_8));
    }
    
    @Test
    public void setNestedProperty() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", new HashMap<String,String>());
        plist.setProperty("a.b", "123");
        assertEquals(ImmutableMap.of("b", "123"), plist.getDictProperty("a"));
        assertEquals("123", plist.getProperty("a.b", ""));
    }
    
    @Test
    public void handleNullProperties() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", null);
        assertNull(plist.getProperty("a", "default"));
        assertEquals("default", plist.getProperty("b", "default"));
        
        StringBuilder expected = new StringBuilder();
        expected.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        expected.append("<plist version=\"1.0\">\n");
        expected.append("    <dict>\n");
        expected.append("        <key>a</key>\n");
        expected.append("        <string />\n");
        expected.append("    </dict>\n");
        expected.append("</plist>\n");
        
        byte[] saved = saveToByteArray(plist);
        assertEquals(expected.toString(), new String(saved, Charsets.UTF_8)); 
    }
    
    @Test
    public void testLoadEmptyCollections() throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        xml.append("<plist version=\"1.0\">\n");
        xml.append("    <dict>\n");
        xml.append("        <key>a</key>\n");
        xml.append("        <array/>\n");
        xml.append("        <key>b</key>\n");
        xml.append("        <dict/>\n");
        xml.append("    </dict>\n");
        xml.append("</plist>\n");
        
        PropertyList plist = PropertyList.load(toStream(xml));
        assertEquals(ImmutableList.of(), plist.getArrayProperty("a"));
        assertEquals(ImmutableMap.of(), plist.getDictProperty("b"));
    }
    
    @Test
    public void testSaveAsProperties() throws IOException {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", "test");
        plist.setProperty("b", "123");
        plist.setProperty("c", "false");
        plist.setProperty("d", MockDataHelper.asDate(2013, 02, 21));
        
        String expected = "";
        expected += "a=test\n";
        expected += "b=123\n";
        expected += "c=false\n";
        expected += "d=2013-02-21T00:00:00\n";
        
        File tempFile = LoadUtils.getTempFile(".properties");
        plist.saveAsProperties(tempFile);
        assertEquals(expected, FileUtils.read(tempFile, Charsets.UTF_8));
    }
    
    @Test(expected = ClassCastException.class)
    public void testIncompatibleDefaultValue() {
        PropertyList plist = new PropertyList();
        plist.setProperty("a", "test");
        plist.getProperty("a", 123);
    }
    
    @Test
    public void testNonexistentParentProperty() {
        PropertyList plist = new PropertyList();
        assertEquals("default", plist.getProperty("a.b", "default"));
        plist.setProperty("c.d", "test");
        assertEquals("test", plist.getProperty("c.d", "default"));
    }
    
    @Test
    public void testSaveAndLoadSet() throws IOException {
        Set<String> data = ImmutableSet.of("a", "b", "c");
        PropertyList plist = new PropertyList();
        plist.setProperty("test", data);
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        plist.save(buffer);
        
        PropertyList reloaded = PropertyList.load(new ByteArrayInputStream(buffer.toByteArray()));
        List<String> reloadedData = reloaded.getArrayProperty("test");
        
        assertEquals(data, ImmutableSet.copyOf(reloadedData));
    }
    
    @Test
    public void testSaveAndLoadMultimap() throws IOException {
        Map<String, List<String>> data = new HashMap<>();
        data.put("a", ImmutableList.of("1"));
        data.put("b", ImmutableList.of("2", "3"));
        
        PropertyList plist = new PropertyList();
        plist.setProperty("test", data);
        
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        plist.save(buffer);
        
        PropertyList reloaded = PropertyList.load(new ByteArrayInputStream(buffer.toByteArray()));
        Map<String, List<String>> reloadedData = reloaded.getDictProperty("test");
        
        assertEquals(ImmutableList.of("1"), reloadedData.get("a"));
        assertEquals(ImmutableList.of("2", "3"), reloadedData.get("b"));
        assertNull(reloadedData.get("c"));
    }
    
    private PropertyList loadFromByteArray(byte[] bytes) {
        try {
            return PropertyList.load(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    private byte[] saveToByteArray(PropertyList plist) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            plist.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    private ByteArrayInputStream toStream(CharSequence xml) {
        try {
            byte[] bytes = xml.toString().getBytes(Charsets.UTF_8.displayName());
            return new ByteArrayInputStream(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
