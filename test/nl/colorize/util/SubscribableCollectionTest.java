//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SubscribableCollectionTest {

    @Test
    void subscribeAdd() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(new ArrayList<>());
        Queue<String> queue = new LinkedList<>();
        elements.getAddedElements().collect(queue);

        elements.add("a");
        elements.add("b");

        assertEquals("[a, b]", queue.toString());
    }

    @Test
    void subscribeRemove() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(new ArrayList<>());
        Queue<String> queue = new LinkedList<>();
        elements.getRemovedElements().collect(queue);

        elements.add("a");
        elements.remove("a");
        elements.remove("b");

        assertEquals("[a]", queue.toString());
    }

    @Test
    void subscribeClear() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(new ArrayList<>());
        Queue<String> queue = new LinkedList<>();
        elements.getRemovedElements().collect(queue);

        elements.add("a");
        elements.add("b");
        elements.clear();

        assertEquals("[a, b]", queue.toString());
    }

    @Test
    void removeUsingRetain() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(new ArrayList<>());
        Queue<String> queue = new LinkedList<>();
        elements.getRemovedElements().collect(queue);

        elements.add("a");
        elements.add("b");
        elements.retainAll(List.of("b"));

        assertEquals("[a]", queue.toString());
    }

    @Test
    void removeDuringIteration() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(
            new CopyOnWriteArrayList<>());
        elements.add("a");
        elements.add("b");

        Queue<String> queue = new LinkedList<>();
        elements.getRemovedElements().collect(queue);

        for (String element : elements) {
            if (element.equals("a")) {
                elements.remove("b");
            }
        }

        assertEquals("[a]", elements.toString());
        assertEquals("[b]", queue.toString());
    }

    @Test
    void getFirst() {
        SubscribableCollection<String> asList = SubscribableCollection.wrap(new ArrayList<>());
        asList.add("a");
        asList.add("b");

        SubscribableCollection<String> asSet = SubscribableCollection.wrap(new HashSet<>());
        asSet.add("c");

        assertEquals("a", asList.getFirst());
        assertEquals("c", asSet.getFirst());
    }

    @Test
    void flush() {
        SubscribableCollection<String> elements = SubscribableCollection.wrap(new ArrayList<>());
        Queue<String> queue = new LinkedList<>();
        elements.getRemovedElements().collect(queue);

        elements.add("a");
        elements.add("b");
        Stream<String> flushed = elements.flush();

        assertEquals("[]", elements.toString());
        assertEquals(List.of("a", "b"), flushed.toList());
    }
}
