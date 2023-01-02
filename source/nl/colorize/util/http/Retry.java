//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import nl.colorize.util.LogHelper;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic retry logic for operations that can potentially fail, such as network
 * requests. This class will try the operation a number of "attempts", where one
 * attempt is effectively no retry, two attempts means the original attempt
 * followed by one retry attempt if necessary, and so on. If all attempts have
 * failed, the exception from the last attempt is thrown to the caller.
 */
public class Retry {

    private int attempts;
    private boolean loggingEnabled;

    private static final Logger LOGGER = LogHelper.getLogger(Retry.class);

    public Retry(int attempts, boolean loggingEnabled) {
        Preconditions.checkArgument(attempts >= 1, "Must do at least 1 attempt");

        this.attempts = attempts;
        this.loggingEnabled = loggingEnabled;
    }

    public Retry(int attempts) {
        this(attempts, true);
    }

    /**
     * Attempts to perform the specified operation, for the number of attempts
     * as configured in this {@link Retry}.
     *
     * @throws ExecutionException if the operation is not successful. The thrown
     *         exception will include the original exception from the last failed
     *         attempt as its cause.
     */
    public <T> T attempt(Callable<T> operation) throws ExecutionException {
        Exception thrown = null;

        for (int i = 0; i < attempts; i++) {
            try {
                return operation.call();
            } catch (Exception e) {
                thrown = e;
                if (loggingEnabled) {
                    LOGGER.log(Level.WARNING, "Operation failed: " + e.getMessage());
                    LOGGER.log(Level.INFO, "Retrying operation");
                }
            }
        }

        throw new ExecutionException("Operation failed after " + attempts + " attempts", thrown);
    }
}
