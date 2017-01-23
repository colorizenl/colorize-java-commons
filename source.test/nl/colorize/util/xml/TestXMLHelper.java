//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import java.io.File;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import nl.colorize.util.LoadUtils;
import nl.colorize.util.xml.XMLHelper;
import nl.colorize.util.xml.XPath;

import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for working with XML using the JDOM library and the {@code XMLHelper}
 * class.
 */
public class TestXMLHelper {

	@Test
	public void testParseFromString() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
		xml += "<a>\n";
		xml += "    <b>test</b>\n";
		xml += "    <c d=\"test attribute\" />\n";
		xml += "    <c>test 2</c>\n";
		xml += "</a>\n";
		
		Document dom = XMLHelper.parse(xml);
		
		Element a = dom.getRootElement();
		assertEquals("a", a.getName());
		assertEquals(3, a.getChildren().size());
		assertEquals(2, a.getChildren("c").size());
		
		Element b = a.getChild("b");
		assertEquals("test", b.getText());
		
		Element firstC = a.getChild("c");
		assertEquals("", firstC.getText());
		assertEquals("test attribute", firstC.getAttributeValue("d"));
		assertNull(firstC.getAttributeValue("e"));
	}
	
	@Test
	public void testParseUTF8() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
		xml += "<root>\n";
		xml += "    <child>\u25B2</child>\n";
		xml += "</root>\n";
		
		File tempFile = LoadUtils.getTempFile(".xml");
		Files.write(xml, tempFile, Charsets.UTF_8);
		
		Document loaded = XMLHelper.parse(tempFile);
		Element root = loaded.getRootElement();
		assertEquals("\u25B2", root.getChildText("child"));
	}
	
	@Test
	public void testOutput() throws Exception {
		Element first = new Element("first");
		first.setText("Test");
		
		Element second = new Element("second");
		second.setAttribute("q", "bla");
		second.setAttribute("f", "");
		
		Element root = new Element("root");
		root.addContent(first);
		root.addContent(second);
		
		Document dom = new Document(root);
		
		File tempFile = LoadUtils.getTempFile(".xml");
		XMLHelper.write(dom, tempFile);
		
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		expected += "<root>\n";
		expected += "    <first>Test</first>\n";
		expected += "    <second q=\"bla\" f=\"\" />\n";
		expected += "</root>\n";
		
		String contents = Files.toString(tempFile, Charsets.UTF_8);
		assertEquals(expected, contents);
	}
	
	@Test
	public void testPreserveWhitespace() {
		Element normal = new Element("normal");
		normal.setText("text");
		
		Element preserve = new Element("preserve");
		XMLHelper.preserveWhitespace(preserve);
		preserve.setText("    \ntext    \n");
		
		Element root = new Element("root");
		root.addContent(normal);
		root.addContent(preserve);
		
		String expected = "";
		expected += "<root>\n";
		expected += "    <normal>text</normal>\n";
		expected += "    <preserve xml:space=\"preserve\">    \ntext    \n</preserve>\n";
		expected += "</root>";
		
		assertEquals(expected, XMLHelper.toString(root));
	}
	
	@Test
	public void testAddPropertyElement() {
		Element parent = new Element("a");
		XMLHelper.addPropertyElement(parent, "b", "test");
		XMLHelper.addPropertyElement(parent, "c", "test");
		
		String expected = "";
		expected += "<a>\n";
		expected += "    <b>test</b>\n";
		expected += "    <c>test</c>\n";
		expected += "</a>";
		
		assertEquals(expected, XMLHelper.toString(parent));
	}
	
	@Test
	public void testXPath() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
		xml += "<a>\n";
		xml += "    <b>test</b>\n";
		xml += "    <c />\n";
		xml += "    <c />\n";
		xml += "    <c />\n";
		xml += "</a>\n";
		
		Document document = XMLHelper.parse(xml);
		
		assertEquals("b", XPath.parse("/a/b").findFirst(document).getName());
		assertEquals("test", XPath.parse("/a/b").findText(document));
		assertNull(XPath.parse("/a/b/c").findFirst(document));
		assertEquals("c", XPath.parse("//c").findFirst(document).getName());
		assertEquals(3, XPath.parse("//c").findAll(document).size());
		assertEquals(0, XPath.parse("//d").findAll(document).size());
	}
	
	@Test
	public void testDoNotLoadExternalDTD() throws Exception {
		String xml = "";
		xml += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		xml += "<!DOCTYPE plist PUBLIC \"-//Apple Computer//DTD PLIST 1.0//EN\" " +
				"\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n";
		xml += "<plist version=\"1.0\">\n";
		xml += "	<dict />\n";
		xml += "</plist>\n";
		
		Document document = XMLHelper.parse(xml);
		assertEquals("plist", document.getRootElement().getName());
	}
}
