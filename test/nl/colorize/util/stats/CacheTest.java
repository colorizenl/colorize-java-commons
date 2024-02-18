//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2024 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.stats;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheTest {

    @Test
    void useLoader() {
        Cache<Integer, String> cache = Cache.from(key -> "" + key);

        assertEquals("1", cache.get(1));
        assertEquals("2", cache.get(2));
        assertEquals("2", cache.get(2));
        assertEquals("2", cache.get(2));
    }

    @Test
    void evictOldEntries() {
        Cache<Integer, String> cache = Cache.from(key -> "" + key, 3);
        cache.get(1);
        cache.get(2);

        assertTrue(cache.isCached(1));
        assertTrue(cache.isCached(2));
        assertFalse(cache.isCached(3));
        assertFalse(cache.isCached(4));

        cache.get(3);
        cache.get(4);

        assertFalse(cache.isCached(1));
        assertTrue(cache.isCached(2));
        assertTrue(cache.isCached(3));
        assertTrue(cache.isCached(4));
    }

    @Test
    void precomputeResults() {
        Map<String, Integer> values = Map.of("a", 2, "b", 3);
        Cache<String, Integer> cache = Cache.from(values::get);
        cache.precompute(List.of("a", "c"));

        assertEquals("Cache [2]\n    a=2\n    c=null", cache.toString());
    }

    @Test
    void invalidateKey() {
        Map<String, Integer> values = Map.of("a", 2, "b", 3);
        Cache<String, Integer> cache = Cache.from(values::get);
        cache.get("a");
        cache.get("b");
        cache.forget("a");

        assertEquals("Cache [1]\n    b=3", cache.toString());
    }

    @Test
    void invalidateAll() {
        Map<String, Integer> values = Map.of("a", 2, "b", 3);
        Cache<String, Integer> cache = Cache.from(values::get);
        cache.get("a");
        cache.get("b");
        cache.forgetAll();

        assertEquals("Cache [0]", cache.toString());
    }
}
