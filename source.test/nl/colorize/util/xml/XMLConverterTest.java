//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import nl.colorize.util.Version;
import nl.colorize.util.mock.MockDataHelper;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for the {@code XMLConverter} class.
 */
public class XMLConverterTest {
    
    @Test
    public void testSerializeSimpleObjects() {
        XMLConverter converter = new XMLConverter();
        assertEquals("<a>test</a>", toXmlString(converter.toXML("test", "a")));
        assertEquals("<a />", toXmlString(converter.toXML("", "a")));
        assertEquals("<a>2</a>", toXmlString(converter.toXML(2, "a")));
        assertEquals("<a>2.3</a>", toXmlString(converter.toXML(2.3f, "a")));
        assertEquals("<a>true</a>", toXmlString(converter.toXML(true, "a")));
        assertEquals("<a>2014-08-01 00:00:00</a>", toXmlString(converter.toXML(
                MockDataHelper.asDate("2014-08-01"), "a")));
    }
    
    @Test
    public void testSerializeList() {
        List<String> list = new ArrayList<String>();
        list.add("first");
        list.add("second");
        
        String expected = "";
        expected += "<a>\n";
        expected += "    <element>first</element>\n";
        expected += "    <element>second</element>\n";
        expected += "</a>";
        
        XMLConverter converter = new XMLConverter();
        assertEquals(expected, toXmlString(converter.toXML(list, "a")));
    }
    
    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new LinkedHashMap<String, Integer>();
        map.put("a", 2);
        map.put("b", 3);
        
        String expected = "";
        expected += "<a>\n";
        expected += "    <a>2</a>\n";
        expected += "    <b>3</b>\n";
        expected += "</a>";
        
        XMLConverter converter = new XMLConverter();
        assertEquals(expected, toXmlString(converter.toXML(map, "a")));
    }
    
    @Test
    public void testSerializeArray() {
        XMLConverter converter = new XMLConverter();
        Document xml = converter.toXML(new int[] { 1, 7 }, "a");
        
        String expected = "";
        expected += "<a>\n";
        expected += "    <element>1</element>\n";
        expected += "    <element>7</element>\n";
        expected += "</a>";
        
        assertEquals(expected, toXmlString(xml));
    }

    @Test(expected=NullPointerException.class)
    public void testSerializeNullNotAllowed() {
        XMLConverter converter = new XMLConverter();
        converter.toXML(null, "a");
    }
    
    @Test
    public void testConvertComplexType() {
        XMLConverter converter = new XMLConverter();
        Document xml = converter.toXML(Version.parse("1.2.3"), "a");
        
        String expected = "";
        expected += "<a>\n";
        expected += "    <versionString>1.2.3</versionString>\n";
        expected += "    <digits>\n";
        expected += "        <element>1</element>\n";
        expected += "        <element>2</element>\n";
        expected += "        <element>3</element>\n";
        expected += "    </digits>\n";
        expected += "</a>";
        
        assertEquals(expected, toXmlString(xml));
    }
    
    @Test
    public void testRegisterCustomTypeSupport() {
        XMLConverter converter = new XMLConverter();
        converter.registerTypeConverter(Locale.class, new XMLTypeConverter<Locale>() {
            public Element convertObject(Locale obj, String elementName) {
                Element element = new Element(elementName);
                element.addContent(XMLHelper.createPropertyElement("language", obj.getLanguage()));
                element.addContent(XMLHelper.createPropertyElement("country", obj.getCountry()));
                return element;
            }
        });
        
        String expected = "";
        expected += "<locale>\n";
        expected += "    <language>nl</language>\n";
        expected += "    <country>NL</country>\n";
        expected += "</locale>";
        
        assertEquals(expected, toXmlString(converter.toXML(new Locale("nl", "NL"), "locale")));
    }
    
    @Test
    public void testOverrideStandardBehaviorForType() {
        XMLConverter converter = new XMLConverter();
        converter.registerTypeConverter(Long.class, new XMLTypeConverter<Long>() {
            public Element convertObject(Long obj, String elementName) {
                return new Element("b");
            }
        });
        
        assertEquals("<b />", toXmlString(converter.toXML(123L, "a")));
    }
    
    private String toXmlString(Document xml) {
        return XMLHelper.toString(xml.getRootElement());
    }
}
