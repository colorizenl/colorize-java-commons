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

class PromiseTest {

    @Test
    void callbackAfterOperation() {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = Promise.resolveWith("A");
        promise.then(values::add, errors::add);

        assertEquals(List.of("A"), values);
        assertEquals(0, errors.size());
    }

    @Test
    void callbackBeforeOperation() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add, errors::add);
        runInThread(500, () -> promise.resolve("A"));

        Thread.sleep(1000);

        assertEquals(List.of("A"), values);
        assertEquals(0, errors.size());
    }

    @Test
    void errorHandler() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add, errors::add);

        runInThread(500, () -> {
            try {
                throw new IllegalStateException("Operation failed");
            } catch (IllegalStateException e) {
                promise.reject(e);
            }
        });

        Thread.sleep(1000);

        assertEquals(0, values.size());
        assertEquals(1, errors.size());
    }

    @Test
    void multipleCallbacks() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add).then(values::add).thenCatch(errors::add);
        runInThread(500, () -> promise.resolve("A"));

        Thread.sleep(1000);

        assertEquals(List.of("A", "A"), values);
        assertEquals(0, errors.size());
    }

    @Test
    void thenMapper() {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = Promise.resolveWith("A");
        promise.thenMap(value -> value + "1").then(values::add).thenCatch(errors::add);

        assertEquals(List.of("A1"), values);
        assertEquals(0, errors.size());
    }

    @Test
    void cancel() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add).then(values::add).thenCatch(errors::add);
        promise.cancel();
        runInThread(500, () -> promise.resolve("A"));

        promise.then(values::add, errors::add);

        Thread.sleep(1000);

        assertEquals(0, values.size());
        assertEquals(0, errors.size());
    }

    private void runInThread(long delay, Runnable task) {
        Thread otherThread = new Thread(() -> {
            try {
                Thread.sleep(delay);
                task.run();
            } catch (InterruptedException e) {
                throw new AssertionError("Operation failed", e);
            }
        });

        otherThread.start();
    }
}
