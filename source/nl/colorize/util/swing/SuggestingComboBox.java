//-----------------------------------------------------------------------------
// Colorize Java Commons
// Copyright 2007-2026 Colorize
// Apache license (http://www.apache.org/licenses/LICENSE-2.0)
//-----------------------------------------------------------------------------

package nl.colorize.util.swing;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Text field that shows suggestions based on the entered text and history,
 * similar to a browser's combined URL/search field.
 */
public class SuggestingComboBox extends JTextField implements DocumentListener, FocusListener {

    private List<JMenuItem> suggestionItems;
    private List<JMenuItem> currentlyValidItems;
    private AtomicBoolean locked;
    private JPopupMenu popup;

    private static final int MINIMAL_MATCH = 2;
    private static final Color ARROW_COLOR = new Color(140, 140, 140);
    private static final int ARROW_HIT_AREA = 30;

    public SuggestingComboBox() {
        suggestionItems = new ArrayList<>();
        currentlyValidItems = new ArrayList<>();
        locked = new AtomicBoolean(false);

        getDocument().addDocumentListener(this);
        addFocusListener(this);
        addKeyListener(SwingUtils.createKeyReleasedListener(this::onKeyReleased));
        addMouseListener(SwingUtils.toMouseReleasedListener(this::onMouseReleased));
        addActionListener(_ -> hidePopup());

        popup = new JPopupMenu();
        popup.setFocusable(false);
    }

    private void onKeyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP -> moveSelection(-1);
            case KeyEvent.VK_DOWN -> moveSelection(1);
            default -> moveSelection(0);
        }
    }

    private void onMouseReleased(MouseEvent e) {
        if (e.getX() > getWidth() - ARROW_HIT_AREA && e.getX() < getWidth() &&
            e.getY() > 0 && e.getY() < getHeight()) {
            showPopup(true);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        hidePopup();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        showPopup(false);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        showPopup(false);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        showPopup(false);
    }

    @Override
    public void setText(String text) {
        locked.set(true);
        super.setText(text);
        locked.set(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!suggestionItems.isEmpty()) {
            Graphics2D g2 = Utils2D.createGraphics(g, true, false);
            g2.setColor(ARROW_COLOR);
            Utils2D.drawStringRight(g2, "▼", getWidth() - 10, Math.round(0.7f * getHeight()));
        }
    }

    private void showPopup(boolean showAllSuggestions) {
        if (!locked.get()) {
            updateContentAssistSuggestions(showAllSuggestions);
        }
    }

    private void updateContentAssistSuggestions(boolean showAllSuggestions) {
        List<JMenuItem> validItems = determineValidSuggestions(getText(), showAllSuggestions);

        if (!currentlyValidItems.equals(validItems)) {
            currentlyValidItems = validItems;

            popup.removeAll();
            for (JMenuItem suggestionItem : currentlyValidItems) {
                popup.add(suggestionItem);
            }
        }

        if (currentlyValidItems.isEmpty()) {
            hidePopup();
        } else {
            popup.revalidate();
            popup.pack();
            popup.show(this, 0, getHeight());
        }
    }

    private List<JMenuItem> determineValidSuggestions(String typedText, boolean showAllSuggestions) {
        if (showAllSuggestions) {
            return suggestionItems;
        }

        if (typedText.isEmpty() || suggestionItems.isEmpty()) {
            return Collections.emptyList();
        }

        return suggestionItems.stream()
            .filter(item -> isValidSuggestion(typedText, item.getText()))
            .collect(Collectors.toList());
    }

    private boolean isValidSuggestion(String typedText, String suggestion) {
        typedText = typedText.toLowerCase();
        boolean match = typedText.contains(suggestion) || suggestion.contains(typedText);
        return match && typedText.length() >= MINIMAL_MATCH;
    }

    private void changeSelection(ActionEvent e) {
        setSelection((JMenuItem) e.getSource());
        hidePopup();
    }

    private void moveSelection(int delta) {
        if (currentlyValidItems.isEmpty() || locked.get() || delta == 0) {
            return;
        }

        int index = getSelectionIndex() + delta;
        index = (index < 0) ? currentlyValidItems.size() - 1 : index;
        index = (index >= currentlyValidItems.size()) ? 0 : index;

        setSelection(currentlyValidItems.get(index));
    }

    private void setSelection(JMenuItem selectedItem) {
        locked.set(true);
        MenuElement[] newSelection = {popup, selectedItem};
        MenuSelectionManager.defaultManager().setSelectedPath(newSelection);
        setText(selectedItem.getText());
        locked.set(false);
    }

    private int getSelectionIndex() {
        MenuElement[] selection = MenuSelectionManager.defaultManager().getSelectedPath();
        if (selection.length == 2) {
            for (int i = 0; i < currentlyValidItems.size(); i++) {
                if (currentlyValidItems.get(i).equals(selection[1])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void hidePopup() {
        popup.setVisible(false);
    }

    /**
     * Registers the specified objects as suggestions. Any objects with a string
     * form that matches part of the entered text will be down shown in content
     * assist.
     */
    public void setContentAssistSuggestions(List<?> suggestions) {
        hidePopup();
        suggestionItems.clear();

        List<String> stringForms = suggestions.stream()
            .map(suggestion -> suggestion.toString().toLowerCase())
            .sorted()
            .toList();

        for (String suggestion : stringForms) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(this::changeSelection);
            suggestionItems.add(item);
        }
    }
}
