//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2023 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import com.google.common.collect.ImmutableList;
import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;
import nl.colorize.util.swing.SwingAnimator;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Depicts different interpolation methods intended for animation. This is done
 * using both a graph and a simple example animation.
 */
public class InterpolationUIT extends JPanel implements Animatable {
    
    private Interpolation selected;
    private Timeline exampleAnim;
    
    private JFrame window;
    private Map<Interpolation,Color> lineColorMap;
    
    @SuppressWarnings("rawtypes") 
    private JComboBox selector;
    
    private static final int GRAPH_SIZE = 400;
    private static final int MARGIN = 50;
    private static final Color GRID_COLOR = new Color(50, 50, 50);
    private static final Stroke AXIS_STROKE = new BasicStroke(1.5f);
    private static final Stroke GRID_LINE_STROKE = new BasicStroke(0.5f);
    private static final Stroke VALUE_STROKE = new BasicStroke(2.5f);
    private static final Color EXAMPLE_ANIM_COLOR = new Color(255, 64, 0);
    private static final Stroke EXAMPLE_ANIM_STROKE = new BasicStroke(1.5f);
    private static final float EXAMPLE_ANIM_DURATION = 2f;

    private static final List<Interpolation> INTERPOLATION_METHODS = ImmutableList.of(
        Interpolation.DISCRETE, Interpolation.LINEAR, Interpolation.EASE,
        Interpolation.CUBIC, Interpolation.QUADRATIC, Interpolation.QUINTIC
    );

    public static void main(String[] args) {
        SwingUtils.initializeSwing();

        InterpolationUIT test = new InterpolationUIT();
        test.createWindow();
    }
    
    public InterpolationUIT() {
        super(new BorderLayout());
        
        setPreferredSize(new Dimension(GRAPH_SIZE + 2 * MARGIN, GRAPH_SIZE + MARGIN + 100));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        selected = Interpolation.LINEAR;
        buildLineColorMap();
        recreateExample();
        exampleAnim.end();
        
        SwingAnimator animator = new SwingAnimator();
        animator.play(this);
        animator.start();
    }
    
    private void buildLineColorMap() {
        String[] lineColors = {"#822B35", "#FF4E33", "#B35300", "#FF941A", "#C794DB", "#633091", 
                "#3B54B0", "#3B93B0", "#3BB08B"};

        lineColorMap = new HashMap<>();
        for (int i = 0; i < INTERPOLATION_METHODS.size(); i++) {
            lineColorMap.put(INTERPOLATION_METHODS.get(i), Utils2D.parseHexColor(lineColors[i]));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public JFrame createWindow() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        add(topPanel, BorderLayout.NORTH);
        
        selector = new JComboBox(INTERPOLATION_METHODS.toArray(new Interpolation[0]));
        selector.addActionListener(e -> selectionChanged());
        selector.setSelectedItem(Interpolation.LINEAR);
        topPanel.add(new JLabel("Interpolation method:"));
        topPanel.add(selector);
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);
        add(bottomPanel, BorderLayout.SOUTH);
        
        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> playExample());
        bottomPanel.add(playButton);
        
        window = new JFrame("Test Interpolation");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.add(this, BorderLayout.CENTER);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setVisible(true);
        return window;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2 = Utils2D.createGraphics(g, true, false);
        paintGrid(g2);
        for (Interpolation interpolationMethod : INTERPOLATION_METHODS) {
            if (interpolationMethod != selected) {
                Color color = Utils2D.withAlpha(lineColorMap.get(interpolationMethod), 64);
                paintGraph(g2, color, interpolationMethod);
            }
        }
        paintGraph(g2, lineColorMap.get(selected), selected);
        paintExampleAnimation(g2);
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(AXIS_STROKE);
        g2.drawLine(MARGIN, MARGIN, MARGIN, MARGIN + GRAPH_SIZE);
        g2.drawLine(MARGIN, MARGIN + GRAPH_SIZE, MARGIN + GRAPH_SIZE, MARGIN + GRAPH_SIZE);
        
        g2.setStroke(GRID_LINE_STROKE);
        for (int i = 0; i <= GRAPH_SIZE; i += 50) {
            g2.drawLine(MARGIN, MARGIN + i, MARGIN + GRAPH_SIZE, MARGIN + i);
            g2.drawLine(MARGIN + i, MARGIN, MARGIN + i, MARGIN + GRAPH_SIZE);
        }
    }
    
    private void paintGraph(Graphics2D g2, Color lineColor, Interpolation interpolationMethod) {
        g2.setColor(lineColor);
        g2.setStroke(VALUE_STROKE);
        
        for (int x = 0; x < GRAPH_SIZE; x++) {
            float delta0 = (float) x / (float) GRAPH_SIZE;
            float delta1 = (float) (x + 1) / (float) GRAPH_SIZE;
            float y0 = interpolationMethod.interpolate(GRAPH_SIZE, 0, delta0);
            float y1 = interpolationMethod.interpolate(GRAPH_SIZE, 0, delta1);
            g2.drawLine(MARGIN + x, MARGIN + Math.round(y0), MARGIN + x + 1, MARGIN + Math.round(y1));
        }
    }
    
    private void paintExampleAnimation(Graphics2D g2) {
        int x0 = MARGIN;
        int x1 = getWidth() - MARGIN - 70;
        int currentX = Math.round(selected.interpolate(x0, x1, exampleAnim.getDelta()));
        
        g2.setStroke(EXAMPLE_ANIM_STROKE);
        g2.setColor(EXAMPLE_ANIM_COLOR);
        g2.fillOval(currentX + 50, getHeight() - 70, 50, 50);
        g2.setColor(GRID_COLOR);
        g2.drawOval(currentX + 50, getHeight() - 70, 50, 50);
    }
    
    private void selectionChanged() {
        selected = (Interpolation) selector.getSelectedItem();
        recreateExample();
        repaint();
    }
    
    private void playExample() {
        if (exampleAnim.isAtStart() || exampleAnim.isCompleted()) {
            exampleAnim.reset();
        }
    }
    
    private void recreateExample() {
        exampleAnim = new Timeline(selected, false);
        exampleAnim.addKeyFrame(0f, 0f);
        exampleAnim.addKeyFrame(EXAMPLE_ANIM_DURATION, 1f);
    }
    
    @Override
    public void onFrame(float deltaTime) {
        exampleAnim.movePlayhead(deltaTime);
        repaint();
    }
}
