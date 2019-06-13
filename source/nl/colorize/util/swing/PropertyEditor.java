//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2019 Colorize
// Apache license (http://www.colorize.nl/code_license.txt)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import com.google.common.collect.ImmutableList;
import nl.colorize.util.DynamicResourceBundle;

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
import java.util.stream.Collectors;

/**
 * Simple user interface for editing name/value properties. The interface
 * consists of a list of available properties, plus buttons to add, change,
 * or remove properties. The provided callback will be invoked whenever the
 * underlying properties are modified.
 */
public class PropertyEditor extends JPanel {

    private Map<String, String> properties;
    private DynamicResourceBundle bundle;
    private List<Consumer<Map<String, String>>> listeners;

    private Table<String> table;

    public PropertyEditor(Map<String, String> initialProperties) {
        super(new BorderLayout(0, 10));

        this.properties = new LinkedHashMap<>();
        this.bundle = SwingUtils.getCustomComponentsBundle();
        this.listeners = new ArrayList<>();

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
            return ImmutableList.copyOf(initialProperties.keySet());
        } else {
            return initialProperties.keySet().stream()
                .sorted()
                .collect(Collectors.toList());
        }
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
            notifyListeners();
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
            notifyListeners();
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

    public void addListener(Consumer<Map<String, String>> callback) {
        listeners.add(callback);
    }

    public void removeListener(Consumer<Map<String, String>> callback) {
        listeners.remove(callback);
    }

    private void notifyListeners() {
        listeners.forEach(listener -> listener.accept(properties));
    }
}
