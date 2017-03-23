//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method to accept action events sent to it by
 * {@link ActionDelegate}.
 * <p>
 * <strong>Warning</strong>: The functionality provided by this class is no 
 * longer needed in Java 8, as action handlers for Swing can be created much 
 * more easily using lambda expressions and/or method references. The only
 * reason this class is not deprecated is because this library still supports 
 * environments in which Java 8 is not yet available. 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action {
	
	//TODO deprecate this class as soon as Java 8 becomes the
	//     minimum requirement for this library. Refer to the
	//     warning in the class documentation for details.
}
