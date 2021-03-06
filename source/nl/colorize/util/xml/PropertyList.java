//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.xml;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import nl.colorize.util.ResourceFile;
import nl.colorize.util.TextUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Property lists serialize data to {@code .plist} XML files. Property lists are 
 * similar to {@code .properties} files, but support multiple data types instead 
 * of only strings, and support indexed and nested properties. This class supports 
 * the {@code .plist} <a href="http://en.wikipedia.org/wiki/Plist">XML file format 
 * version 1.0</a>.
 * <p>
 * Properties stored in a property list can have one of the following types:
 *
 * <ul>
 *   <li>&lt;string&gt; (represented by java.lang.String)
 *   <li>&lt;integer&gt; (represented by java.lang.Integer)
 *   <li>&lt;real&gt; (represented by java.lang.Double)
 *   <li>&lt;true&gt; and &lt;false&gt; (represented by java.lang.Boolean)
 *   <li>&lt;date&gt; (represented by java.util.Date, stored as a ISO 8601 date)
 *   <li>&lt;data&gt; (represented by byte[], stored Base64-encoded)
 *   <li>&lt;array&gt; (represented by java.util.List)
 *   <li>&lt;dict&gt; (represented by java.util.Map)
 * </ul>
 */
public class PropertyList {
    
    private Map<String,Object> rootProperty;
    
    private static final String PLIST_FILE_FORMAT_VERSION = "1.0";
    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    private enum PropType {
        STRING(String.class),
        INTEGER(Integer.class),
        REAL(Float.class),
        BOOLEAN(Boolean.class),
        DATE(Date.class),
        DATA(byte[].class),
        ARRAY(List.class),
        DICT(Map.class);
        
        private Class<?> javaClass;
        
        private PropType(Class<?> javaClass) {
            this.javaClass = javaClass;
        }
        
        public String getTagName(Object value) {
            if (value instanceof Boolean) {
                return value.toString();
            }
            return name().toLowerCase();
        }
    }

    /**
     * Creates a new property list that initially has an empty &lt;dict&gt; as
     * root property.
     */
    public PropertyList() {
        rootProperty = new LinkedHashMap<>();
    }
    
    public Map<String,Object> getRootProperty() {
        return rootProperty;
    }
    
    /**
     * Returns the value of the property located at the specified path. The path
     * refers to named properties in the root dictionary, nested properties are
     * separated by dots.
     *
     * @return The property's value, or {@code defaultValue} if it doesn't exist.
     *
     * @throws ClassCastException if the property's value is of a type that is
     *         not compatible with {@code defaultType}.
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String path, T defaultValue) {
        T value = defaultValue;
        if (rootProperty.containsKey(path)) {
            value = (T) rootProperty.get(path);
        } else if (path.indexOf('.') != -1) {
            String[] pathComponents = path.split("\\.");
            Map<String, Object> dict = locateDict(pathComponents, false);
            String lastComponent = pathComponents[pathComponents.length - 1];
            if (dict != null && dict.containsKey(lastComponent)) {
                value = (T) dict.get(lastComponent);
            }
        }
        
        // Make sure that the value is compatible with the default type
        if (value != null && defaultValue != null) {
            getPropType(defaultValue).javaClass.cast(value);
        }
        
        return value;
    }
    
    /**
     * Returns the value of the array property located at the specified path, or
     * an empty list if it doesn't exist.
     *
     * @throws ClassCastException if the property exists but is not &lt;array&gt;.
     */
    public <T> List<T> getArrayProperty(String path) {
        return getProperty(path, new ArrayList<T>());
    }
    
    /**
     * Returns the value of the dictionary property located at the specified path, or
     * an empty map if it doesn't exist.
     *
     * @throws ClassCastException if the property exists but is not &lt;dict&gt;.
     */
    public <K,V> Map<K,V> getDictProperty(String path) {
        return getProperty(path, new LinkedHashMap<K,V>()); 
    }
    
    private PropType getPropType(Object value) {
        if (value == null) {
            // Cannot determine the type null is supposed to represent
            return PropType.STRING;
        }
        
        for (PropType type : PropType.values()) {
            if (type.javaClass.isInstance(value)) {
                return type;
            }
        }
        
        throw new IllegalArgumentException("No property type: " + value.getClass());
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> locateDict(String[] pathComponents, boolean createIfAbsent) {
        Map<String,Object> dict = rootProperty;
        for (int i = 0; i < pathComponents.length - 1; i++) {
            Map<String,Object> subDict = (Map<String,Object>) dict.get(pathComponents[i]);
            if (createIfAbsent && (subDict == null)) {
                subDict = new LinkedHashMap<String,Object>();
                dict.put(pathComponents[i], subDict);
            }
            dict = subDict;
        }
        return dict;
    }

    /**
     * Changes the value of the property located at the specified path.
     *
     * @throws IllegalArgumentException if the value is of a type that cannot be
     *         stored in a property list.
     * @throws ClassCastException if a nested property's parent is not a &lt;dict&gt;.
     */
    public void setProperty(String path, Object value) {
        value = processValueType(value);
        checkTypeSupported(value);
        
        if (path.indexOf('.') == -1) {
            rootProperty.put(path, value);    
        } else {
            String[] pathComponents = path.split("\\.");
            Map<String,Object> dict = locateDict(pathComponents, true);
            dict.put(pathComponents[pathComponents.length - 1], value);
        }
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object processValueType(Object value) {
        if (value instanceof Set<?>) {
            List<?> convertedToList = new ArrayList<>();
            convertedToList.addAll((Set) value);
            return convertedToList;
        }
        
        return value;
    }

    private void checkTypeSupported(Object value) {
        PropType type = getPropType(value);
        
        if (type == PropType.ARRAY) {
            for (Object element : (List<?>) value) {
                checkTypeSupported(element);
            }
        } else if (type == PropType.DICT) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                checkTypeSupported(entry.getKey());
                checkTypeSupported(entry.getValue());
            }
        }
    }
    
    /**
     * Converts this property list to a {@code Properties} object. All properties
     * in this property list will be converted to strings, inclusing nested ones.
     */
    public Properties toProperties() {
        Properties properties = new Properties();
        properties.putAll(toMap());
        return properties;
    }
    
    private Map<String,String> toMap() {
        LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
        for (Map.Entry<?,?> entry : ((Map<?,?>) rootProperty).entrySet()) {
            map.put((String) entry.getKey(), serializePropertyValue(entry.getValue()));
        }
        return map;
    }
    
    /**
     * Writes this property list to an {@code .plist} XML file. The stream is
     * closed afterwards.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public void save(OutputStream stream) throws IOException {
        Document xml = serialize();
        XMLHelper.write(xml, stream);
    }
    
    /**
     * Writes this property list to an {@code .plist} XML file.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public void save(File plistFile) throws IOException {
        save(new FileOutputStream(plistFile));
    }
    
    private Document serialize() {
        Element rootElement = new Element("plist");
        rootElement.setAttribute("version", PLIST_FILE_FORMAT_VERSION);
        rootElement.addContent(serializeProperty(rootProperty));
        return new Document(rootElement);
    }
    
    private Element serializeProperty(Object property) {
        PropType type = getPropType(property);

        if (type == PropType.ARRAY) {
            return serializeArrayProperty((List<?>) property);
        } else if (type == PropType.DICT) {
            return serializeDictProperty((Map<?,?>) property);
        } else {
            Element propertyElement = new Element(type.getTagName(property));
            propertyElement.addContent(serializePropertyValue(property));
            return propertyElement;
        }
    }
    
    private Element serializeArrayProperty(List<?> property) {
        Element arrayElement = new Element(PropType.ARRAY.getTagName(property));
        for (Object child : property) {
            arrayElement.addContent(serializeProperty(child));
        }
        return arrayElement;
    }

    private Element serializeDictProperty(Map<?,?> property) {
        Element dictElement = new Element(PropType.DICT.getTagName(property));
        for (Map.Entry<?,?> entry : property.entrySet()) {
            Element keyElement = new Element("key");
            keyElement.addContent((String) entry.getKey());
            dictElement.addContent(keyElement);
            dictElement.addContent(serializeProperty(entry.getValue()));
        }
        return dictElement;
    }

    private String serializePropertyValue(Object value) {
        PropType type = getPropType(value);

        switch (type) {
            case BOOLEAN : return "";
            case DATE : return new SimpleDateFormat(ISO_8601_DATE_FORMAT).format((Date) value);
            case DATA : return BaseEncoding.base64Url().encode((byte[]) value);
            case ARRAY : return TextUtils.removeLeadingAndTrailing(value.toString(), "[", "]");
            default : return (value != null) ? value.toString() : "";
        }
    }

    /**
     * Writes this property list to a {@code .properties} file. The property
     * list is converted as described in {@link #toProperties()}.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public void saveAsProperties(OutputStream stream) throws IOException {
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, Charsets.UTF_8));

        try (writer) {
            for (Map.Entry<String,String> entry : toMap().entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }
    
    /**
     * Writes this property list to a {@code .properties} file. The property
     * list is converted as described in {@link #toProperties()}.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public void saveAsProperties(File propertiesFile) throws IOException {
        saveAsProperties(new FileOutputStream(propertiesFile));
    }
    
    /**
     * Loads a property list from a {@code .plist} XML file. The stream is
     * closed afterwards.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    public static PropertyList load(InputStream stream) throws IOException {
        try (stream) {
            Document xml = XMLHelper.parse(stream);
            if (!isPropertyListXmlFile(xml)) {
                throw new IOException("XML file is not a property list");
            }
            
            PropertyList plist = new PropertyList();
            plist.rootProperty = parseDictElement(xml.getRootElement().getChildren().get(0));
            return plist;
        } catch (JDOMException e) {
            throw new IOException("XML parse error", e);
        }
    }
    
    private static boolean isPropertyListXmlFile(Document xml) {
        Element rootElement = xml.getRootElement();
        return rootElement.getName().equals("plist") &&
                rootElement.getChildren().size() >= 1 && 
                rootElement.getAttributeValue("version").equals(PLIST_FILE_FORMAT_VERSION);
    }

    private static Object parsePropertyElement(Element element) {
        String value = element.getText();
        PropType type = getTypeForTag(element.getName(), value);

        Map<PropType, Supplier<Object>> mapping = new HashMap<>();
        mapping.put(PropType.STRING, () -> value);
        mapping.put(PropType.INTEGER, () -> Integer.parseInt(value));
        mapping.put(PropType.REAL, () -> Float.parseFloat(value));
        mapping.put(PropType.BOOLEAN, () -> "true".equals(element.getName()));
        mapping.put(PropType.DATE, () -> parseDate(value));
        mapping.put(PropType.DATA, () -> BaseEncoding.base64Url().decode(value));
        mapping.put(PropType.ARRAY, () -> parseArrayElement(element));
        mapping.put(PropType.DICT, () -> parseDictElement(element));

        return mapping.get(type).get();
    }
    
    private static Date parseDate(String value) {
        try {
            return new SimpleDateFormat(ISO_8601_DATE_FORMAT).parse(value);
        } catch (ParseException e) {
            throw new IllegalStateException("Invalid ISO 8601 date: " + value);
        }
    }

    private static List<?> parseArrayElement(Element element) {
        List<Object> result = new ArrayList<Object>();
        for (Element childElement : element.getChildren()) {
            result.add(parsePropertyElement(childElement));
        }
        return result;
    }
    
    private static Map<String,Object> parseDictElement(Element element) {
        Map<String,Object> result = new LinkedHashMap<String,Object>();
        List<Element> children = element.getChildren();
        for (int i = 0; i < children.size(); i += 2) {
            result.put(children.get(i).getText(), parsePropertyElement(children.get(i + 1)));
        }
        return result;
    }
    
    private static PropType getTypeForTag(String tag, Object value) {
        if (tag.equals("true") || tag.equals("false")) {
            return PropType.BOOLEAN;
        }
        for (PropType type : PropType.values()) {
            if (type.getTagName(value).equals(tag)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown property tag: " + tag);
    }

    /**
     * Loads a property list from a {@code .plist} XML file.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    public static PropertyList load(File plistFile) throws IOException {
        return load(new FileInputStream(plistFile));
    }
    
    /**
     * Loads a property list from a {@code .plist} XML file.
     *
     * @throws IOException if an I/O error occurs while reading.
     */
    public static PropertyList load(ResourceFile plistFile) throws IOException {
        return load(plistFile.openStream());
    }
    
    /**
     * Converts an existing {@code Properties} object to a property list. The
     * result will have a root directory with properties of type string.
     */
    public static PropertyList fromProperties(Properties properties) {
        PropertyList plist = new PropertyList();
        for (Map.Entry<Object,Object> entry : properties.entrySet()) {
            plist.setProperty((String) entry.getKey(), (String) entry.getValue());
        }
        return plist;
    }
}
