//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2017 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.base.Charsets;

import nl.colorize.util.http.URLLoader;
import nl.colorize.util.swing.Action;
import nl.colorize.util.swing.ActionDelegate;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

/**
 * Graphical test for all image manipulation functions.
 */
public class TestImageManipulation extends JPanel {

	private BufferedImage photo;
	private int scale;
	private int blur;
	private Color tintColor;
	private int tintIntensity;
	private int dropShadowSize;
	private int dropShadowBlur;
	
	private static final int PADDING = 40;
	
	public static void main(String[] args) throws IOException {
		TestImageManipulation test = new TestImageManipulation();
		test.createWindow();
	}
	
	public TestImageManipulation() {
		super(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	}
	
	public JFrame createWindow() {
		loadPhoto();

		scale = 100;
		blur = 0;
		tintColor = Color.WHITE;
		tintIntensity = 0;
		dropShadowSize = 0;
		dropShadowBlur = 0;
		
		add(createEditPanel(), BorderLayout.SOUTH);
		add(createToggleImagePanel(), BorderLayout.NORTH);
		
		JFrame window = new JFrame("Test Image Manipulation");
		window.setSize(650, 600);
		window.setLocationRelativeTo(null);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(true);
		window.setLayout(new BorderLayout());
		window.add(this, BorderLayout.CENTER);
		window.setVisible(true);
		return window;
	}
	
	private void loadPhoto() {
		// This currently loads a photo from the website, so if
		// the website changes this test will break.
		URLLoader loader = URLLoader.get("http://www.colorize.nl/images/about_studio.png", Charsets.UTF_8);
		try {
			photo = Utils2D.loadImage(loader.sendRequest().openStream());
			photo = Utils2D.makeImageCompatible(photo);
			photo = Utils2D.addPadding(photo, PADDING);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	private void loadImageWithAlpha() {
		BufferedImage image = Utils2D.createImage(220, 220, true);
		Graphics2D g2 = Utils2D.createGraphics(image, true, true);
		g2.setColor(Color.RED);
		g2.fillOval(10, 10, 200, 200);
		g2.setColor(Color.WHITE);
		g2.setFont(g2.getFont().deriveFont(1, 60f));
		Utils2D.drawStringCentered(g2, "Test!", image.getWidth() / 2, 140);
		g2.dispose();
		
		photo = image;
	}
	
	private JPanel createToggleImagePanel() {
		JButton toggleImage = new JButton("Change image");
		toggleImage.addActionListener(new ActionDelegate(this, "toggleImage"));
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		panel.setOpaque(false);
		panel.add(toggleImage);
		return panel;
	}
	
	@Action
	public void toggleImage() {
		if (photo.getWidth() > 300) {
			loadImageWithAlpha();
		} else {
			loadPhoto();
		}
		repaint();
	}
	
	private JPanel createEditPanel() {
		JTabbedPane tabs = new JTabbedPane();
		SwingUtils.setPreferredHeight(tabs, 200);
		tabs.addTab("Scaling", createScalingPanel());
		tabs.addTab("Blur", createBlurPanel());
		tabs.addTab("Tint", createTintPanel());
		tabs.addTab("Drop Shadow", createDropShadowPanel());
		
		JPanel editPanel = new JPanel(new BorderLayout());
		editPanel.add(tabs, BorderLayout.CENTER);
		return editPanel;
	}
	
	private FormPanel createScalingPanel() {
		final JSlider scaleSlider = new JSlider(10, 200, 100);
		scaleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				scale = scaleSlider.getValue();
				repaint();
			}
		});
		
		FormPanel panel = new FormPanel();
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.addRow("Scale:", scaleSlider, false);
		return panel;
	}

	private FormPanel createBlurPanel() {
		final JSlider blurSlider = new JSlider(0, 10, 0);
		blurSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				blur = blurSlider.getValue();
				repaint();
			}
		});
		
		FormPanel panel = new FormPanel();
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.addRow("Gaussian blur:", blurSlider, true);
		return panel;
	}
	
	private FormPanel createTintPanel() {
		final JSlider redSlider = new JSlider(0, 255, 255);
		final JSlider greenSlider = new JSlider(0, 255, 255);
		final JSlider blueSlider = new JSlider(0, 255, 255);
		
		ChangeListener tintListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				tintColor = new Color(redSlider.getValue(), greenSlider.getValue(), 
						blueSlider.getValue());
				repaint();
			}
		};
		redSlider.addChangeListener(tintListener);
		greenSlider.addChangeListener(tintListener);
		blueSlider.addChangeListener(tintListener);
		
		final JSlider intensitySlider = new JSlider(0, 100, 0);
		intensitySlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				tintIntensity = intensitySlider.getValue();
				repaint();
			}
		});
		
		FormPanel panel = new FormPanel();
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.addRow("Red:", redSlider, true);
		panel.addRow("Green:", greenSlider, true);
		panel.addRow("Blue:", blueSlider, true);
		panel.addRow("Intensity:", intensitySlider, true);
		return panel;
	}
	
	private FormPanel createDropShadowPanel() {
		final JSlider shadowSizeSlider = new JSlider(0, 50, 0);
		final JSlider shadowBlurSlider = new JSlider(0, 20, 5);
		
		ChangeListener shadowListener = new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				dropShadowSize = shadowSizeSlider.getValue();
				dropShadowBlur = shadowBlurSlider.getValue();
				repaint();
			}
		};
		shadowSizeSlider.addChangeListener(shadowListener);
		shadowBlurSlider.addChangeListener(shadowListener);
		
		FormPanel panel = new FormPanel();
		panel.setOpaque(false);
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		panel.addRow("Shadow size:", shadowSizeSlider, true);
		panel.addRow("Shadow blur:", shadowBlurSlider, true);
		return panel;
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		int targetWidth = Math.round((scale / 100f) * photo.getWidth());
		int targetHeight = Math.round((scale / 100f) * photo.getHeight());
		int x = getWidth() / 2 - targetWidth / 2;
		int y = 20;
		
		Graphics2D g2 = Utils2D.createGraphics(g, true, true);
		g2.drawImage(getPhotoToDraw(targetWidth, targetHeight), x, y, null);
	}

	private BufferedImage getPhotoToDraw(int targetWidth, int targetHeight) {
		BufferedImage scaledPhoto = Utils2D.scaleImage(photo, targetWidth, targetHeight);
		if (blur > 0) {
			return Utils2D.applyGaussianBlur(scaledPhoto, blur);
		} else if (tintIntensity > 0) {
			return Utils2D.applyTint(scaledPhoto, tintColor);
		} else if (dropShadowSize > 0) {
			return Utils2D.applyDropShadow(scaledPhoto, dropShadowSize, dropShadowBlur);
		} else {
			return scaledPhoto;
		}
	}
}
