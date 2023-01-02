//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReflectionUtilsTest {
    
    @Test
    public void testGetProperty() {
        ReflectionSubject subject = new ReflectionSubject();
        subject.firstProperty = "first";
        subject.secondProperty = "second";
        
        assertEquals("first", ReflectionUtils.getProperty(subject, "firstProperty"));
        assertEquals("second", ReflectionUtils.getProperty(subject, "secondProperty"));
    }
    
    @Test
    public void testGetPropertyNamesAndTypes() {
        Map<String, Class<?>> properties = ReflectionUtils.getPropertyTypes(ReflectionSubject.class);
        
        assertEquals(3, properties.size());
        assertEquals(String.class, properties.get("firstProperty"));
        assertEquals(String.class, properties.get("secondProperty"));
        assertEquals(Object.class, properties.get("thirdProperty"));
        assertEquals(properties.keySet(), ReflectionUtils.getPropertyNames(ReflectionSubject.class));
    }
    
    @Test
    public void testGetProperties() {
        ReflectionSubject subject = new ReflectionSubject();
        subject.thirdProperty = 123;
        Map<String, Object> properties = ReflectionUtils.getProperties(subject);
        
        assertEquals(3, properties.size());
        assertEquals("first", properties.get("firstProperty"));
        assertEquals("second", properties.get("secondProperty"));
        assertEquals(123, properties.get("thirdProperty"));
    }
    
    @Test
    public void testSetProperty() {
        ReflectionSubject subject = new ReflectionSubject();
        ReflectionUtils.setProperty(subject, "firstProperty", "test");
        ReflectionUtils.setProperty(subject, "secondProperty", "test2");
        
        assertEquals("test", subject.firstProperty);
        assertEquals("test2", subject.secondProperty);
    }
    
    @Test
    public void testSetPropertyWrongType() {
        ReflectionSubject subject = new ReflectionSubject();

        assertThrows(IllegalArgumentException.class, () -> {
            ReflectionUtils.setProperty(subject, "firstProperty", 1234);
        });
    }
    
    @Test
    public void testPropertyComparator() {
        ReflectionSubject firstSubject = new ReflectionSubject();
        firstSubject.firstProperty = "zzz";
        ReflectionSubject secondSubject = new ReflectionSubject();
        secondSubject.firstProperty = "bbb";
        
        List<ReflectionSubject> subjects = Arrays.asList(firstSubject, secondSubject);
        Comparator<ReflectionSubject> comparator = ReflectionUtils.getPropertyComparator("firstProperty");
        Collections.sort(subjects, comparator);
        
        assertEquals(2, subjects.size());
        assertEquals(secondSubject, subjects.get(0));
        assertEquals(firstSubject, subjects.get(1));
    }
    
    @Test
    public void testComparatorForNonComparableProperty() {
        ReflectionSubject firstSubject = new ReflectionSubject();
        ReflectionSubject secondSubject = new ReflectionSubject();

        assertThrows(ClassCastException.class, () -> {
            Comparator<ReflectionSubject> comparator = ReflectionUtils.getPropertyComparator("thirdProperty");
            Collections.sort(Arrays.asList(firstSubject, secondSubject), comparator);
        });
    }
    
    @Test
    public void testCallMethod() {
        ReflectionSubject subject = new ReflectionSubject();
        assertEquals("first", ReflectionUtils.callMethod(subject, "firstMethod"));
        assertEquals("second", ReflectionUtils.callMethod(subject, "secondMethod"));
    }
    
    @Test
    public void testGetMethodsWithAnnotation() {
        ReflectionSubject subject = new ReflectionSubject();
        List<Method> methods = ReflectionUtils.getMethodsWithAnnotation(subject, Deprecated.class);
        
        assertEquals(1, methods.size());
        assertEquals("secondMethod", methods.get(0).getName());
    }
    
    @Test
    public void testGetFieldsWithAnnotation() {
        ReflectionSubject subject = new ReflectionSubject();
        List<Field> fields = ReflectionUtils.getFieldsWithAnnotation(subject, Deprecated.class);
        
        assertEquals(1, fields.size());
        assertEquals("secondProperty", fields.get(0).getName());
    }

    private static class ReflectionSubject {
        public String firstProperty = "first";
        @Deprecated private String secondProperty = "second";
        public Object thirdProperty = new Object();
        @SuppressWarnings("unused")
        private static int staticProperty = 0; 
        
        @SuppressWarnings("unused")
        public String firstMethod() {
            return "first";
        }
        
        @Deprecated
        @SuppressWarnings("unused")
        public String secondMethod() {
            return "second";
        }
        
        public int incrementCounter(AtomicInteger counter) {
            counter.set(counter.get() + 1);
            return counter.get();
        }
    }
}
