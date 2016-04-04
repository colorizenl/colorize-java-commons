//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import com.google.common.base.Charsets;
import com.google.common.io.Closeables;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.colorize.util.Platform;
import nl.colorize.util.ResourceFile;

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
	 * @throws JDOMException if the XML cannot be parsed.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static Document parse(InputStream stream) throws JDOMException, IOException {
		Document document = null;
		try {
			document = getSaxBuilder().build(stream);
		} finally {
			Closeables.close(stream, true);
		}
		return document;
	}
	
	/**
	 * Parses XML from a reader. The reader is closed afterwards.
	 * @throws JDOMException if the XML cannot be parsed.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static Document parse(Reader source) throws JDOMException, IOException {
		Document document = null;
		try {
			document = getSaxBuilder().build(source);
		} finally {
			Closeables.close(source, true);
		}
		return document;
	}
	
	/**
	 * Parses XML from a file. 
	 * @throws JDOMException if the XML cannot be parsed.
	 * @throws IOException if an I/O error occurs while reading.
	 */
	public static Document parse(File source) throws JDOMException, IOException {
		InputStream stream = new FileInputStream(source);
		return parse(stream);
	}
	
	/**
	 * Parses XML from a resource file. 
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
	 * @throws IOException if an I/O error occurs while writing.
	 */
	public static void write(Document document, OutputStream dest) throws IOException {
		try {
			getOutputter().output(document, dest);
		} finally {
			Closeables.close(dest, true);
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
	public static Element preserveWhitespace(Element element) {
		element.setAttribute("space", "preserve", Namespace.XML_NAMESPACE);
		return element;
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
	
	/**
	 * Prevents JDOM from loading DTDs from the actual URL listed in the XML file,
	 * since validation is disabled anyway and this introduces a potential performance
	 * problem.
	 */
	private static class NoOpEntityResolver implements EntityResolver {

		public InputSource resolveEntity(String publicId, String systemId) 
				throws SAXException, IOException {
			return new InputSource(new ByteArrayInputStream(new byte[0]));
		}
	}
}
