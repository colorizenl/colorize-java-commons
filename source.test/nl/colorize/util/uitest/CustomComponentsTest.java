//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.uitest;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.google.common.base.Suppliers;

import com.google.common.collect.ImmutableMap;
import nl.colorize.util.LogHelper;
import nl.colorize.util.ReflectionUtils;
import nl.colorize.util.swing.AccordionPanel;
import nl.colorize.util.swing.CircularLoader;
import nl.colorize.util.swing.ComboFileDialog;
import nl.colorize.util.swing.FormPanel;
import nl.colorize.util.swing.MultiLabel;
import nl.colorize.util.swing.Popups;
import nl.colorize.util.swing.PropertyEditor;
import nl.colorize.util.swing.Table;
import nl.colorize.util.swing.SwingAnimator;
import nl.colorize.util.swing.SwingUtils;

/**
 * Graphical test for a number of custom Swing components. These components are
 * displayed in different tabs. 
 */
public class CustomComponentsTest {
    
    private JFrame frame;
    private SwingAnimator animator;
    private Table<String> table;
    private List<String> items;
    
    private static final Logger LOGGER = LogHelper.getLogger(CustomComponentsTest.class);

    public static void main(String[] args) {
        CustomComponentsTest test = new CustomComponentsTest();
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
        tabs.addTab("Animation", createAnimationTab());
        tabs.setSelectedIndex(1);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.add(tabs, BorderLayout.CENTER);
        
        frame = new JFrame("Test Custom Components");
        frame.setSize(900, 500);
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
        return tab;
    }

    private JPanel createFormPanelTab() {
        final JPanel customHeightLabel = new JPanel();
        customHeightLabel.setBackground(new Color(255, 200, 200));
        SwingUtils.setPreferredHeight(customHeightLabel, 50);
        
        final FormPanel form = new FormPanel();
        form.addRow("Row with textfield:", new JTextField());
        form.addRow("Row with other component:", new JButton("Button"));
        form.addRow("Custom height:", customHeightLabel);
        form.addRow("Radio buttons:", new JRadioButton("First"), new JRadioButton("Second"));
        form.addRow(new JCheckBox("Row with checkbox"));
        form.addRow(new JCheckBox("Another checkbox with a very long label that doesn't fit"));
        form.addEmptyRow();
        form.addRow(createAddRemoveItemsPanel(), 100);
        return form;
    }

    private JComponent createAddRemoveItemsPanel() {
        items = new ArrayList<String>();
        items.add("First item");
        items.add("Second item");
        items.add("Third item");
        
        return SwingUtils.createAddRemoveItemsPanel(Suppliers.ofInstance(items), "Item", 
                ReflectionUtils.toMethodCallback(this, "onAddRow"),
                ReflectionUtils.toMethodCallback(this, "onRemoveRow", String.class));
    }

    private JPanel createMultiLabelTab() {
        String text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec sit " +
                "amet gravida justo. Nunc nec elit a orci facilisis fermentum. Aliquam placerat " +
                "rutrum ornare. Donec fermentum pellentesque egestas. Morbi posuere consequat " +
                "augue ac convallis. In et arcu ante, sed vestibulum nibh.\n\n" +
                "Phasellus tincidunt arcu at elit fermentum vehicula. Cras a orci est. Aenean " +
                "pulvinar nunc ut felis ultrices posuere. Integer odio purus, tempor imperdiet " +
                "semper in, congue adipiscing turpis. Nulla sed ligula urna, vitae sodales " +
                "justo. Fusce mi nisl, imperdiet in venenatis id, tempor quis turpis. Fusce " +
                "tempor facilisis tincidunt. Aenean libero dolor, varius id feugiat molestie, " +
                "blandit sit amet lectus. Cras rhoncus, purus vel posuere malesuada, sapien " +
                "libero imperdiet quam, quis cursus purus velit non mauris.";
        
        MultiLabel multiLabel = new MultiLabel(text, 300);
        multiLabel.setBackground(Color.WHITE);
        multiLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tab.add(multiLabel);
        return tab;
    }

    private JPanel createSimpleTableTab() {
        table = new Table<String>("Name", "Year of Birth");
        table.setColumnWidth(1, 100);
        table.addRow("D", "Dave", "1984");
        table.addRow("J", "Jim", "1983");
        table.addRow("N", "Nick", "1984");
        table.setRowTooltip("D", "This is a tooltip text for a row which is different from its cells"); 
        table.addActionListener(e -> tableSelectionChanged());
        table.addDoubleClickListener(e -> tableDoubleClicked());
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
            title.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            
            JPanel details = new JPanel(new BorderLayout());
            details.add(new JLabel("Details panel " + (i + 1)), BorderLayout.CENTER);
            details.setBackground(Color.WHITE);
            details.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            SwingUtils.setPreferredHeight(details, 100);
            
            accordion.addSubPanel(i, title, details);
        }
        return accordion;
    }

    private JPanel createPropertyEditorTab() {
        PropertyEditor propertyEditor = new PropertyEditor();
        propertyEditor.setProperties(ImmutableMap.of("a", "1", "b", "2", "c", "3"));
        SwingUtils.setPreferredHeight(propertyEditor, 200);

        JPanel tab = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        tab.add(propertyEditor);
        return tab;
    }
    
    private JPanel createAnimationTab() {
        JPanel target = new JPanel();
        target.setOpaque(true);
        target.setBackground(Color.RED);
        target.setPreferredSize(new Dimension(200, 200));
        
        JButton animateBackgroundButton = new JButton("Animate background color");
        animateBackgroundButton.addActionListener(e -> animator.animateBackgroundColor(target, Color.BLUE, 1f));
        
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
                Arrays.asList("One", "Two"));
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
    
    public void tableSelectionChanged() {
        LOGGER.info("Selected row: " + table.getSelectedRowKey());
    }
    
    public void tableDoubleClicked() {
        LOGGER.info("Double-clicked on row: " + table.getSelectedRowKey());
    }
    
    public void onAddRow() {
        JTextField inputField = new JTextField();
        Popups.message(frame, "Add row", inputField);
        items.add(inputField.getText());
    }
    
    public void onRemoveRow(String selected) {
        items.remove(selected);
    }
}