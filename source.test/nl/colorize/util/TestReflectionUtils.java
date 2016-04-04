//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2009-2016 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

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
	
	private static class ReflectionSubject {
		public String firstProperty = "first";
		private String secondProperty = "second";
		public Object thirdProperty = new Object();
		
		@SuppressWarnings("unused")
		public String firstMethod() {
			return "first";
		}
		
		@Deprecated
		@SuppressWarnings("unused")
		public String secondMethod() {
			return "second";
		}
	}
}
