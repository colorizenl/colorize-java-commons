//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2018 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.base.Charsets;
import nl.colorize.util.DynamicResourceBundle;
import nl.colorize.util.ResourceFile;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple user interface for editing name/value properties. The interface
 * consists of a list of available properties, plus buttons to add, change,
 * or remove properties.
 */
public class PropertyEditor extends JPanel {

    private Map<String, String> properties;
    private DynamicResourceBundle bundle;
    private List<Runnable> listeners;

    private Table<String> table;

    public PropertyEditor() {
        super(new BorderLayout(0, 10));

        properties = new LinkedHashMap<>();
        bundle = SwingUtils.getCustomComponentsBundle();
        listeners = new ArrayList<>();

        table = new Table<>(bundle.getString("PropertyEditor.name"),
            bundle.getString("PropertyEditor.value"));
        add(table, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JButton addButton = new JButton(bundle.getString("PropertyEditor.add"));
        addButton.addActionListener(e -> addProperty());

        JButton editButton = new JButton(bundle.getString("PropertyEditor.edit"));
        editButton.addActionListener(e -> editProperty());

        JButton removeButton = new JButton(bundle.getString("PropertyEditor.remove"));
        removeButton.addActionListener(e -> removeProperty());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(removeButton);
        return buttonPanel;
    }

    private void addProperty() {
        showForm(bundle.getString("PropertyEditor.addTitle"), "", "");
    }

    private void editProperty() {
        String selected = table.getSelectedRowKey();

        if (selected != null) {
            showForm(bundle.getString("PropertyEditor.editTitle"), selected, properties.get(selected));
        } else {
            Popups.message(null, bundle.getString("PropertyEditor.noPropertySelected"));
        }
    }

    private void removeProperty() {
        String selected = table.getSelectedRowKey();

        if (selected != null) {
            properties.remove(selected);
            refresh();
        } else {
            Popups.message(null, bundle.getString("PropertyEditor.noPropertySelected"));
        }
    }

    private void showForm(String title, String name, String value) {
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
        }
    }

    public void setProperties(Map<String, String> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
        refresh();
    }

    private void refresh() {
        table.removeAllRows();

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            table.addRow(entry.getKey(), entry.getKey(), entry.getValue());
        }

        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void addListener(Runnable callback) {
        listeners.add(callback);
    }

    public void removeListener(Runnable callback) {
        listeners.remove(callback);
    }
}
