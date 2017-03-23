//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Basic unit tests for the {@code nl.colorize.util.swing} package.
 */
@RunWith(HeadlessTestRunner.class)
public class TestSwingUtilities {
	
	private List<Object> events;
	
	@Before
	public void before() {
		events = new ArrayList<Object>();
	}
	
	@Action
	public void firstActionMethod() {
		events.add("first");
	}
	
	@Action
	public void secondActionMethod(Object source) {
		events.add(source);
	}
	
	@Action
	private void thirdActionMethod() {
		events.add("third");
	}
	
	@Action
	public void fourthActionMethod(Object arg) {
		int parsedArg = (Integer) arg;
		events.add(parsedArg);
	}

	@Test
	public void testActionDelegate() {
		JButton button = new JButton("Test");
		button.addActionListener(new ActionDelegate(this, "firstActionMethod"));
		button.doClick();
		assertEquals(1, events.size());
		assertEquals("first", events.get(0));
	}
	
	@Test
	public void testActionDelegateWithArg() {
		JButton button = new JButton("Test");
		button.addActionListener(new ActionDelegate(this, "secondActionMethod"));
		button.doClick();
		assertEquals(1, events.size());
		assertEquals(button, events.get(0));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testPrivateActionDelegate() {
		JButton button = new JButton("Test");
		button.addActionListener(new ActionDelegate(this, "thirdActionMethod"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testNonExistingActionDelegate() {
		JButton button = new JButton("Test");
		button.addActionListener(new ActionDelegate(this, "nonExistingActionMethod"));
	}
	
	@Test
	public void testActionDelegateWithProvidedArg() {
		JButton button = new JButton("Test");
		button.addActionListener(new ActionDelegate(this, "fourthActionMethod", 123));
		button.doClick();
		assertEquals(1, events.size());
		assertEquals(123, events.get(0));
	}
	
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
