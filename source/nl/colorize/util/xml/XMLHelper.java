//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import com.google.common.base.Charsets;
import nl.colorize.util.Platform;
import nl.colorize.util.ResourceFile;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/**
 * Contains convenience methods for working with XML using JDOM. 
 */
public final class XMLHelper {

    private XMLHelper() {
    }
    
    private static SAXBuilder getSaxBuilder() {
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setEntityResolver(new NoOpEntityResolver());
        return saxBuilder;
    }
    
    /**
     * Parses XML from a stream. The stream is closed afterwards.
     *
     * @throws JDOMException if the XML cannot be parsed.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static Document parse(InputStream stream) throws JDOMException, IOException {
        try (stream) {
            SAXBuilder saxBuilder = getSaxBuilder();
            return saxBuilder.build(stream);
        }
    }
    
    /**
     * Parses XML from a reader. The reader is closed afterwards.
     *
     * @throws JDOMException if the XML cannot be parsed.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static Document parse(Reader source) throws JDOMException, IOException {
        try (source) {
            SAXBuilder saxBuilder = getSaxBuilder();
            return saxBuilder.build(source);
        }
    }
    
    /**
     * Parses XML from a file.
     *
     * @throws JDOMException if the XML cannot be parsed.
     * @throws IOException if an I/O error occurs while reading.
     */
    public static Document parse(File source) throws JDOMException, IOException {
        try (InputStream stream = new FileInputStream(source)) {
            return parse(stream);
        }
    }
    
    /**
     * Parses XML from a resource file.
     *
     * @throws JDOMException if the XML cannot be parsed.
     * @throws RuntimeException if the resource file could not be parsed.
     */
    public static Document parse(ResourceFile source) throws JDOMException {
        try {
            return parse(source.openStream());
        } catch (IOException e) {
            throw new RuntimeException("Cannot load resource file: " + source, e);
        }
    }
    
    /**
     * Parses XML from a string.
     *
     * @throws JDOMException if the XML cannot be parsed.
     */
    public static Document parse(String xml) throws JDOMException {
        try {
            StringReader stringReader = new StringReader(xml);
            return parse(stringReader);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    private static XMLOutputter getOutputter() {
        Format formatter = Format.getPrettyFormat();
        formatter.setEncoding(Charsets.UTF_8.displayName());
        formatter.setIndent("    ");
        formatter.setLineSeparator(Platform.getLineSeparator());
        formatter.setExpandEmptyElements(false);
        formatter.setOmitDeclaration(false);
        formatter.setOmitEncoding(false);
        
        XMLOutputter outputter = new XMLOutputter(formatter);
        outputter.setFormat(formatter);
        return outputter;
    }
    
    /**
     * Writes XML to a stream using a default formatter and UTF-8 encoding.
     * The stream is closed afterwards.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void write(Document document, OutputStream dest) throws IOException {
        try (dest) {
            XMLOutputter output = getOutputter();
            output.output(document, dest);
        }
    }
    
    /**
     * Writes XML to a file using a default formatter and UTF-8 encoding.
     * @throws IOException if an I/O error occurs while writing.
     */
    public static void write(Document document, File dest) throws IOException {
        OutputStream stream = new FileOutputStream(dest);
        write(document, stream);
    }
    
    /**
     * Writes XML to a string using a default formatter.
     */
    public static String toString(Document document) {
        return getOutputter().outputString(document);
    }
    
    /**
     * Writes XML to a string using a default formatter.
     */
    public static String toString(Element element) {
        return getOutputter().outputString(element);
    }
    
    /**
     * Requests whitespace in {@code element}'s text to be preserved when printed.
     * This will add the attribute {@code xml:space="preserve"}.
     */
    public static void preserveWhitespace(Element element) {
        element.setAttribute("space", "preserve", Namespace.XML_NAMESPACE);
    }
    
    /**
     * Convenience method that creates an element with the specified name and 
     * text content, but with no attributes or namespace.
     */
    public static Element createPropertyElement(String name, String text) {
        Element element = new Element(name);
        element.setText(text);
        return element;
    }
    
    /**
     * Convenience method that creates an element and adds it to the specified
     * parent. The creates element will have a name and text content, but no
     * attributes or namespace. 
     */
    public static void addPropertyElement(Element parent, String name, String text) {
        parent.addContent(createPropertyElement(name, text));
    }

    private static XPathExpression<Element> compileXPath(String xpath) {
        return XPathFactory.instance().compile(xpath, Filters.element());
    }

    /**
     * Queries an XML document using the specified XPath expression and return
     * the first element found. If there are no results this will return
     * {@code null}.
     */
    public static Element findFirst(Document document, String xpath) {
        return compileXPath(xpath).evaluateFirst(document);
    }

    /**
     * Queries an XML document using this XPath expression and returns the text
     * content of the first element found. If there are no results this will
     * return {@code null}.
     */
    public static String findText(Document document, String xpath) {
        Element result = findFirst(document, xpath);
        if (result == null) {
            return null;
        }
        return result.getText();
    }

    /**
     * Queries an XML document using this XPath expression and returns a list
     * containing all found elements.
     */
    public static List<Element> findAll(Document document, String xpath) {
        return compileXPath(xpath).evaluate(document);
    }
    
    /**
     * Prevents JDOM from loading DTDs from the actual URL listed in the XML file,
     * since validation is disabled anyway and this introduces a potential performance
     * problem.
     */
    private static class NoOpEntityResolver implements EntityResolver {

        @Override
        public InputSource resolveEntity(String publicId, String systemId) {
            return new InputSource(new ByteArrayInputStream(new byte[0]));
        }
    }
}
