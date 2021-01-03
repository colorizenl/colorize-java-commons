//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2021 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TaskTest {

    @Test
    void complete() {
        Task<String> task = new Task<>();
        task.complete("abc");

        assertEquals("abc", task.get());
    }

    @Test
    void cannotCompleteTwice() {
        assertThrows(IllegalStateException.class, () -> {
            Task<String> task = new Task<>();
            task.complete("abc");
            task.complete("def");
        });
    }

    @Test
    void fail() {
        assertThrows(RuntimeException.class, () -> {
            Task<String> task = new Task<>();
            task.fail(new RuntimeException("Failed"));
            task.get();
        });
    }

    @Test
    void cancel() {
        assertThrows(IllegalStateException.class, () -> {
            Task<String> task = new Task<>();
            task.cancel();
            task.get();
        });
    }

    @Test
    void completeAsync() throws InterruptedException {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(results::add);

        Thread thread = new Thread(() -> task.complete("abc"));
        thread.start();

        Thread.sleep(500);

        assertEquals(ImmutableList.of("abc"), results);
    }

    @Test
    void failAsync() {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(results::add);

        Thread thread = new Thread(() -> task.fail(new RuntimeException("fail")));
        thread.start();

        assertEquals(Collections.emptyList(), results);
    }

    @Test
    void cancelAsync() {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(results::add);

        assertEquals(Collections.emptyList(), results);
    }

    @Test
    void onlyErrorCallback() {
        List<String> results = new ArrayList<>();
        List<Exception> failed = new ArrayList<>();

        Task<String> task = new Task<>();
        task.error(failed::add);
        task.fail(new RuntimeException("failed"));

        assertEquals(0, results.size());
        assertEquals(1, failed.size());
    }

    @Test
    void pipe() {
        List<String> results = new ArrayList<>();

        Task<String> task = new Task<>();
        task.pipe(result -> result + "ABC").then(results::add);
        task.complete("123");

        assertEquals(ImmutableList.of("123ABC"), results);
    }

    @Test
    void pipeWithFailure() {
        List<String> results = new ArrayList<>();
        List<Exception> failed = new ArrayList<>();

        Task<String> task = new Task<>();
        task.pipe(result -> result + "ABC").then(results::add, failed::add);
        task.fail(new RuntimeException());

        assertEquals(0, results.size());
        assertEquals(1, failed.size());
    }
}
