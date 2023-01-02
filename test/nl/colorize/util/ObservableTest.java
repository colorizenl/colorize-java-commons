//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservableTest {

    @Test
    void subscribeToEvents() {
        Observable<String> observable = new Observable<>();
        List<String> events = new ArrayList<>();
        observable.subscribe(events::add);

        observable.emit("1");
        observable.emit("2");

        assertEquals(List.of("1", "2"), events);
    }

    @Test
    void addMoreSubscribersDuringCallback() {
        Observable<String> observable = new Observable<>();
        List<String> events = new ArrayList<>();

        observable.subscribe(event -> {
            events.add("1." + event);
            observable.subscribe(event2 -> events.add("2." + event2));
        });

        observable.emit("1");
        observable.emit("2");

        assertEquals(List.of("1.1", "1.2", "2.2"), events);
    }

    @Test
    void unsubscribeDuringCallback() {
        Observable<String> observable = new Observable<>();
        List<String> events = new ArrayList<>();
        Consumer<String> subscriber = event -> events.add(event);
        observable.subscribe(subscriber);

        observable.emit("1");
        observable.unsubscribe(subscriber);
        observable.emit("2");

        assertEquals(List.of("1"), events);
    }
}
