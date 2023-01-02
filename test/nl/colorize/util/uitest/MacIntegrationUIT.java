//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import nl.colorize.util.LogHelper;
import nl.colorize.util.Platform;
import nl.colorize.util.swing.ApplicationMenuListener;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.MacIntegration;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

/**
 * Graphical test for the {@code MacIntegration} class, which provides access to
 * a number of macOS specific features for Swing applications. For obvious reasons
 * this test should only be run when running on macOS.
 */
public class MacIntegrationUIT implements ApplicationMenuListener {
    
    private JFrame window;
    private JLabel message;
    private JLabel iconLabel;

    private static final Logger LOGGER = LogHelper.getLogger(MacIntegrationUIT.class);

    public static void main(String[] args) {
        if (!Platform.isMac()) {
            throw new UnsupportedOperationException("Test must run on macOS");
        }

        MacIntegrationUIT test = new MacIntegrationUIT();
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
        contentPanel.add(createButton("Show notification",  e -> showNotification()));
        contentPanel.add(createButton("Open browser", e -> openURL()));
        contentPanel.add(createButton("Open file",  e -> openFile()));
        contentPanel.add(createButton("Fullscreen",  e -> goFullScreen()));
        contentPanel.add(message);
        contentPanel.add(iconLabel);
        
        window = new JFrame("Test Mac Integration");
        window.setSize(470, 400);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.add(contentPanel, BorderLayout.CENTER);
        MacIntegration.setApplicationMenuListener(this);
        window.setVisible(true);

        return window;
    }
    
    private JButton createButton(String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        SwingUtils.setPreferredWidth(button, 200);
        return button;
    }

    @Override
    public void onAbout() { 
        message.setText("About"); 
    }

    @Override
    public void onQuit() {
        LOGGER.info("Application quit requested");
    }

    @Override
    public boolean hasPreferencesMenu() {
        return true;
    }

    @Override
    public void onPreferences() {
        message.setText("Preferences");
    }
    
    private void setDockIcon() {
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
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
            MacIntegration.bounceDockIcon();
        } catch (Exception e) { 
            throw new AssertionError(e);
        }
    }

    private void showNotification() {
        MacIntegration.showNotification("Test", "This is a notification");
    }
    
    private void openURL() {
        MacIntegration.openBrowser("http://www.colorize.nl");
    }
    
    private void openFile() {
        ComboFileDialog fileDialog = new ComboFileDialog();
        fileDialog.setTitle("Select a file to open");
        File selectedFile = fileDialog.showOpenDialog(null);
        if (selectedFile != null) {
            MacIntegration.openFile(selectedFile);
        }
    }

    private void goFullScreen() {
        MacIntegration.goFullScreen(window);
    }
}
