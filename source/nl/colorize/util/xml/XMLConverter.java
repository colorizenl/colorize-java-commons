//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import com.google.gson.internal.Primitives;
import nl.colorize.util.ReflectionUtils;
import org.jdom2.Document;
import org.jdom2.Element;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts Java object structures into XML elements. This takes the approach
 * that the Gson library (https://code.google.com/p/google-gson/) uses for JSON
 * and applies it to XML. Like Gson, conversion does not require annotations.
 * This fixes two problematic aspects of the JAXB framework: it prevents XML-related
 * annotations from being added to non-XML-related classes, and allows you to also
 * convert classes for which you do not have the source code.
 * <p>
 * By default, all non-primitive types are serialized to XML by accessing their
 * properties using reflection. Support for custom serialization is available by
 * implementing the {@link XMLTypeConverter} interface.
 */
public class XMLConverter {
    
    private Map<Class<?>, XMLTypeConverter<?>> typeConverters;
    private String dateFormat;
    private String collectionElementName;
    
    public XMLConverter() {
        typeConverters = new LinkedHashMap<Class<?>, XMLTypeConverter<?>>();
        dateFormat = "yyyy-MM-dd HH:mm:ss";
        collectionElementName = "element";
        
        registerStandardTypeConverters();
    }
    
    /**
     * Adds or replaces support for objects of the specified type. The type converter
     * will be used for any object where {@code obj instanceof type}.
     */
    public <T> void registerTypeConverter(Class<? extends T> type, XMLTypeConverter<T> typeConverter) {
        typeConverters.put(type, typeConverter);
    }
    
    private void registerStandardTypeConverters() {
        registerTypeConverter(Date.class, (obj, elementName) -> {
            String formatted = new SimpleDateFormat(dateFormat).format(obj);
            return XMLHelper.createPropertyElement(elementName, formatted);
        });
        
        registerTypeConverter(Map.class, (obj, elementName) -> {
            Element element = new Element(elementName);
            for (Object key : obj.keySet()) {
                element.addContent(serializeObject(obj.get(key), key.toString()));
            }
            return element;
        });
    }
    
    /**
     * Converts the specified object to XML. The object will be serialized using
     * either the provided custom serialization format for the object's type, or
     * the default serialization format if no custom one has been registered.
     * @param rootElementName Will be used as the element name for the root element
     *        in the created XML document.
     * @throws NullPointerException when {@code obj} is {@code null}.
     */
    public Document toXML(Object obj, String rootElementName) {
        if (obj == null || rootElementName == null) {
            throw new NullPointerException();
        }
        
        Element rootElement = serializeObject(obj, rootElementName);
        return new Document(rootElement);
    }
    
    private Element serializeObject(Object obj, String elementName) {
        // Use a custom serialization format if one has been registered.
        XMLTypeConverter<Object> customTypeConverter = getTypeConverter(obj);
        if (customTypeConverter != null) {
            return customTypeConverter.convertObject(obj, elementName);
        }
        
        // Default serialization formats: for complex types, access the 
        // object's properties using reflection. For simple types,
        // serialize the elements to strings.
        if (isSimpleType(obj)) {
            return serializeSimpleType(obj, elementName);
        } else if (isCollectionType(obj)) {
            return serializeCollectionType(obj, elementName);
        } else {
            return serializeComplexTypeUsingReflection(obj, elementName);
        }
    }
    
    private Element serializeSimpleType(Object obj, String elementName) {
        if (obj == null) {
            return XMLHelper.createPropertyElement(elementName, "");
        }
        return XMLHelper.createPropertyElement(elementName, String.valueOf(obj));
    }
    
    private Element serializeCollectionType(Object obj, String elementName) {
        Collection<?> collection = null;
        if (obj instanceof Collection<?>) {
            collection = (Collection<?>) obj;
        } else if (obj.getClass().isArray()) {
            collection = convertPrimitiveArrayToList(obj);
        }
        
        Element element = new Element(elementName);
        for (Object collectionElement : collection) {
            element.addContent(serializeObject(collectionElement, collectionElementName));
        }
        return element;
    }

    private Element serializeComplexTypeUsingReflection(Object obj, String elementName) {
        Element element = new Element(elementName);
        for (Map.Entry<String, Object> prop : ReflectionUtils.getProperties(obj).entrySet()) {
            element.addContent(serializeObject(prop.getValue(), prop.getKey()));
        }
        return element;
    }
    
    private boolean isSimpleType(Object obj) {
        if (obj == null) {
            return true;
        }
        Class<?> type = obj.getClass();
        return Primitives.isPrimitive(type) || Primitives.isWrapperType(type) || type == String.class;
    }
    
    private boolean isCollectionType(Object obj) {
        return obj instanceof Collection<?> || obj.getClass().isArray();
    }

    @SuppressWarnings("unchecked")
    private <T> XMLTypeConverter<T> getTypeConverter(T obj) {
        for (Class<?> type : typeConverters.keySet()) {
            if (type.isInstance(obj)) {
                return (XMLTypeConverter<T>) typeConverters.get(type);
            }
        }
        return null;
    }
    
    private List<Object> convertPrimitiveArrayToList(Object primitiveArray) {
        List<Object> converted = new ArrayList<>();
        for (int i = 0; i < Array.getLength(primitiveArray); i++) {
            converted.add(Array.get(primitiveArray, i));
        }
        return converted;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }

    public void setCollectionElementName(String collectionElementName) {
        this.collectionElementName = collectionElementName;
    }
    
    public String getCollectionElementName() {
        return collectionElementName;
    }
}
