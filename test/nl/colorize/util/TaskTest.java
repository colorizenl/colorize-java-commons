//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
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
    void completeAsync() {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(t -> results.add(t.get()));

        Thread thread = new Thread(() -> task.complete("abc"));
        thread.start();

        assertEquals(ImmutableList.of("abc"), results);
    }

    @Test
    void failAsync() {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(t -> results.add(t.get()));

        Thread thread = new Thread(() -> task.fail(new RuntimeException("fail")));
        thread.start();

        assertEquals(Collections.emptyList(), results);
    }

    @Test
    void cancelAsync() {
        List<String> results = new ArrayList<>();
        Task<String> task = new Task<>();
        task.then(t -> results.add(t.get()));

        assertEquals(Collections.emptyList(), results);
    }
}
