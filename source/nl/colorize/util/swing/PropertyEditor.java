//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2025 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import nl.colorize.util.Subject;
import nl.colorize.util.TranslationBundle;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple user interface for editing name/value properties. The interface
 * consists of a list of available properties, plus buttons to add, change,
 * or remove properties. The provided callback will be invoked whenever the
 * underlying properties are modified.
 */
public class PropertyEditor extends JPanel {

    private Map<String, String> properties;
    private TranslationBundle bundle;
    private Subject<Map<String, String>> changes;
    private Table<String> table;

    public PropertyEditor(Map<String, String> initialProperties) {
        super(new BorderLayout(0, 10));

        this.properties = new LinkedHashMap<>();
        this.bundle = SwingUtils.getCustomComponentsBundle();
        this.changes = new Subject<>();

        // Initialize the properties. The original map cannot be used
        // because we need to ensure the properties are a LinkedHashMap
        // so that it keeps the iteration order.
        for (String name : getInitialPropertyNames(initialProperties)) {
            this.properties.put(name, initialProperties.get(name));
        }

        table = new Table<>(bundle.getString("PropertyEditor.name"),
            bundle.getString("PropertyEditor.value"));
        add(table, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        refresh();
    }

    private List<String> getInitialPropertyNames(Map<String, String> initialProperties) {
        if (initialProperties instanceof LinkedHashMap) {
            return List.copyOf(initialProperties.keySet());
        } else {
            return initialProperties.keySet().stream()
                .sorted()
                .toList();
        }
    }

    private JPanel createButtonPanel() {
        JButton addButton = new JButton(bundle.getText("PropertyEditor.add"));
        addButton.addActionListener(e -> addProperty());

        JButton editButton = new JButton(bundle.getText("PropertyEditor.edit"));
        editButton.addActionListener(e -> editProperty());

        JButton removeButton = new JButton(bundle.getText("PropertyEditor.remove"));
        removeButton.addActionListener(e -> removeProperty());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(editButton);
        return buttonPanel;
    }

    private void addProperty() {
        showEditForm(bundle.getString("PropertyEditor.addTitle"), "", "");
    }

    private void editProperty() {
        String selected = table.getSelectedRowKey();

        if (selected != null) {
            showEditForm(bundle.getString("PropertyEditor.editTitle"), selected, properties.get(selected));
        } else {
            Popups.message(null, bundle.getString("PropertyEditor.noPropertySelected"));
        }
    }

    private void removeProperty() {
        String selected = table.getSelectedRowKey();

        if (selected != null) {
            properties.remove(selected);
            refresh();
            changes.next(properties);
        } else {
            Popups.message(null, bundle.getString("PropertyEditor.noPropertySelected"));
        }
    }

    private void showEditForm(String title, String name, String value) {
        JTextField nameField = new JTextField(name);
        JTextField valueField = new JTextField(value);

        FormPanel form = new FormPanel();
        form.addRow(bundle.getString("PropertyEditor.addName"), nameField);
        form.addRow(bundle.getString("PropertyEditor.addValue"), valueField);
        form.packFormHeight();

        if (Popups.message(null, title, form,
                bundle.getString("PropertyEditor.save"), bundle.getString("PropertyEditor.cancel")) == 0) {
            properties.put(nameField.getText(), valueField.getText());
            refresh();
            changes.next(properties);
        }
    }

    private void refresh() {
        table.removeAllRows();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            table.addRow(entry.getKey(), entry.getKey(), entry.getValue());
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Subject<Map<String, String>> getChanges() {
        return changes;
    }
}
