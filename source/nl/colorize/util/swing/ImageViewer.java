//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

/**
 * Custom Swing component for displaying an image in desktop applications, with
 * configurable camera behavior. The user interface and keyboard shortcuts for
 * controlling the camera are optional, when hidden this component will just
 * display the image and nothing else.
 */
public class ImageViewer extends JPanel {

    private BufferedImage displayedImage;
    private float zoom;

    private static final float ZOOM_INCREMENT = 0.2f;
    private static final float SCROLL_ZOOM_FACTOR = 0.01f;
    private static final float MIN_ZOOM_LEVEL = 0.001f;

    public ImageViewer(boolean controls) {
        super(new BorderLayout());
        setOpaque(true);
        addComponentListener(SwingUtils.createResizeListener(this::zoomToFit));

        if (controls) {
            setFocusable(true);
            addKeyListener(SwingUtils.createKeyReleasedListener(this::handleKeyEvent));
            addMouseWheelListener(this::handleScrollEvent);
        }
    }

    public ImageViewer(boolean controls, BufferedImage image) {
        this(controls);
        display(image);
    }

    private void handleKeyEvent(KeyEvent event) {
        if (event.isMetaDown()) {
            if (event.getKeyCode() == KeyEvent.VK_PLUS) {
                changeZoom(zoom + ZOOM_INCREMENT);
            } else if (event.getKeyCode() == KeyEvent.VK_MINUS) {
                changeZoom(zoom - ZOOM_INCREMENT);
            }
        }
    }

    private void handleScrollEvent(MouseWheelEvent event) {
        float scroll = (float) event.getPreciseWheelRotation();
        changeZoom(zoom + scroll * SCROLL_ZOOM_FACTOR);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (displayedImage != null) {
            Graphics2D g2 = Utils2D.createGraphics(g, true, true);
            int width = Math.round(displayedImage.getWidth() * zoom);
            int height = Math.round(displayedImage.getHeight() * zoom);
            g2.drawImage(displayedImage, getWidth() / 2 - width / 2, getHeight() / 2 - height / 2,
                width, height, null);
        }
    }

    public void display(BufferedImage image) {
        displayedImage = image;
        zoomToFit();
        repaint();
    }

    /**
     * Changes the zoom level so that the displayed image fits exactly within
     * the current bounds of this component.
     */
    private void zoomToFit() {
        if (displayedImage == null) {
            zoom = 1f;
        } else {
            float horizontalZoom = (float) getWidth() / (float) displayedImage.getWidth();
            float verticalZoom = (float) getHeight() / (float) displayedImage.getHeight();
            changeZoom(Math.min(horizontalZoom, verticalZoom));
        }
    }

    /**
     * Changes the zoom level to the specified value. 1.0 means the image is
     * displayed at its native size.
     */
    private void changeZoom(float zoom) {
        this.zoom = Math.max(zoom, MIN_ZOOM_LEVEL);
        repaint();
    }
}
