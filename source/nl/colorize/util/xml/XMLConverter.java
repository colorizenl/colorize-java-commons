//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.jdom2.Document;
import org.jdom2.Element;

/**
 * Converts Java object structures into XML elements. This takes the approach
 * that the Gson library (https://code.google.com/p/google-gson/) uses for JSON
 * and applies it to XML. Like Gson, conversion does not require annotations.
 * This fixes two problematic aspects of the JAXB framework: it prevents XML-related
 * annotations from being added to non-XML-related classes, and allows you to also
 * convert classes for which you do not have the source code.
 * <p>
 * Unlike Gson, no reflection is used to convert arbitrary objects to XML. The types
 * supported by default are all primitives, strings, dates, and collections. Support
 * for additional types can be added by implementing the {@link XMLTypeConverter}
 * interface.
 */
public class XMLConverter {
	
	private Map<Class<?>, XMLTypeConverter<?>> typeConverters;
	private String dateFormat;
	private String listEntryElementName;
	
	public XMLConverter() {
		typeConverters = new LinkedHashMap<Class<?>, XMLTypeConverter<?>>();
		dateFormat = "yyyy-MM-dd HH:mm:ss";
		listEntryElementName = "entry";
		
		registerPrimitiveTypeConverters();
		registerStandardTypeConverters();
		registerCollectionTypeConverters();
	}
	
	/**
	 * Adds or replaces support for objects of the specified type. The type converter
	 * will be used for any object where {@code obj instanceof type}.
	 */
	public <T> void registerTypeConverter(Class<? extends T> type, XMLTypeConverter<T> typeConverter) {
		typeConverters.put(type, typeConverter);
	}
	
	private void registerPrimitiveTypeConverters() {
		XMLTypeConverter<Object> primitiveTypeConverter = new XMLTypeConverter<Object>() {
			public Element convertObject(Object obj, String elementName) {
				if (obj == null) {
					return new Element(elementName);
				}
				return XMLHelper.createPropertyElement(elementName, String.valueOf(obj));
			}
		};
		
		List<Class<? extends Object>> primitiveTypes = ImmutableList.<Class<? extends Object>>of(
				Integer.class, Long.class, Float.class, Double.class, Boolean.class, 
				Character.class, Byte.class, Short.class);
		
		for (Class<? extends Object> primitiveType : primitiveTypes) {
			registerTypeConverter(primitiveType, primitiveTypeConverter);
		}
	}
	
	private void registerStandardTypeConverters() {
		registerTypeConverter(String.class, new XMLTypeConverter<String>() {
			public Element convertObject(String obj, String elementName) {
				return XMLHelper.createPropertyElement(elementName, obj);
			}
		});
		
		registerTypeConverter(Date.class, new XMLTypeConverter<Date>() {
			public Element convertObject(Date obj, String elementName) {
				return XMLHelper.createPropertyElement(elementName,
						new SimpleDateFormat(dateFormat).format(obj));
			}
		});
	}
	
	@SuppressWarnings("rawtypes")
	private void registerCollectionTypeConverters() {
		registerTypeConverter(Collection.class, new XMLTypeConverter<Collection>() {
			public Element convertObject(Collection obj, String elementName) {
				Element element = new Element(elementName);
				for (Object entry : obj) {
					element.addContent(convertObjectToXML(entry, listEntryElementName));
				}
				return element;
			}
		});
		
		registerTypeConverter(Map.class, new XMLTypeConverter<Map>() {
			public Element convertObject(Map obj, String elementName) {
				Element element = new Element(elementName);
				for (Object key : obj.keySet()) {
					element.addContent(convertObjectToXML(obj.get(key), key.toString()));
				}
				return element;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	private XMLTypeConverter<Object> getTypeConverterForObject(Object obj) {
		for (Class<?> type : typeConverters.keySet()) {
			if (type.isInstance(obj)) {
				return (XMLTypeConverter<Object>) typeConverters.get(type);
			}
		}
		
		throw new UnsupportedOperationException("Type not supported: " + obj.getClass());
	}
	
	/**
	 * Converts the specified object to XML.
	 * @param rootElementName Will be used as the element name for the root element
	 *        in the created XML document.
	 * @throws UnsupportedOperationException when no type support exists for the
	 *         object, or any of its children if the object is a hierarchy.
	 * @throws NullPointerException when {@code obj} is {@code null}.
	 */
	public Document toXML(Object obj, String rootElementName) {
		if (obj == null || rootElementName == null) {
			throw new NullPointerException();
		}
		
		Element rootElement = convertObjectToXML(obj, rootElementName);
		return new Document(rootElement);
	}
	
	private Element convertObjectToXML(Object obj, String elementName) {
		XMLTypeConverter<Object> typeConverter = getTypeConverterForObject(obj);
		return typeConverter.convertObject(obj, elementName);
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}
	
	public String getDateFormat() {
		return dateFormat;
	}

	public void setListEntryElementName(String listEntryElementName) {
		this.listEntryElementName = listEntryElementName;
	}
	
	public String getListEntryElementName() {
		return listEntryElementName;
	}
}
