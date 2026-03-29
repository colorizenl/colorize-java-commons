//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.animation.Animatable;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_MITER;

/**
 * A circular progress bar as commonly seen in web applications. This class
 * provides a cross-platform Swing implementation of this component. The
 * progress bar can be scaled beyond its default size if needed, by using
 * {@link #setPreferredSize(Dimension)}.
 */
public class CircularLoader extends JPanel implements Animatable {

    private int size;
    private Color lineColor;
    private Stroke lineStroke;

    private int currentFrame;

    private static final int FRAMERATE = 15;
    private static final int NUM_LINES = 12;
    private static final int NUM_FRAMES = NUM_LINES;
    private static final int DEFAULT_SIZE = 50;
    private static final Color DEFAULT_COLOR = Color.BLACK;
    private static final Stroke DEFAULT_STROKE = new BasicStroke(2.5f, CAP_ROUND, JOIN_MITER);

    /**
     * Creates a new {@link CircularLoader} with all properties set to default
     * values. This is the recommended constructor for applications that just
     * need to display a progress bar, without needing to customize its
     * look-and-feel.
     */
    public CircularLoader() {
        this(DEFAULT_SIZE, DEFAULT_COLOR, DEFAULT_STROKE);
    }

    /**
     * Creates a new {@code CircularLoader} with the specified size. Using
     * size zero means the component's size is determined by the layout
     * manager.
     */
    public CircularLoader(int size) {
        this(size, DEFAULT_COLOR, DEFAULT_STROKE);
    }

    /**
     * Creates a new {@code CircularLoader} with the specified properties.
     * Using size zero means the component's size is determined by the
     * layout manager.
     */
    public CircularLoader(int size, Color lineColor, Stroke lineStroke) {
        super(null);
        setOpaque(false);
        if (size > 0) {
            setPreferredSize(new Dimension(size, size));
        }

        this.size = size == 0 ? DEFAULT_SIZE : size;
        this.lineColor = lineColor;
        this.lineStroke = lineStroke;

        SwingAnimator animator = new SwingAnimator(FRAMERATE);
        animator.play(this);
        animator.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // If the component is disabled or not visible there
        // is no point to draw anything.
        if (!isEnabled() || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        Graphics2D g2 = Utils2D.createGraphics(g, true, false);
        drawFrame(g2);
    }

    private void drawFrame(Graphics2D g2) {
        int offsetX = getWidth() / 2 ;
        int offsetY = getHeight() / 2;
        int outerRadius = size / 2 - 2;
        int innerRadius = outerRadius / 2 + 1;

        for (int i = 0; i < NUM_LINES; i++) {
            float angle = (float) Math.toRadians(i * (360.0 / NUM_LINES));
            int startX = (int) (Math.cos(angle) * outerRadius);
            int startY = (int) (Math.sin(angle) * outerRadius);
            int endX = (int) (Math.cos(angle) * innerRadius);
            int endY = (int) (Math.sin(angle) * innerRadius);

            g2.setColor(getLineColor(i, currentFrame, lineColor));
            g2.setStroke(lineStroke);
            g2.drawLine(offsetX + startX, offsetY + startY, offsetX + endX, offsetY + endY);
        }
    }

    private Color getLineColor(int n, int active, Color color) {
        int offset = (n - NUM_FRAMES) - active;
        if (n - active < 0) {
            offset = n - active;
        }

        float a = 1f;
        if (offset < 0) {
            a += offset * 0.1f;
        }
        a = Math.max(a, 0.1f);

        return new Color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, a);
    }
    
    @Override
    public void onFrame(float deltaTime) {
        currentFrame++;
        if (currentFrame >= NUM_FRAMES) {
            currentFrame = 0;
        }
        repaint();
    }
}
