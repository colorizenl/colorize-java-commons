//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util;

/**
 * A task that is called during or after another task. This is a task interface
 * comparable with {@link java.util.concurrent.Callable}. However, unlike
 * {@code Callable}, callbacks take input, produce no return value, and are not
 * expected to throw an exception.
 * @param <T> The type of the calling task's result. 
 */
public interface Callback<T> {
	
	//TODO once the minimum requirement for this library becomes Java 8,
	//     this class should either implement FunctionalInterface or be
	//     deprecated.

	/**
	 * Invoked by the calling task once that has computed a result. 
	 */
	public void call(T value);
}
