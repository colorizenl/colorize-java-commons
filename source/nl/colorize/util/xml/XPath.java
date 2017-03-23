//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * XPath expression that can be used to query XML documents parsed with JDOM.
 * This class is a wrapper around JDOM's XPath API (that in turn uses jaxen 
 * internally).
 */
public class XPath {

	private XPathExpression<Element> expression;
	
	protected XPath(XPathExpression<Element> expression) {
		this.expression = expression;
	}
	
	/**
	 * Queries an XML document using this XPath expression and returns the first
	 * element found. If there are no results this will return {@code null}.
	 */
	public Element findFirst(Document document) {
		return expression.evaluateFirst(document);
	}
	
	/**
	 * Queries an XML document using this XPath expression and returns a list
	 * containing all found elements.
	 */
	public List<Element> findAll(Document document) {
		return expression.evaluate(document);
	}
	
	/**
	 * Queries an XML document using this XPath expression and returns the text
	 * content of the first element found. If there are no results this will 
	 * return {@code null}.
	 */
	public String findText(Document document) {
		Element result = findFirst(document);
		if (result != null) {
			return result.getText();
		} else {
			return null;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof XPath) {
			return toString().equals(o.toString());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public String toString() {
		return expression.toString();
	}
	
	/**
	 * Creates an {@code XPath} instance from an XPath expression in text form.
	 * @throws IllegalArgumentException if the expression is not valid XPath.
	 */
	public static XPath parse(String expression) {
		XPathFactory xpathFactory = XPathFactory.instance();
		XPathExpression<Element> compiled = xpathFactory.compile(expression, Filters.element());
		return new XPath(compiled);
	}
}
