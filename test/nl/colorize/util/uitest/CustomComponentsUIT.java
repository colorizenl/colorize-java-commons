//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import com.google.common.collect.ImmutableMap;
import nl.colorize.util.LogHelper;
import nl.colorize.util.cli.AnsiColor;
import nl.colorize.util.swing.AccordionPanel;
import nl.colorize.util.swing.CircularLoader;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.MultiLabel;
import nl.colorize.util.swing.Popups;
import nl.colorize.util.swing.PropertyEditor;
import nl.colorize.util.swing.SwingAnimator;
import nl.colorize.util.swing.SwingUtils;
import nl.colorize.util.swing.Table;
import nl.colorize.util.swing.Utils2D;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.awt.Color.BLUE;

/**
 * Graphical test for a number of custom Swing components. These components are
 * displayed in different tabs. 
 */
public class CustomComponentsUIT {
    
    private JFrame frame;
    private SwingAnimator animator;

    private static final Logger LOGGER = LogHelper.getLogger(CustomComponentsUIT.class);

    public static void main(String[] args) {
        System.out.println("---------------------");
        AnsiColor.RED_BOLD.println("Colorize Java Commons");
        System.out.println("---------------------");

        CustomComponentsUIT test = new CustomComponentsUIT();
        test.createWindow();
    }
    
    private void createWindow() {    
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("CircularLoader", createCircularLoaderTab());
        tabs.addTab("FormPanel", createFormPanelTab());
        tabs.addTab("MultiLabel", createMultiLabelTab());
        tabs.addTab("SimpleTable", createSimpleTableTab());
        tabs.addTab("Dialogs", createDialogsTab());
        tabs.addTab("Accordion", createAccordionTab());
        tabs.addTab("PropertyEditor", createPropertyEditorTab());
        tabs.addTab("StripedList", createStripedListTab());
        tabs.addTab("Animation", createAnimationTab());
        tabs.setSelectedIndex(1);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(tabs, BorderLayout.CENTER);
        
        frame = new JFrame("Test Custom Components");
        frame.setSize(1000, 500);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout(0, 20));
        frame.add(contentPanel, BorderLayout.CENTER);
        frame.setVisible(true);
        
        animator = new SwingAnimator();
        animator.start();
    }
    
    private JPanel createCircularLoaderTab() {
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tab.add(new CircularLoader(40));
        tab.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        return tab;
    }

    private JPanel createFormPanelTab() {
        JPanel customHeightLabel = new JPanel();
        customHeightLabel.setBackground(new Color(255, 200, 200));
        SwingUtils.setPreferredHeight(customHeightLabel, 50);

        List<String> labels = new ArrayList<>();
        labels.add("text label");
        
        FormPanel form = new FormPanel();
        form.addRow("Row with textfield:", new JTextField());
        form.addRow("Row with other component:", new JButton("Button"));
        form.addRow("Custom height:", customHeightLabel);
        form.addRow("Radio buttons:", new JRadioButton("First"), new JRadioButton("Second"));
        form.addEmptyRow();
        form.addRow(new JCheckBox("Row with checkbox"));
        form.addRow(new JCheckBox("Another checkbox with a very long label that doesn't fit"));
        form.addEllipsesRow(labels::getFirst, () -> labels.set(0, "something else"));
        return form;
    }

    private JPanel createMultiLabelTab() {
        String text = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sit amet gravida justo. Nunc nec elit a orci facilisis fermentum. Aliquam placerat rutrum ornare. Donec fermentum pellentesque egestas. Morbi posuere consequat augue ac convallis. In et arcu ante, sed vestibulum nibh.

            Phasellus tincidunt arcu at elit fermentum vehicula. Cras a orci est. Aenean pulvinar nunc ut felis ultrices posuere. Integer odio purus, tempor imperdiet semper in, congue adipiscing turpis. Nulla sed ligula urna, vitae sodales justo. Fusce mi nisl, imperdiet in venenatis id, tempor quis turpis. Fusce tempor facilisis tincidunt. Aenean libero dolor, varius id feugiat molestie, blandit sit amet lectus. Cras rhoncus, purus vel posuere malesuada, sapien libero imperdiet quam, quis cursus purus velit non mauris.
            """;

        MultiLabel multiLabel = new MultiLabel(text, 300);
        multiLabel.setBackground(Color.WHITE);
        multiLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tab.add(multiLabel);
        return tab;
    }

    private JPanel createSimpleTableTab() {
        Table<String> table = new Table<>("Name", "Year of Birth", "Historical");
        table.setColumnWidth(1, 100);
        table.setColumnWidth(2, 100);
        table.addRow("D", "Dave", 1984, false);
        table.addRow("J", "Jim", 1983, false);
        table.addRow("N", "Nick", 1984, false);
        table.addRow("B", "Balthasar", 557, true);
        table.setRowTooltip("D", "This is a tooltip text for a row which is different from its cells"); 
        table.onSelect().subscribe(row -> LOGGER.info("Selected row: " + row));
        table.onDoubleClick().subscribe(row -> LOGGER.info("Double-clicked on row: " + row));
        return table;
    }

    private JPanel createDialogsTab() {
        JButton popupButton = new JButton("Show Pop-up");
        popupButton.addActionListener(e -> showPopup());
        
        JButton fileDialogButton = new JButton("Show File Dialog");
        fileDialogButton.addActionListener(e -> showFileDialogs());
        
        JPanel tab = new JPanel(new GridLayout(3, 1, 0, 5));
        tab.add(popupButton);
        tab.add(fileDialogButton);
        return tab;
    }
    
    private JPanel createAccordionTab() {
        AccordionPanel<Integer> accordion = new AccordionPanel<>(true);

        for (int i = 0; i < 4; i++) {
            JLabel title = new JLabel("Title panel " + (i + 1));
            title.setBackground(Utils2D.parseHexColor("#e45d61"));
            title.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
            title.setForeground(Color.WHITE);
            title.setOpaque(true);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            
            JPanel details = new JPanel(new BorderLayout());
            details.add(new MultiLabel("Details panel " + (i + 1), 400), BorderLayout.CENTER);
            details.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            SwingUtils.setPreferredHeight(details, 100);
            
            accordion.addSubPanel(i, title, details);
        }

        accordion.expandSubPanel(0);
        return accordion;
    }

    private JPanel createPropertyEditorTab() {
        PropertyEditor propertyEditor = new PropertyEditor(ImmutableMap.of("a", "1", "b", "2", "c", "3"));
        SwingUtils.setPreferredHeight(propertyEditor, 200);

        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        tab.add(propertyEditor);
        return tab;
    }

    private JPanel createStripedListTab() {
        List<String> elements = List.of("First", "Second", "Third", "Fourth", "Fifth");
        JList<String> list = SwingUtils.createStripedList(elements);
        SwingUtils.setPreferredSize(list, 300, 600);

        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        tab.add(SwingUtils.wrapInScrollPane(list));
        return tab;
    }
    
    private JPanel createAnimationTab() {
        JPanel target = new JPanel();
        target.setOpaque(true);
        target.setBackground(Color.RED);
        target.setPreferredSize(new Dimension(200, 200));
        
        JButton animateBackgroundButton = new JButton("Animate background color");
        animateBackgroundButton.addActionListener(e -> animator.animateBackgroundColor(target, BLUE, 1f));
        
        JButton animateWidthButton = new JButton("Animate width");
        animateWidthButton.addActionListener(e -> animator.animateWidth(target, 300, 1f));
        
        JButton animateHeightButton = new JButton("Animate height");
        animateHeightButton.addActionListener(e -> animator.animateHeight(target, 300, 1f));
        
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        tab.add(animateBackgroundButton);
        tab.add(animateWidthButton);
        tab.add(animateHeightButton);
        tab.add(target);
        return tab;
    }
    
    public void showPopup() {
        int button = Popups.message(frame, "Window title", 
            "This is a message that spans multiple  lines and word wraps across multiple lines.",
            List.of("One", "Two"));
        LOGGER.info("Selected button: " + button);

        Popups.message(frame, "Simple message with default button.");
    }
    
    public void showFileDialogs() {
        ComboFileDialog dialog = new ComboFileDialog();
        dialog.setFilter("XML Files", "xml");
        Popups.message(frame, "Open file dialog");
        LOGGER.info("Selected file: " + dialog.showOpenDialog(frame));
        Popups.message(frame, "Save file dialog");
        LOGGER.info("Selected file: " + dialog.showSaveDialog(frame, "txt"));
    }
}
