//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Callback interface that can either produce a response of type {@code <T>} or
 * an error. Only the response handler is required. By default, exceptions are
 * simply logged, but it is also possible to provide an explit exception handler.
 */
@FunctionalInterface
public interface Callback<T> {

    public void onResponse(T response);

    default void onError(Throwable error) {
        Logger logger = LogHelper.getLogger(Callback.class);
        logger.log(Level.WARNING, "Operation failed", error);
    }

    public static <T> Callback<T> from(Consumer<T> handler, Consumer<Throwable> errorHandler) {
        return new Callback<T>() {
            @Override
            public void onResponse(T response) {
                handler.accept(response);
            }

            @Override
            public void onError(Throwable error) {
                errorHandler.accept(error);
            }
        };
    }
}
