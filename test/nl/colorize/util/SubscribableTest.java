//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscribableTest {

    @Test
    void subscribe() {
        List<String> received = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();
        subject.subscribe(received::add);
        subject.next("a");
        subject.next("b");

        assertEquals(List.of("a", "b"), received);
    }

    @Test
    void dontReceiveAfterDispose() {
        List<String> received = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();
        Consumer<String> subscriber = received::add;

        subject.subscribe(subscriber);
        subject.next("a");
        subject.dispose();
        subject.next("b");

        assertEquals(List.of("a"), received);
    }

    @Test
    void disposeDuringProcessing() {
        List<String> received = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();

        subject.subscribe(event -> {
            received.add(event);
            if (event.equals("b")) {
                subject.dispose();
            }
        });

        subject.next("a");
        subject.next("b");
        subject.next("c");

        assertEquals(List.of("a", "b"), received);
    }

    @Test
    void useSupplier() {
        List<String> received = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();

        subject.subscribe(received::add, errors::add);
        subject.next(() -> "a");

        assertEquals(1, received.size());
        assertEquals(0, errors.size());
    }

    @Test
    void notifyError() {
        List<String> received = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();

        subject.subscribe(received::add, errors::add);
        subject.next(() -> "Whoops! " + (1 / 0));

        assertEquals(0, received.size());
        assertEquals(1, errors.size());
    }

    @Test
    void receivePreviousEventsUponSubscription() {
        List<String> received = new ArrayList<>();
        Subscribable<String> subject = new Subscribable<>();
        subject.next("1");
        subject.next("2");
        subject.subscribe(received::add);
        subject.next("3");
        subject.next("4");

        assertEquals("[1, 2, 3, 4]", received.toString());
    }

    @Test
    void fromValues() {
        List<String> received = new ArrayList<>();
        Subscribable<String> subject = Subscribable.of("1", "2", "3");
        subject.subscribe(received::add);

        assertEquals("[1, 2, 3]", received.toString());
    }

    @Test
    void exceptionWhenTryingToSubscribeAfterDispose() {
        Subscribable<String> subject = Subscribable.of("1", "2", "3");
        subject.subscribe(value -> System.out.println("a"));
        subject.dispose();

        assertThrows(IllegalStateException.class, () -> {
            subject.subscribe(value -> System.out.println("b"));
        });
    }

    @Test
    void fromOperation() {
        List<String> received = new CopyOnWriteArrayList<>();
        Subscribable<String> subject = Subscribable.run(() -> "1");
        subject.subscribe(received::add);

        assertEquals("[1]", received.toString());
    }

    @Test
    void inBackgroundThread() throws InterruptedException {
        List<String> received = new CopyOnWriteArrayList<>();
        Subscribable<String> subject = Subscribable.runAsync(() -> {
            Thread.sleep(500);
            return "1";
        });
        subject.subscribe(received::add);

        Thread.sleep(1000);

        assertEquals("[1]", received.toString());
    }

    @Test
    void map() {
        List<String> received = new ArrayList<>();

        Subscribable.of("a", "b")
            .subscribe(received::add)
            .map(x -> x + "2")
            .subscribe(received::add);

        assertEquals("[a, b, a2, b2]", received.toString());
    }

    @Test
    void mapException() {
        List<Integer> received = new ArrayList<>();
        List<Exception> originalErrors = new ArrayList<>();
        List<Exception> mappedErrors = new ArrayList<>();

        Subscribable.of(1, 2)
            .subscribeErrors(originalErrors::add)
            .map(x -> x / 0)
            .subscribe(received::add, mappedErrors::add);

        assertEquals(0, received.size());
        assertEquals(0, originalErrors.size());
        assertEquals(2, mappedErrors.size());
    }
}
