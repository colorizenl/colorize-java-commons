//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import org.jdom2.Element;

/**
 * Converts Java object of type {@code <T>} to a XML representation. This can
 * be used to extend or change how objects are serialized by {@code XMLConverter}. 
 * @param <T> The type of object to serialize to XML.
 */
public interface XMLTypeConverter<T> {

    /**
     * Converts the specified to an XML element. The element's text, attributes,
     * and child elements are all determined during the conversion.
     * @param elementName Will be used as element name for the created element.
     */
    public Element convertObject(T obj, String elementName);
}
