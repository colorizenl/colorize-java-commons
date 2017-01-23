//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Function;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for the {@code ReflectionUtils} class.
 */
public class TestReflectionUtils {
	
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
	
	@Test(expected=IllegalArgumentException.class)
	public void testSetPropertyWrongType() {
		ReflectionSubject subject = new ReflectionSubject();
		ReflectionUtils.setProperty(subject, "firstProperty", 1234);
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
	
	@Test(expected=ClassCastException.class)
	public void testComparatorForNonComparableProperty() {
		ReflectionSubject firstSubject = new ReflectionSubject();
		ReflectionSubject secondSubject = new ReflectionSubject();
		Comparator<ReflectionSubject> comparator = ReflectionUtils.getPropertyComparator("thirdProperty");
		Collections.sort(Arrays.asList(firstSubject, secondSubject), comparator);
		assertNotEquals(firstSubject.thirdProperty, secondSubject.thirdProperty);
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
	
	@Test
	public void testToMethodCallback() throws Exception {
		ReflectionSubject subject = new ReflectionSubject();
		AtomicInteger counter = new AtomicInteger();
		Callback<AtomicInteger> callback = ReflectionUtils.toMethodCallback(subject, 
				"incrementCounter", AtomicInteger.class);
		subject.incrementCounter(counter);
		callback.call(counter);
		callback.call(counter);
		
		assertEquals(3, counter.get());
		
		Function<AtomicInteger, Integer> callbackFunction = ReflectionUtils.toMethodCallback(
				subject, "incrementCounter", AtomicInteger.class, Integer.class);
		Integer returnValue = callbackFunction.apply(counter);
		
		assertEquals(4, returnValue.intValue());
		assertEquals(4, counter.get());
		
		Method method = ReflectionSubject.class.getMethod("incrementCounter", AtomicInteger.class);
		callbackFunction = ReflectionUtils.toMethodCallback(subject, method, AtomicInteger.class, 
				int.class);
		returnValue = callbackFunction.apply(counter);
		
		assertEquals(5, returnValue.intValue());
		assertEquals(5, counter.get());
	}
	
	@Test(expected=IllegalArgumentException.class)
	@Ignore // Temporarily disabled for compatibility with Google App Engine
	public void testMethodCallbackMustHaveCorrectArgumentType() throws Exception {
		ReflectionSubject subject = new ReflectionSubject();
		Method method = ReflectionSubject.class.getMethod("firstMethod");
		ReflectionUtils.toMethodCallback(subject, method, String.class, String.class);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMethodCallbackMustHaveCorrectReturnType() throws Exception {
		ReflectionSubject subject = new ReflectionSubject();
		Method method = ReflectionSubject.class.getMethod("incrementCounter", AtomicInteger.class);
		ReflectionUtils.toMethodCallback(subject, method, AtomicInteger.class, String.class);
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
