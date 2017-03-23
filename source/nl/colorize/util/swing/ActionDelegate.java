//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import nl.colorize.util.Callback;

/**
 * Delegates a Swing action event to a method. This class can be used to reduce
 * the number of anonymous inner classes that is usually needed in Swing
 * applications. Delegate methods should be annotated with {@link Action} and
 * should be public. They should either declare no arguments, or one argument of
 * type {@link java.lang.Object}, where the value of the argument is specified
 * when the {@code ActionDelegate} is created.
 * <p>
 * Usage example:
 * <p>
 * <pre>
 *     JButton sendButton = new JButton("Send");
 *     sendButton.addActionListener(new ActionDelegate(this, "foo"));
 *     
 *     &#64;Action
 *     public void foo() {
 *         ...
 *     }
 * </pre>
 * <p>
 * <strong>Warning</strong>: The functionality provided by this class is no 
 * longer needed in Java 8, as action handlers for Swing can be created much 
 * more easily using lambda expressions and/or method references. The only
 * reason this class is not deprecated is because this library still supports 
 * environments in which Java 8 is not yet available.  
 */
public class ActionDelegate implements ActionListener, Callback<ActionEvent> {
	
	//TODO deprecate this class as soon as Java 8 becomes the
	//     minimum requirement for this library. Refer to the
	//     warning in the class documentation for details.

	private Object owner;
	private Method delegateMethod;
	private Object eventArg;
	
	/**
	 * Creates an {@link ActionListener} that delegates to the specified method.
	 * If the delegate method expects an argument, the value of that argument
	 * will be the source of the event as described by
	 * {@link java.awt.event.ActionEvent#getSource()}.
	 * @param owner The object that contains the delegate method.
	 * @param methodName Name of the delegate method.
	 * @throws IllegalArgumentException if the delegate method does not exist, if 
	 *         it is not annotated with {@link Action}, or if it is not public.
	 */
	public ActionDelegate(Object owner, String methodName) {
		this(owner, methodName, null);
	}
	
	/**
	 * Creates an {@link ActionListener} that delegates to the specified method.
	 * The value of {@code eventArg} will be used as argument to pass to the
	 * delegate method.
	 * @param owner The object that contains the delegate method.
	 * @param methodName Name of the delegate method.
	 * @throws IllegalArgumentException if the delegate method does not exist, if
	 *         it is not annotated with {@link Action}, or if it is not public.
	 */
	public ActionDelegate(Object owner, String methodName, Object eventArg) {
		this.owner = owner;
		this.delegateMethod = findDelegateMethod(owner.getClass(), methodName);
		this.eventArg = eventArg;
	}
	
	private Method findDelegateMethod(Class<?> ownerClass, String methodName) {
		Method withNoArgs = lookupMethod(ownerClass, methodName, new Class[0]);
		if (withNoArgs != null && withNoArgs.isAnnotationPresent(Action.class)) {
			return withNoArgs;
		} else {
			Method withOneArg = lookupMethod(ownerClass, methodName, new Class[] {Object.class});
			if (withOneArg != null && withOneArg.isAnnotationPresent(Action.class)) {
				return withOneArg;
			} else {
				throw new IllegalArgumentException(String.format("Could not find delegate @Action " +
						"method named '%s' on the class '%s'", methodName, ownerClass.getName()));
			}
		}
	}
	
	private Method lookupMethod(Class<?> ownerClass, String name, Class<?>[] paramTypes) {
		try {
			Method method = ownerClass.getMethod(name, paramTypes);
			method.setAccessible(true);
			return method;
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
	
	public void actionPerformed(ActionEvent event) {
		call(event);
	}
	
	public void call(ActionEvent event) {
		try {
			if (expectsArg()) {
				delegateMethod.invoke(owner, getEventArgValue(event));
			} else {
				delegateMethod.invoke(owner);
			}
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No access to delegate method", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Delegate method invocation caused exception", e);
		}
	}
	
	private boolean expectsArg() {
		return delegateMethod.getParameterTypes().length > 0;
	}
	
	private Object getEventArgValue(ActionEvent event) {
		if (eventArg == null) {
			return event.getSource();
		} else {
			return eventArg;
		}
	}
}
