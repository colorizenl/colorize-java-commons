//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        Subscriber<String> subscriber = received::add;

        subject.subscribe(subscriber);
        subject.next("a");
        subject.complete();
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
                subject.complete();
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
    void fromOperation() {
        List<String> received = new CopyOnWriteArrayList<>();
        Subscribable<String> subject = Subscribable.run(() -> "1");
        subject.subscribe(received::add);

        assertEquals("[1]", received.toString());
    }

    @Test
    void inBackgroundThread() throws InterruptedException {
        List<String> received = new CopyOnWriteArrayList<>();
        Subscribable<String> subject = Subscribable.runBackground(() -> {
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

    @Test
    void filter() {
        List<String> received = new ArrayList<>();

        Subscribable.of("a", "b", "c")
            .filter(x -> !x.equals("b"))
            .subscribe(received::add);

        assertEquals("[a, c]", received.toString());
    }

    @Test
    void subscribeOther() {
        List<String> received = new ArrayList<>();

        Subscribable<String> first = Subscribable.of("a", "b");
        first.subscribe(received::add);

        Subscribable<String> second = new Subscribable<>();
        second.subscribe(event -> received.add(event + "2"));
        first.subscribe(second);

        assertEquals("[a, b, a2, b2]", received.toString());
    }

    @Test
    void unsubscribe() {
        List<String> received = new ArrayList<>();
        Subscriber<String> a = event -> received.add("a" + event);
        Subscriber<String> b = event -> received.add("b" + event);

        Subscribable<String> subject = new Subscribable<>();
        subject.subscribe(a);
        subject.subscribe(b);
        subject.next("1");
        subject.next("2");
        subject.unsubscribe(a);
        subject.next("3");

        assertEquals("[a1, b1, a2, b2, b3]", received.toString());
    }

    @Test
    void retry() {
        List<Integer> result = new ArrayList<>();
        List<Integer> invocations = new ArrayList<>();

        Subscribable<Integer> subject = new Subscribable<>();
        subject.subscribe(result::add);
        subject.retry(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n >= 2, "Fail!");
            return n;
        }, 3);

        assertEquals(1, result.size());
        assertEquals(2, result.getFirst());
    }

    @Test
    void throwLastExceptionIfRetryFailed() {
        List<Integer> result = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        List<Integer> invocations = new ArrayList<>();

        Subscribable<Integer> subject = new Subscribable<>();
        subject.subscribe(result::add);
        subject.subscribeErrors(errors::add);
        subject.retry(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n >= 5, "Fail!");
            return n;
        }, 3);

        assertEquals(0, result.size());
        assertEquals(1, errors.size());
    }

    @Test
    void retryWithDelay() {
        List<Integer> result = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        List<Integer> invocations = new ArrayList<>();

        Subscribable<Integer> subject = new Subscribable<>();
        subject.subscribe(result::add);
        subject.subscribeErrors(errors::add);
        subject.retry(() -> {
            int n = invocations.size() + 1;
            invocations.add(n);
            Preconditions.checkArgument(n >= 2, "Fail!");
            return n;
        }, 3, 500L);

        assertEquals(1, result.size());
        assertEquals(0, errors.size());
    }

    @Test
    void completedEvent() {
        AtomicInteger counter = new AtomicInteger(0);

        Subscribable<String> subject = new Subscribable<>();
        subject.subscribe(new Subscriber<>() {
            @Override
            public void onEvent(String event) {
            }

            @Override
            public void onComplete() {
                counter.incrementAndGet();
            }
        });
        subject.complete();

        assertEquals(1, counter.get());
    }

    @Test
    void doNotSendCompletedEventTwice() {
        AtomicInteger counter = new AtomicInteger(0);

        Subscribable<String> subject = new Subscribable<>();
        subject.subscribe(new Subscriber<>() {
            @Override
            public void onEvent(String event) {
            }

            @Override
            public void onComplete() {
                counter.incrementAndGet();
            }
        });
        subject.complete();
        subject.complete();

        assertEquals(1, counter.get());
    }

    @Test
    void ofIterable() {
        Subscribable<String> subscribable = Subscribable.of(List.of("a", "b"));
        List<String> buffer = new ArrayList<>();
        subscribable.subscribe(buffer::add);

        assertEquals("[a, b]", buffer.toString());
    }
}
