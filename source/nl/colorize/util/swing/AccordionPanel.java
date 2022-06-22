//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2022 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.LogHelper;
import nl.colorize.util.animation.Interpolation;
import nl.colorize.util.animation.Timeline;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Panel that contains a number of vertically stacked sub-panels, which can
 * be expanded or collapsed when clicking on them. The sub-panels consist of
 * a title panel (which is always displayed), and a detail panel (which is
 * only displayed when the sub-panel is expanded). Whether one or multiple
 * sub-panels can be expanded simultaneously can be configured.
 * <p>
 * This class uses a custom {@code LayoutManager} to achieve the desired
 * effect, changing the layout manager will mean these capabilities are lost.
 *
 * @param <K> The type of key objects that are used to identify sub-panels.
 */
public class AccordionPanel<K> extends JPanel implements LayoutManager {

    private List<SubPanelInfo> subPanels;
    private boolean allowMultipleExpanded;
    private SwingAnimator animator;
    
    private static final float ANIMATION_DURATION = 0.4f;
    private static final Logger LOGGER = LogHelper.getLogger(AccordionPanel.class);
    
    public AccordionPanel(boolean allowMultipleExpanded) {
        super(null);
        setLayout(this);
        
        this.subPanels = new ArrayList<>();
        this.allowMultipleExpanded = allowMultipleExpanded;
        
        animator = new SwingAnimator();
        animator.start();
    }
    
    public void addSubPanel(K key, JComponent titlePanel, JComponent detailsPanel) {
        final SubPanelInfo subPanelInfo = new SubPanelInfo(key, titlePanel, detailsPanel);
        subPanels.add(subPanelInfo);
        
        add(titlePanel);
        add(detailsPanel);
        
        titlePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                expandSubPanel(subPanelInfo);
            }
        });
    }
    
    public void removeSubPanel(K key) {
        SubPanelInfo subPanel = getSubPanel(key);
        if (subPanel != null) {
            remove(subPanel.titlePanel);
            remove(subPanel.detailPanel);
            subPanels.remove(subPanel);
            revalidate();
        }
    }
    
    public void clearSubPanels() {
        for (SubPanelInfo subPanelInfo : subPanels) {
            remove(subPanelInfo.titlePanel);
            remove(subPanelInfo.detailPanel);
        }
        
        subPanels.clear();
        revalidate();
    }
    
    private SubPanelInfo getSubPanel(K key) {
        for (SubPanelInfo subPanel : subPanels) {
            if (subPanel.key.equals(key)) {
                return subPanel;
            }
        }
        return null;
    }
    
    private void expandSubPanel(SubPanelInfo selected) {
        for (SubPanelInfo subPanel : subPanels) {
            boolean isSelected = subPanel.equals(selected);
            if (allowMultipleExpanded) {
                if (isSelected) {
                    subPanel.setExpanded(animator, !subPanel.isExpanded());
                }
            } else {
                subPanel.setExpanded(animator, isSelected && !subPanel.isExpanded());
            }
        }

        revalidate();
    }

    public void expandSubPanel(K key) {
        for (SubPanelInfo subPanel : subPanels) {
            if (subPanel.key.equals(key)) {
                subPanel.setExpanded(animator, true);
            } else {
                if (!allowMultipleExpanded) {
                    subPanel.setExpanded(animator, false);
                }
            }
        }

        revalidate();
    }
    
    @Override
    public void addLayoutComponent(String name, Component comp) {
    }

    @Override
    public void removeLayoutComponent(Component comp) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Insets insets = parent.getInsets();
        int width = insets.left + insets.right;
        int height = insets.top + insets.bottom;
        
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component component = parent.getComponent(i);
            width = Math.max(width, component.getPreferredSize().width);
            height += getComponentHeight(component);
        }
        
        return new Dimension(width, height);
    }
    
    private int getComponentHeight(Component component) {
        for (SubPanelInfo subPanelInfo : subPanels) {
            if (subPanelInfo.titlePanel.equals(component)) {
                return component.getPreferredSize().height;
            } else if (subPanelInfo.detailPanel.equals(component)) {
                return subPanelInfo.currentHeight;
            }
        }
        
        LOGGER.warning("Unknown accordion sub-panel: " + component);
        return component.getPreferredSize().height;
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int y = insets.top;
        int width = parent.getWidth() - insets.left - insets.right;
        
        for (int i = 0; i < parent.getComponentCount(); i++) {
            Component component = parent.getComponent(i);
            int componentHeight = getComponentHeight(component);
            component.setBounds(insets.left, y, width, componentHeight);
            y += componentHeight;
        }
    }
    
    public int getNumSubPanels() {
        return subPanels.size();
    }
    
    /**
     * Groups configuration information on how a sub-panel should be displayed.
     */
    private static class SubPanelInfo {

        private Object key;
        private JComponent titlePanel;
        private JComponent detailPanel;
        private int fullHeight;
        private int currentHeight;
        
        public SubPanelInfo(Object key, JComponent titlePanel, JComponent detailPanel) {
            this.key = key;
            this.titlePanel = titlePanel;
            this.detailPanel = detailPanel;
            this.fullHeight = detailPanel.getPreferredSize().height;
            this.currentHeight = 0;
        }
        
        public void setExpanded(SwingAnimator animator, boolean expanded) {
            Timeline timeline = new Timeline(Interpolation.EASE);
            timeline.addKeyFrame(0f, currentHeight);
            timeline.addKeyFrame(ANIMATION_DURATION, expanded ? fullHeight : 0);

            animator.play(timeline, dt -> {
                currentHeight = Math.round(timeline.getValue());
                detailPanel.revalidate();
            });
        }
        
        public boolean isExpanded() {
            return currentHeight > 0;
        }
    }
}
