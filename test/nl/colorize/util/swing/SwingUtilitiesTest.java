//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2020 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import static org.junit.Assert.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic unit tests for the {@code nl.colorize.util.swing} package.
 */
@RunWith(HeadlessTestRunner.class)
public class SwingUtilitiesTest {
    
    @Test
    public void testCircularLoader() {
        BufferedImage[] frames = CircularLoader.createAnimation(100, Color.BLACK, new BasicStroke(1f));
        assertTrue(frames.length > 1);
        assertEquals(100, frames[0].getWidth());
        assertEquals(100, frames[0].getHeight());
        assertEquals(frames[0].getWidth(), frames[1].getWidth());
        assertEquals(frames[0].getHeight(), frames[1].getHeight());
    }
    
    @Test
    public void testMultiLabel() {
        MultiLabel label = new MultiLabel("Test", 100);
        int height = label.getPreferredSize().height;
        assertTrue(height > 0);
        label.setLabel("A longer text that will cause word wrap", true);
        assertTrue(label.getPreferredSize().height > height);
    }
    
    @Test
    public void testSwingUtils() {
        KeyStroke e = SwingUtils.getKeyStroke(KeyEvent.VK_E, false);
        assertEquals(KeyEvent.VK_E, e.getKeyCode());
        
        JButton button = new JButton("test");
        button.setPreferredSize(new Dimension(100, 100));
        SwingUtils.setPreferredHeight(button, 200);
        assertEquals(new Dimension(100, 200), button.getPreferredSize());
        
        JPanel spacer = SwingUtils.createSpacerPanel(100, 200);
        assertEquals(new Dimension(100, 200), spacer.getPreferredSize());
        
        JMenu menu = new JMenu("Test");
        SwingUtils.createMenuItem(menu, "test", KeyEvent.VK_E);
    }
}
