//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import com.google.common.base.Preconditions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubjectTest {

    @Test
    void subscribe() {
        List<String> received = new ArrayList<>();
        Subject<String> subject = new Subject<>();
        subject.subscribe(received::add);
        subject.next("a");
        subject.next("b");

        assertEquals(List.of("a", "b"), received);
    }

    @Test
    void dontReceiveAfterDispose() {
        List<String> received = new ArrayList<>();
        Subject<String> subject = new Subject<>();

        subject.subscribe(received::add);
        subject.next("a");
        subject.complete();
        subject.next("b");

        assertEquals(List.of("a"), received);
    }

    @Test
    void disposeDuringProcessing() {
        List<String> received = new ArrayList<>();
        Subject<String> subject = new Subject<>();

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
        List<Throwable> errors = new ArrayList<>();
        Subject<String> subject = new Subject<>();

        subject.subscribe(received::add, errors::add);
        subject.next(() -> "a");

        assertEquals(1, received.size());
        assertEquals(0, errors.size());
    }

    @Test
    void notifyError() {
        List<String> received = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();
        Subject<String> subject = new Subject<>();

        subject.subscribe(received::add, errors::add);
        subject.next(() -> "Whoops! " + (1 / 0));

        assertEquals(0, received.size());
        assertEquals(1, errors.size());
    }

    @Test
    void receivePreviousEventsUponSubscription() {
        List<String> received = new ArrayList<>();
        Subject<String> subject = new Subject<>();
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
        Subject<String> subject = Subject.of("1", "2", "3");
        subject.subscribe(received::add);

        assertEquals("[1, 2, 3]", received.toString());
    }

    @Test
    void fromOperation() {
        List<String> received = new CopyOnWriteArrayList<>();
        Subject<String> subject = Subject.run(() -> "1");
        subject.subscribe(received::add);

        assertEquals("[1]", received.toString());
    }

    @Test
    void inBackgroundThread() throws InterruptedException {
        List<String> received = new CopyOnWriteArrayList<>();
        Subject<String> subject = Subject.runBackground(() -> {
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

        Subject<String> originalSubject = Subject.of("a", "b");
        originalSubject.subscribe(received::add);
        Subject<String> mappedSubject = originalSubject.map(x -> x + "2");
        mappedSubject.subscribe(received::add);

        assertEquals("[a, b, a2, b2]", received.toString());
    }

    @Test
    void mapException() {
        List<Integer> received = new ArrayList<>();
        List<Throwable> originalErrors = new ArrayList<>();
        List<Throwable> mappedErrors = new ArrayList<>();

        Subject<Integer> originalSubject = Subject.of(1, 2);
        originalSubject.subscribeErrors(originalErrors::add);
        Subject<Integer> mappedSubject = originalSubject.map(x -> x / 0);
        mappedSubject.subscribe(received::add, mappedErrors::add);

        assertEquals(0, received.size());
        assertEquals(0, originalErrors.size());
        assertEquals(2, mappedErrors.size());
    }

    @Test
    void filter() {
        List<String> received = new ArrayList<>();

        Subject.of("a", "b", "c")
            .filter(x -> !x.equals("b"))
            .subscribe(received::add);

        assertEquals("[a, c]", received.toString());
    }

    @Test
    void subscribeOther() {
        List<String> received = new ArrayList<>();

        Subject<String> first = Subject.of("a", "b");
        first.subscribe(received::add);

        Subject<String> second = new Subject<>();
        second.subscribe(event -> received.add(event + "2"));
        first.subscribe(second);

        assertEquals("[a, b, a2, b2]", received.toString());
    }

    @Test
    void unsubscribe() {
        List<String> received = new ArrayList<>();
        Subscriber<String> a = Subject.createSubscriber(event -> received.add("a" + event));
        Subscriber<String> b = Subject.createSubscriber(event -> received.add("b" + event));

        Subject<String> subject = new Subject<>();
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

        Subject<Integer> subject = new Subject<>();
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
        List<Throwable> errors = new ArrayList<>();
        List<Integer> invocations = new ArrayList<>();

        Subject<Integer> subject = new Subject<>();
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
        List<Throwable> errors = new ArrayList<>();
        List<Integer> invocations = new ArrayList<>();

        Subject<Integer> subject = new Subject<>();
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

        Subject<String> subject = new Subject<>();
        subject.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
            }

            @Override
            public void onNext(String item) {
            }

            @Override
            public void onError(Throwable throwable) {
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

        Subject<String> subject = new Subject<>();
        subject.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
            }

            @Override
            public void onNext(String item) {
            }

            @Override
            public void onError(Throwable throwable) {
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
        Subject<String> subject = Subject.of(List.of("a", "b"));
        List<String> buffer = new ArrayList<>();
        subject.subscribe(buffer::add);

        assertEquals("[a, b]", buffer.toString());
    }

    @Test
    void collectInMessageQueue() {
        Subject<String> subject = new Subject<>();
        subject.next("a");
        subject.next("b");

        Queue<String> queue = new LinkedList<>();
        subject.collect(queue);

        assertEquals("[a, b]", queue.toString());

        subject.next("c");

        assertEquals("[a, b, c]", queue.toString());
    }

    @Test
    void unsubscribeFromSubscription() {
        List<String> received = new ArrayList<>();

        Subject<String> subject = new Subject<>();
        subject.next("a");
        subject.next("b");
        subject.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.cancel();
            }

            @Override
            public void onNext(String item) {
                received.add(item);
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
            }
        });
        subject.next("c");

        assertEquals("[a, b]", received.toString());
    }

    @Test
    void unsubscribeUsingSubscription() {
        List<String> received = new ArrayList<>();

        Subject<String> subject = new Subject<>();
        subject.next("a");
        Subscription subscription = subject.subscribe(received::add);
        subject.next("b");
        subscription.cancel();
        subject.next("c");

        assertEquals("[a, b]", received.toString());
    }
}
