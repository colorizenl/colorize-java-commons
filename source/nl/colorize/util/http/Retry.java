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
 * Generic retry logic for operations that can potentially fail, such as
 * network requests. This class will try the operation a number of "attempts",
 * where one attempt is effectively no retry, two attempts means the original
 * attempt followed by one retry attempt if necessary, and so on. Optionally,
 * a time delay can be added in between retry attempts. Once all attempts have
 * failed, the exception from the last attempt is thrown to the caller.
 */
public class Retry {

    private int attempts;
    private long delay;
    private boolean loggingEnabled;

    private static final Logger LOGGER = LogHelper.getLogger(Retry.class);

    private Retry(int attempts, long delay, boolean loggingEnabled) {
        Preconditions.checkArgument(attempts >= 1, "Must do at least 1 attempt");
        Preconditions.checkArgument(delay >= 0L, "Invalid delay: " + delay);

        this.attempts = attempts;
        this.delay = delay;
        this.loggingEnabled = loggingEnabled;
    }

    /**
     * Adds a delay that lasts for the specified value in milliseconds, which
     * is then applied before every retry attempts. Note the delay is
     * <em>not</em> applied to the initial attempt, so it is only applied when
     * the operation has failed at least once.
     */
    public Retry withDelay(long delay) {
        Preconditions.checkArgument(delay >= 0L, "Invalid delay: " + delay);
        this.delay = delay;
        return this;
    }

    /**
     * Disables the logging that normally occurs when retrying operations.
     */
    public Retry withoutLogging() {
        loggingEnabled = false;
        return this;
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
                }
            }

            prepareNextAttempt();
        }

        throw new ExecutionException("Operation failed after " + attempts + " attempts", thrown);
    }

    private void prepareNextAttempt() throws ExecutionException {
        if (delay > 0L) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                throw new ExecutionException("Retry delay interruped", e);
            }
        }
    }

    /**
     * Returns a {@link Retry} instance that will perform operations the
     * specified number of attempts. Note the original attempt is included
     * in this number, so the value of {@code attempts} needs to be at least
     * 2 to achieve actual retry logic.
     */
    public static Retry create(int attempts) {
        return new Retry(attempts, 0L, true);
    }
}
