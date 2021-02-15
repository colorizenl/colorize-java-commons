//-----------------------------------------------------------------------------
// Remember That
// Copyright 2015-2021 Colorize
//-----------------------------------------------------------------------------

package nl.colorize.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceWrapperTest {

    @Test
    void openResourceMultipleTimes() {
        List<String> result = new ArrayList<>();
        AutoCloseable resource = () -> result.add("");
        ResourceWrapper<AutoCloseable> wrapper = ResourceWrapper.wrap(() -> resource);
        wrapper.open();
        wrapper.close();
        wrapper.open();
        wrapper.close();
        wrapper.close();

        assertEquals(2, result.size());
    }

    @Test
    void cannotOpenAlreadyOpenResource() {
        List<String> result = new ArrayList<>();
        AutoCloseable resource = () -> result.add("");
        ResourceWrapper<AutoCloseable> wrapper = ResourceWrapper.wrap(() -> resource);
        wrapper.open();

        assertThrows(IllegalStateException.class, wrapper::open);
    }
}