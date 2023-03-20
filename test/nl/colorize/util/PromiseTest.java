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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        runInThread(200, () -> promise.resolve("A"));

        Thread.sleep(500);

        assertEquals(List.of("A"), values);
        assertEquals(0, errors.size());
    }

    @Test
    void errorHandler() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add, errors::add);

        runInThread(200, () -> promise.reject(new IllegalStateException("Operation failed")));

        Thread.sleep(500);

        assertEquals(0, values.size());
        assertEquals(1, errors.size());
    }

    @Test
    void multipleCallbacks() throws InterruptedException {
        List<String> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promise = new Promise<>();
        promise.then(values::add).then(values::add).thenCatch(errors::add);
        runInThread(200, () -> promise.resolve("A"));

        Thread.sleep(500);

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
        runInThread(200, () -> promise.resolve("A"));

        promise.then(values::add, errors::add);

        Thread.sleep(500);

        assertEquals(0, values.size());
        assertEquals(0, errors.size());
    }

    @Test
    void all() throws InterruptedException {
        List<List<String>> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promiseA = new Promise<>();
        runInThread(300, () -> promiseA.resolve("A"));

        Promise<String> promiseB = new Promise<>();
        runInThread(100, () -> promiseB.resolve("B"));

        Promise.all(promiseA, promiseB)
            .then(values::add)
            .thenCatch(errors::add);

        Thread.sleep(500);

        assertEquals("[[A, B]]", values.toString());
        assertEquals(0, errors.size());
    }

    @Test
    void allWithError() throws InterruptedException {
        List<List<String>> values = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();

        Promise<String> promiseA = new Promise<>();
        runInThread(300, () -> promiseA.resolve("A"));

        Promise<String> promiseB = new Promise<>();
        runInThread(100, () -> promiseB.reject(new IllegalStateException("Operation failed")));

        Promise.all(promiseA, promiseB)
            .then(values::add)
            .thenCatch(errors::add);

        Thread.sleep(500);

        assertEquals(0, values.size());
        assertEquals(1, errors.size());
    }

    @Test
    void getPendingResult() {
        Promise<String> promise = new Promise<>();

        assertFalse(promise.getValue().isPresent());
    }

    @Test
    void getResolvedResult() throws InterruptedException {
        Promise<String> promise = new Promise<>();
        runInThread(100, () -> promise.resolve("A"));

        assertFalse(promise.getValue().isPresent());

        Thread.sleep(500);

        assertTrue(promise.getValue().isPresent());
        assertEquals("A", promise.getValue().get());
    }

    @Test
    void throwExceptionForRejectedResult() throws InterruptedException {
        Promise<String> promise = new Promise<>();
        runInThread(100, () -> promise.reject(new IllegalStateException("Operation failed")));

        Thread.sleep(500);

        assertThrows(RuntimeException.class, () -> promise.getValue());
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
