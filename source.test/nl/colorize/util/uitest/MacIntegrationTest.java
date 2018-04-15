//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.colorize.util.Platform;
import nl.colorize.util.swing.ApplicationMenuListener;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.MacIntegration;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

/**
 * Graphical test for the {@code MacIntegration} class, which provides access to
 * a number of macOS specific features for Swing applications. For obvious reasons
 * this test should only be run when running on macOS.
 */
public class MacIntegrationTest implements ApplicationMenuListener {
    
    private JFrame window;
    private JLabel message;
    private JLabel iconLabel;

    public static void main(String[] args) {
        if (!Platform.isMac()) {
            throw new UnsupportedOperationException("Test must run on macOS");
        }

        MacIntegrationTest test = new MacIntegrationTest();
        test.createWindow();
    }
    
    @SuppressWarnings("deprecation")
    public JFrame createWindow() {
        message = new JLabel("");
        message.setHorizontalAlignment(JLabel.CENTER);
        message.setPreferredSize(new Dimension(500, 30));
        
        iconLabel = new JLabel();
        
        JPanel contentPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(new JLabel("Screen size: " + SwingUtils.getScreenSize().width + "x" +
                SwingUtils.getScreenSize().height));
        contentPanel.add(new JLabel("Screen pixel ratio: " + SwingUtils.getScreenPixelRatio()));
        contentPanel.add(createButton("Change dock icon", e -> setDockIcon()));
        contentPanel.add(createButton("Change dock badge", e -> setDockBadge()));
        contentPanel.add(createButton("Bounce dock icon", e -> bounceDockIcon()));
        contentPanel.add(createButton("Toggle fullscreen", e -> toggleFullscreen()));
        contentPanel.add(createButton("Show notification",  e -> showNotification()));
        contentPanel.add(createButton("Open browser", e -> openURL()));
        contentPanel.add(createButton("Open file",  e -> openFile()));
        contentPanel.add(message);
        contentPanel.add(iconLabel);
        
        window = new JFrame("Test Mac Integration");
        window.setSize(470, 400);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.add(contentPanel, BorderLayout.CENTER);
        MacIntegration.setApplicationMenuListener(this, true);
        MacIntegration.setFullscreenEnabled(window, true);
        window.setVisible(true);
        
        PopupMenu menu = new PopupMenu();
        menu.add("Item 1");
        menu.add("Item 2");
        menu.addSeparator();
        menu.add("Item 3");
        window.add(menu);
        MacIntegration.setDockMenu(menu);
        
        return window;
    }
    
    private JButton createButton(String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        SwingUtils.setPreferredWidth(button, 200);
        return button;
    }
    
    public void onAbout() { 
        message.setText("About"); 
    }
    
    public void onQuit() { 
        message.setText("Quit");
    }
    
    public void onPreferences() {
        message.setText("Preferences");
    }
    
    @SuppressWarnings("deprecation")
    private void setDockIcon() {
        BufferedImage icon = Utils2D.createImage(32, 32, true);
        Graphics2D g2 = Utils2D.createGraphics(icon, true, false);
        g2.setColor(Color.GREEN);
        g2.fillRect(0, 0, 32, 32);
        g2.dispose();
        
        MacIntegration.setDockIcon(icon);
    }
    
    private void setDockBadge() {
        MacIntegration.setDockBadge("1");
    }
    
    private void bounceDockIcon() {
        try { 
            Thread.sleep(3000); 
            MacIntegration.bounceDockIcon(true);
        } catch (Exception e) { 
            throw new AssertionError(e);
        }
    }
    
    private void toggleFullscreen() {
        MacIntegration.toggleFullscreen(window);
    }
    
    private void showNotification() {
        MacIntegration.showNotification("Test", "This is a notification");
    }
    
    @SuppressWarnings("deprecation")
    private void openURL() {
        MacIntegration.openBrowser("http://www.colorize.nl");
    }
    
    @SuppressWarnings("deprecation")
    private void openFile() {
        ComboFileDialog fileDialog = new ComboFileDialog();
        fileDialog.setTitle("Select a file to open");
        File selectedFile = fileDialog.showOpenDialog(null);
        if (selectedFile != null) {
            MacIntegration.openFile(selectedFile);
        }
    }
}
