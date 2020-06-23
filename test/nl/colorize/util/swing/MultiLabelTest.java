//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiLabelTest {

    @Test
    public void testMultiLabel() {
        MultiLabel label = new MultiLabel("Test", 100);
        int height = label.getPreferredSize().height;
        assertTrue(height > 0);
        label.setLabel("A longer text that will cause word wrap", true);
        assertTrue(label.getPreferredSize().height > height);
    }
}
