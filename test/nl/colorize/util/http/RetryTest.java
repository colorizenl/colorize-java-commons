//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.http;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryTest {

    @Test
    void doNotRetryIfSuccessful() throws ExecutionException {
        Retry retry = Retry.create(3);
        List<Integer> invocations = new ArrayList<>();

        int result = retry.attempt(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            return n;
        });

        assertEquals(1, result);
    }

    @Test
    void retryIfNotInitiallySuccessful() throws ExecutionException {
        Retry retry = Retry.create(3);
        List<Integer> invocations = new ArrayList<>();

        int result = retry.attempt(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n >= 2, "Fail!");
            return n;
        });

        assertEquals(2, result);
    }

    @Test
    void throwLastExceptionIfFailed() {
        Retry retry = Retry.create(3);
        List<Integer> invocations = new ArrayList<>();

        assertThrows(ExecutionException.class, () -> {
            retry.attempt(() -> {
                int n = invocations.size() + 1;
                invocations.add(n);
                Preconditions.checkArgument(n >= 5, "Fail!");
                return n;
            });
        });
    }

    @Test
    void handleCasesWhereOperationOccasionallyFails() throws ExecutionException {
        Retry retry = Retry.create(3);
        List<Integer> invocations = new ArrayList<>();

        int result = retry.attempt(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n == 3 || n == 5, "Fail!");
            return n;
        });

        assertEquals(3, result);
    }

    @Test
    void delay() throws ExecutionException {
        Retry retry = Retry.create(3).withDelay(500L);
        List<Integer> invocations = new ArrayList<>();

        int result = retry.attempt(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n >= 2, "Fail!");
            return n;
        });

        assertEquals(2, result);
    }
}
