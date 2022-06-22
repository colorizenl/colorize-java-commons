//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
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
import java.awt.image.BufferedImage;

/**
 * A circular progressbar commonly used in web based AJAX applications. The look
 * of this component has become popular, and although it is available by default
 * on macOS this class provides a generic implementation. The component can be
 * rescaled to any size using {@link #setPreferredSize(Dimension)}.
 */
public class CircularLoader extends JPanel implements Animatable {

    private BufferedImage[] frames;
    private int active;
    private int size;
    private Color lineColor;
    private Stroke lineStroke;
    
    private static final int NUM_LINES = 12;
    
    /**
     * Creates a new {@code CircularLoader} of the specified size. If a size of 
     * 0 is specified the size at which the component will be displayed is 
     * determined by the layout manager that owns it.
     * @param size The component size in pixels.
     */
    public CircularLoader(int size) {
        super(null);
        
        setOpaque(false);
        if (size > 0) {
            setPreferredSize(new Dimension(size, size));
        }
        
        this.size = size;
        this.lineColor = Color.BLACK;
        this.lineStroke = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
        
        SwingAnimator animator = new SwingAnimator();
        animator.play(this);
        animator.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        // If the component is disabled or not visible there is no point to
        // paint anything.
        if (!isEnabled() || (getWidth() == 0) || (getHeight() == 0)) {
            return;
        }

        if (frames == null) {
            frames = createAnimation(size, lineColor, lineStroke);
        }
                
        Graphics2D g2 = Utils2D.createGraphics(g, true, false);
        g2.drawImage(frames[active], getWidth() / 2 - frames[active].getWidth() / 2, 
                getHeight() / 2 - frames[active].getHeight() / 2, 
                frames[active].getWidth(), frames[active].getHeight(), null);
    }
    
    @Override
    public void onFrame(float deltaTime) {
        active++;
        if (active >= NUM_LINES) {
            active = 0;
        }
        repaint();
    }
    
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }
    
    public Color getLineColor() {
        return lineColor;
    }

    public void setLineStroke(Stroke lineStroke) {
        this.lineStroke = lineStroke;
    }
    
    public void setLineStroke(float strokeWidth) {
        this.lineStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
    }
    
    public Stroke getLineStroke() {
        return lineStroke;
    }

    /**
     * Returns an array of frame images. When the animation starts all frames are
     * painted in advance, so that the rest of the animation just requires painting
     * the images instead of having to repaint the animation itself.
     * <p>
     * This method is static so that non-Swing applications can use the graphics
     * used by this component.
     * @param size The width/height of the created images.
     * @return An array of all frames, its length depends on the number of frames.
     */
    public static BufferedImage[] createAnimation(int size, Color lineColor, Stroke lineStroke) {
        BufferedImage[] frames = new BufferedImage[NUM_LINES];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = createFrameImage(i, size, lineColor, lineStroke);
        }
        return frames;
    }

    private static BufferedImage createFrameImage(int frame, int size, Color lineColor, 
            Stroke lineStroke) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Utils2D.createGraphics(image, true, false);
        
        int offsetX = size / 2;
        int offsetY = size / 2;
        int outerRadius = size / 2 - 2;
        int innerRadius = outerRadius / 2 + 1;
                
        for (int i = 0; i < NUM_LINES; i++) {
            float angle = (float) Math.toRadians(i * (360.0 / NUM_LINES));
            int startX = (int) (Math.cos(angle) * outerRadius);
            int startY = (int) (Math.sin(angle) * outerRadius);
            int endX = (int) (Math.cos(angle) * innerRadius);
            int endY = (int) (Math.sin(angle) * innerRadius);            
        
            g2.setColor(getLineColor(i, frame, lineColor));
            g2.setStroke(lineStroke);
            g2.drawLine(offsetX + startX, offsetY + startY, offsetX + endX, offsetY + endY);
        }
        
        g2.dispose();
            
        return image;
    }

    private static Color getLineColor(int n, int active, Color color) {
        int offset = (n - NUM_LINES) - active;
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
}
