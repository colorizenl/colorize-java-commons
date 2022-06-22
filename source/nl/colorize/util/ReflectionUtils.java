//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Various utility and convenience methods for working with reflection. Unless
 * stated otherwise, all methods in this class will throw unchecked exceptions
 * if an error occurs while attempting to locate, access, or call elements.
 * This prevents using code from having to deal with the several types of
 * checked exceptions thrown by the reflection APIs. Also, in some cases methods
 * may throw {@code SecurityException} if the environment in which the
 * application is used does not allow certain forms of reflection. 
 */
public final class ReflectionUtils {

    private ReflectionUtils() {
    }
    
    /**
     * Accesses the object's property with the specified name using reflection, 
     * and returns its value.
     * @throws IllegalArgumentException if the object does not have a property
     *         with that name.
     * @throws SecurityException if the property is private or protected, and 
     *         the environment does not allow access to non-public properties.
     */
    public static Object getProperty(Object subject, String propertyName) {
        Field property = getProperty(subject.getClass(), propertyName);
        return getPropertyValue(subject, property);
    }
    
    private static Field getProperty(Class<?> forClass, String propertyName) {
        try {
            Field property = forClass.getDeclaredField(propertyName);
            property.setAccessible(true);
            return property;
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Class " + forClass.getName() + 
                    " does not have property " + propertyName);
        }
    }
    
    private static Object getPropertyValue(Object subject, Field property) {
        try {
            property.setAccessible(true);
            return property.get(subject);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Property " + property.getName() + " is not accessible");
        }
    }
    
    /**
     * Returns the names of the properties defined by the specified class. This
     * does include private and protected properties, but does not include any
     * properties defined by parent classes, or static or transient properties.
     */
    public static Set<String> getPropertyNames(Class<?> forClass) {
        return getPropertyTypes(forClass).keySet();
    }
    
    /**
     * Returns the names and types of the properties defined by the specified 
     * class. This does include private and protected properties, but does not 
     * include any properties defined by parent classes, or static or transient
     * properties.
     * <p>
     * This method does not retrieve the actual property values for an instance
     * of the class. Use {@link #getProperties(Object)} for that purpose.
     */
    public static Map<String, Class<?>> getPropertyTypes(Class<?> forClass) {
        Map<String, Class<?>> properties = new HashMap<>();
        for (Field property : getDeclaredProperties(forClass)) {
            properties.put(property.getName(), property.getType());
        }
        return properties;
    }
    
    private static List<Field> getDeclaredProperties(Class<?> forClass) {
        List<Field> properties = new ArrayList<>();
        for (Field property : forClass.getDeclaredFields()) {
            int modifiers = property.getModifiers();
            if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers)) {
                properties.add(property);
            }
        }
        return properties;
    }
    
    /**
     * Accesses an object's properties using reflection. The properties are
     * obtained using the approach described in {@link #getProperty(Object, String)}.
     * This does include private and protected properties, but does not include any
     * properties defined by parent classes, or static or transient properties.
     */
    public static Map<String, Object> getProperties(Object subject) {
        if (subject instanceof Class<?>) {
            throw new IllegalArgumentException("Trying to get properties from an object of type Class");
        }
        
        Map<String, Object> properties = new HashMap<>();
        for (Field property : getDeclaredProperties(subject.getClass())) {
            properties.put(property.getName(), getPropertyValue(subject, property));
        }
        return properties;
    }
    
    /**
     * Accesses the object's property with the specified name using reflection,
     * and updates its value. 
     * @throws IllegalArgumentException if the object does not have a property
     *         with that name, or if the property's type and type of {@code value}
     *         don't match.
     * @throws SecurityException if the property is private or protected, and 
     *         the environment does not allow access to non-public properties.
     */
    public static void setProperty(Object subject, String propertyName, Object value) {
        try {
            getProperty(subject.getClass(), propertyName).set(subject, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Property " + propertyName + " is not accessible");
        }
    }
    
    /**
     * Returns a comparator that compares objects based on the value of one of
     * their properties. Property access follows the approach used by
     * {@link #getProperty(Object, String)}, including the exceptions that might
     * be thrown when trying to access the property.
     */
    public static <T> Comparator<T> getPropertyComparator(String propertyName) {
        return new ReflectionPropertyComparator<T>(propertyName);
    }
    
    private static class ReflectionPropertyComparator<T> implements Comparator<T> {
        
        private String propertyName;
        
        public ReflectionPropertyComparator(String propertyName) {
            this.propertyName = propertyName;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public int compare(T a, T b) {
            Comparable propertyValueA = (Comparable) getProperty(a, propertyName);
            Comparable propertyValueB = (Comparable) getProperty(b, propertyName);
            if (propertyValueA == null || propertyValueB == null) {
                return 0;
            }
            return propertyValueA.compareTo(propertyValueB);
        }
    }
    
    /**
     * Calls the object's method with the specified name using reflection. The
     * types of the arguments passed to the method ({@code args}) are also used
     * to find the method with the requested parameter types.
     * @throws RuntimeException if the calling the method results in an exception.
     * @throws IllegalArgumentException if no method with that name exists,
     *         or if the number of parameter or the parameter type don't match.
     */
    public static Object callMethod(Object subject, String methodName, Object... args) {
        Method method = getMethod(subject, methodName, toParameterTypes(args));
        return callMethod(subject, method, args);
    }
    
    private static Object callMethod(Object subject, Method method, Object... args) {
        try {
            return method.invoke(subject, args);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception while calling method " + method.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Method " + method.getName() + " is not accessible");
        }
    }

    private static Method getMethod(Object subject, String methodName, Class<?>... parameterTypes) {
        try {
            return subject.getClass().getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Class " + subject.getClass().getName() + 
                    " does not have a method " + methodName);
        }
    }
    
    private static Class<?>[] toParameterTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }
        return types;
    }
    
    /**
     * Returns a list containing all of an object's methods that are marked with
     * the specified annotation.
     */
    public static List<Method> getMethodsWithAnnotation(Object subject, 
            Class<? extends Annotation> annotationClass) {
        List<Method> matches = new ArrayList<>();
        for (Method method : subject.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(annotationClass) != null) {
                matches.add(method);
            }
        }
        return matches;
    }
    
    /**
     * Returns a list containing all of an object's fields that are marked with
     * the specified annotation.
     */
    public static List<Field> getFieldsWithAnnotation(Object subject,
            Class<? extends Annotation> annotationClass) {
        List<Field> matches = new ArrayList<>();
        for (Field field : subject.getClass().getDeclaredFields()) {
            if (field.getAnnotation(annotationClass) != null) {
                matches.add(field);
            }
        }
        return matches;
    }
}
