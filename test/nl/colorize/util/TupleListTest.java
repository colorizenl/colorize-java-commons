//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util;

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
}
