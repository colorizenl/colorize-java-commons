//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.Tuple;
import org.jspecify.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;

import static java.awt.event.KeyEvent.VK_0;
import static java.awt.event.KeyEvent.VK_EQUALS;
import static java.awt.event.KeyEvent.VK_MINUS;
import static java.awt.event.KeyEvent.VK_PLUS;

/**
 * Custom Swing component for displaying an image in desktop applications,
 * with (optional) pan and zoom controls to influence how the image is
 * displayed.
 * <p>
 * When enabled, the image viewer supports the following controls:
 * <ul>
 *   <li>Dragging the mouse or touchpad will pan the camera.</li>
 *   <li>Using the scroll wheel will zoom the camera.</li>
 *   <li>Control/Command Plus will zoom in.</li>
 *   <li>Control/Command Minus will zoom out.</li>
 *   <li>Control/Command Zero will reset both pan and zoom.</li>
 * </ul>
 */
public class ImageViewer extends JPanel {

    private BufferedImage displayedImage;
    private int cameraX;
    private int cameraY;
    private float cameraZoom;

    private static final float ZOOM_KEY_INCREMENT = 0.25f;
    private static final float ZOOM_SCROLL_INCREMENT = 0.02f;
    private static final float PAN_INCREMENT = 1f;
    private static final float MIN_ZOOM_LEVEL = 0.001f;

    /**
     * Creates a new {@link ImageViewer} component with the specified controls.
     * The component will not initially display an image.
     */
    public ImageViewer(boolean allowPan, boolean allowZoom) {
        super(new BorderLayout());
        setOpaque(true);
        addComponentListener(SwingUtils.createResizeListener(this::zoomToFit));

        if (allowPan) {
            setFocusable(true);
            SwingUtils.trackDrag(this).subscribe(this::handleDragEvent);
        }

        if (allowZoom) {
            setFocusable(true);
            addKeyListener(SwingUtils.createKeyReleasedListener(this::handleKeyEvent));
            addMouseWheelListener(this::handleScrollEvent);
        }
    }

    /**
     * Creates a new {@link ImageViewer} component that allows full pan and
     * zoom controls. This is both the default and the recommended behavior.
     */
    public ImageViewer() {
        this(true, true);
    }

    private void handleKeyEvent(KeyEvent event) {
        if (event.isMetaDown() || event.isControlDown()) {
            switch (event.getKeyCode()) {
                case VK_PLUS, VK_EQUALS -> changeZoom(cameraZoom + ZOOM_KEY_INCREMENT);
                case VK_MINUS -> changeZoom(cameraZoom - ZOOM_KEY_INCREMENT);
                case VK_0 ->  zoomToFit();
                default -> {}
            }
        }
    }

    private void handleScrollEvent(MouseWheelEvent event) {
        float scroll = (float) event.getPreciseWheelRotation();
        changeZoom(cameraZoom + scroll * ZOOM_SCROLL_INCREMENT);
    }

    private void handleDragEvent(Tuple<Point, Point> event) {
        int deltaX = event.left().x - event.right().x;
        int deltaY = event.left().y - event.right().y;
        changePan(cameraX + deltaX * PAN_INCREMENT, cameraY + deltaY * PAN_INCREMENT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (displayedImage != null) {
            Graphics2D g2 = Utils2D.createGraphics(g, true, true);
            int width = Math.round(displayedImage.getWidth() * cameraZoom);
            int height = Math.round(displayedImage.getHeight() * cameraZoom);
            int x = getWidth() / 2 - width / 2 - cameraX;
            int y = getHeight() / 2 - height / 2 - cameraY;
            g2.drawImage(displayedImage, x, y, width, height, null);
        }
    }

    /**
     * Lets this component display the specified image. This will reset the
     * current pan and zoom controls to make sure the image is visible.
     * Supplying a value of {@code null} will make this component display no
     * graphics.
     */
    public void display(@Nullable BufferedImage image) {
        displayedImage = image;
        zoomToFit();
        repaint();
    }

    /**
     * Changes the zoom level so that the displayed image fits exactly within
     * the current bounds of this component.
     */
    private void zoomToFit() {
        cameraX = 0;
        cameraY = 0;

        if (displayedImage == null) {
            changeZoom(1f);
        } else {
            float horizontalZoom = (float) getWidth() / (float) displayedImage.getWidth();
            float verticalZoom = (float) getHeight() / (float) displayedImage.getHeight();
            changeZoom(Math.min(horizontalZoom, verticalZoom));
        }
    }

    /**
     * Changes the camera's position to the specified values. A position of
     * (0, 0) means the image will be positioned in the exact center of the
     * component.
     */
    private void changePan(float x, float y) {
        cameraX = Math.round(x);
        cameraY = Math.round(y);
        repaint();
    }

    /**
     * Changes the zoom level to the specified value. 1.0 means the image is
     * displayed at its native size.
     */
    private void changeZoom(float zoom) {
        cameraZoom = Math.max(zoom, MIN_ZOOM_LEVEL);
        repaint();
    }
}
