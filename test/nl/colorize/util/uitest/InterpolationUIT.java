//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import nl.colorize.util.animation.Animatable;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.SwingAnimator;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import static nl.colorize.util.animation.Interpolation.DISCRETE;
import static nl.colorize.util.animation.Interpolation.EASE;
import static nl.colorize.util.animation.Interpolation.LINEAR;
import static nl.colorize.util.animation.Interpolation.SMOOTHERSTEP;

/**
 * Depicts different interpolation methods intended for animation. This is done
 * using both a graph and a simple example animation.
 */
public class InterpolationUIT {

    private static final List<Interpolation> VALUES = List.of(DISCRETE, LINEAR, EASE, SMOOTHERSTEP);
    private static final Color BALL_COLOR = new Color(228, 93, 97);
    private static final Color BALL_BORDER = new Color(173, 173, 173);
    private static final int BALL_SIZE = 50;
    private static final double DURATION = 5.0;

    public static void main(String[] args) {
        SwingUtils.initializeSwing();

        InterpolationUIT test = new InterpolationUIT();
        test.createWindow();
    }

    private void createWindow() {
        JFrame window = new JFrame("Test Interpolation");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setLayout(new BorderLayout());
        window.add(createContentPanel(), BorderLayout.CENTER);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setResizable(false);
        window.setVisible(true);
    }

    private JPanel createContentPanel() {
        FormPanel form = new FormPanel();
        SwingAnimator animator = new SwingAnimator();
        List<AnimationPanel> animationPanels = new ArrayList<>();

        JButton playButton = new JButton("Play animations");
        form.add(playButton);
        form.addEmptyRow();

        for (Interpolation value : VALUES) {
            AnimationPanel animationPanel = new AnimationPanel(prepareTimeline(value));
            form.addRow(animationPanel);
            animator.play(animationPanel);
            animationPanels.add(animationPanel);
        }

        playButton.addActionListener(_ -> animationPanels.forEach(ap -> ap.timeline.reset()));

        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        form.packFormHeight();
        animator.start();
        return form;
    }

    private Timeline prepareTimeline(Interpolation value) {
        Timeline timeline = new Timeline(value, false);
        timeline.addKeyFrame(0, 0);
        timeline.addKeyFrame(DURATION, 1.0);
        return timeline;
    }

    /**
     * Displays a simple animation based on the specified timeline, which is
     * in turn based on one of the interpolation methods.
     */
    private static class AnimationPanel extends JPanel implements Animatable {

        private Timeline timeline;

        public AnimationPanel(Timeline timeline) {
            super(new BorderLayout());
            setOpaque(true);
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, BALL_SIZE + 20));
            this.timeline = timeline;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Utils2D.createGraphics(g, true, false);
            int x = (int) Math.round(10 + timeline.getValue() * (getWidth() - BALL_SIZE - 20));

            g2.setColor(BALL_COLOR);
            g2.setColor(BALL_COLOR);
            g2.fillOval(x, 10, BALL_SIZE, BALL_SIZE);
            g2.setColor(BALL_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x, 10, BALL_SIZE, BALL_SIZE);
        }

        @Override
        public void onFrame(double deltaTime) {
            timeline.onFrame(deltaTime);
            repaint();
        }
    }
}
