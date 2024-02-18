//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TupleListTest {

    @Test
    void getLeftAndRight() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        list.add("b", 3);

        assertEquals(ImmutableList.of("a", "b"), list.getLeft());
        assertEquals(ImmutableList.of(2, 3), list.getRight());
    }

    @Test
    void inverse() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        list.add("b", 3);

        assertEquals("[(2, a), (3, b)]", list.inverse().toString());
    }

    @Test
    void empty() {
        TupleList<String, Integer> list = TupleList.empty();
        assertThrows(UnsupportedOperationException.class, () -> list.add("b", 2));
    }

    @Test
    void immutable() {
        TupleList<String, Integer> list = new TupleList<>();
        list.add("a", 2);
        TupleList<String, Integer> immutableVersion = list.immutable();
        list.add("b", 3);

        assertEquals("[(a, 2), (b, 3)]", list.toString());
        assertEquals("[(a, 2)]", immutableVersion.toString());
        assertThrows(UnsupportedOperationException.class, () -> immutableVersion.add("c", 4));
    }

    @Test
    void fromStream() {
        Tuple<String, Integer> a = Tuple.of("a", 2);
        Tuple<String, Integer> b = Tuple.of("b", 3);
        Stream<Tuple<String, Integer>> stream = ImmutableList.of(a, b).stream();

        assertEquals("[(a, 2), (b, 3)]", TupleList.fromStream(stream).toString());
    }

    @Test
    void fromMap() {
        Map<String, Integer> map = ImmutableMap.of("a", 2, "b", 3);

        assertEquals("[(a, 2), (b, 3)]", TupleList.fromMap(map).toString());
    }

    @Test
    void fromValues() {
        TupleList<String, Integer> tuples = TupleList.of(Tuple.of("a", 2), Tuple.of("b", 3));

        assertEquals(2, tuples.size());
        assertEquals("[(a, 2), (b, 3)]", tuples.toString());
    }

    @Test
    void forEach() {
        TupleList<String, Integer> tuples = TupleList.of(Tuple.of("a", 2), Tuple.of("b", 3));
        List<Object> counter = new ArrayList<>();
        tuples.forEach((left, right) -> counter.add(left + "/" + right));

        assertEquals("[a/2, b/3]", counter.toString());
    }

    @Test
    void combine() {
        TupleList<String, String> tuples = TupleList.combine(List.of("a", "b"), List.of("2", "3"));

        assertEquals("[(a, 2), (b, 3)]", tuples.toString());
    }

    @Test
    void requireCombinedListsToHaveSameLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            TupleList.combine(List.of("a", "b"), List.of("2"));
        });
    }

    @Test
    void map() {
        TupleList<String, String> original = TupleList.of(Tuple.of("a", "b"), Tuple.of("c", "d"));
        TupleList<String, String> mapped = original.map(x -> x + "2", y -> y + "3");

        assertEquals("[(a2, b3), (c2, d3)]", mapped.toString());
    }

    @Test
    void concat() {
        TupleList<String, Integer> first = TupleList.of(Tuple.of("a", 2), Tuple.of("b", 3));
        TupleList<String, Integer> second = TupleList.of(Tuple.of("d", 4));
        TupleList<String, Integer> result = first.concat(second);

        assertEquals("[(a, 2), (b, 3)]", first.toString());
        assertEquals("[(d, 4)]", second.toString());
        assertEquals("[(a, 2), (b, 3), (d, 4)]", result.toString());
    }

    @Test
    void append() {
        TupleList<String, Integer> list = TupleList.of();
        list.append("a", 2);

        assertEquals("[(a, 2)]", list.toString());
    }

    @Test
    void fromExistingList() {
        List<Tuple<String, Integer>> existing = new ArrayList<>();
        existing.add(Tuple.of("a", 2));
        existing.add(Tuple.of("b", 3));

        TupleList<String, Integer> result = TupleList.of(existing);
        result.add("c", 4);

        assertEquals("[(a, 2), (b, 3), (c, 4)]", result.toString());
    }
}
