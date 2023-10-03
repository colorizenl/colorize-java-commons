//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PromiseTest {

    @Test
    void callbackAfterOperation() {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = Subscribable.of("A")
            .subscribe(values::add, errors::add)
            .toPromise();

        assertEquals(List.of("A"), values);
        assertEquals(0, errors.size());
        assertEquals("A", promise.getValue().orElse(null));
    }

    @Test
    void multipleCallbacks() {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = Subscribable.of("A")
            .subscribe(values::add)
            .subscribe(values::add)
            .subscribeErrors(errors::add)
            .toPromise();

        assertEquals(List.of("A", "A"), values);
        assertEquals(0, errors.size());
        assertEquals("A", promise.getValue().orElse(null));
    }

    @Test
    void getPendingResult() {
        Promise<String> promise = new Subscribable<String>().toPromise();

        assertFalse(promise.getValue().isPresent());
    }

    @Test
    void throwExceptionForRejectedResult() {
        Promise<String> promise = Subscribable.run(() -> String.valueOf(1 / 0)).toPromise();

        assertThrows(RuntimeException.class, () -> promise.getValue());
    }

    @Test
    void firstValueIsUsed() {
        Promise<String> promise = Subscribable.of("1", "2").toPromise();

        assertEquals("1", promise.getValue().orElse(null));
    }
}
