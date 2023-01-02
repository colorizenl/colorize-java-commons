//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Component similar to a {@link javax.swing.JLabel}, but it can display text
 * in multiple lines. Line breaks will be added automatically to keep the
 * text within the component's bounds. Any line break characters already
 * present will be retained.
 */
public class MultiLabel extends JPanel {

    private String label;
    
    private int preferredWidth;
    private int border;
    
    /**
     * Creates a new {@code MultiLabel} with the specified text label.
     * @param label The intial text label.
     * @param width The preferred width of the component.
     * @param border The distance from the edge where no text is painted.
     * @param font The font used for this component.
     */
    public MultiLabel(String label, int width, int border, Font font) {
        super(null);
        
        this.label = label;
        this.preferredWidth = width;
        this.border = border;
        
        if (font != null) {
            setFont(font);
        }
        
        recalculateHeight(width);
    }
    
    /**
     * Creates a new {@code MultiLabel} with the specified text label.
     * @param label The intial text label.
     * @param width The preferred width of the component.
     * @param border The distance from the edge where no text is painted.
     */
    public MultiLabel(String label, int width, int border) {
        this(label, width, border, null);
    }
    
    /**
     * Creates a new {@code MultiLabel} with the specified text label. The height
     * of the component will be auto-detected, and a default border thickness of
     * 5 pixels will be used.
     * @param label The intial text label.
     * @param width The preferred width of the component.
     */
    public MultiLabel(String label, int width) {
        this(label, width, 5);
    }
    
    /**
     * Repaints this label. The text will be layed out according to the width of
     * the component.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = Utils2D.createGraphics(g, true, false);
        g2.setFont(getFont());
        g2.setColor(getForeground());
        
        int width = getWidth();
        int height = Utils2D.drawMultiLineString(g2, label, border, border, width - border * 2);

        // If the width of the component is different from the original width, it
        // means the layout manager has resized it. Re-estimate the preferred height
        // because it might have been changed. Very small changes in width are ignored.
        if (Math.abs(preferredWidth - width) > 10) {
            preferredWidth = width;
            int preferredHeight = height + 2 * border;
            setPreferredSize(new Dimension(width, preferredHeight));
            if (getParent() instanceof JComponent) {
                getParent().revalidate();
            }
        }
    }
    
    private void requestPreferredSize(int width, int height, boolean force) {
        if (height == -1) {
            height = estimateHeight(width);
        }
        
        if (force) {
            setPreferredSize(new Dimension(width, height));
        }
    }
    
    /**
     * Returns an estimate of the height of the component, bases on wrapping
     * around the specified width. This method exists because the preferred
     * size of the component must be set beforehand.
     */
    private int estimateHeight(int width) {
        BufferedImage scratchImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scratchImage.createGraphics();
        g2.setFont(getFont());
        int textHeight = Utils2D.drawMultiLineString(g2, label, border, border, width - border * 2);
        g2.dispose();
        return textHeight + 2 * border;
    }
    
    public void recalculateHeight(int width) {
        requestPreferredSize(width, -1, true);
    }
    
    public void setLabel(String label, boolean forceRescale) {
        this.label = label;
        requestPreferredSize(getPreferredSize().width, -1, forceRescale);
        repaint();
    }
    
    public void setLabel(String label) {
        setLabel(label, false);
    }
    
    public String getLabel() {
        return label;
    }
}
