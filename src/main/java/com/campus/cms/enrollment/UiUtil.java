package com.campus.cms.enrollment;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.util.regex.Pattern;

/** Small helpers shared by the enrollment panels. */
final class UiUtil {
    private UiUtil() { }

    static void onChange(JTextComponent field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        });
    }

    /** Case-insensitive "contains" search across all columns. Blank text shows everything. */
    static void setSearch(TableRowSorter<?> sorter, String text) {
        if (text == null || text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
        }
    }

    /** Runs the refresh every time the panel becomes visible (menu click, tab switch). Nothing runs at construction. */
    static void refreshWhenShown(JComponent panel, Runnable refresh) {
        panel.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && panel.isShowing()) {
                refresh.run();
            }
        });
    }

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Cannot complete this action", JOptionPane.WARNING_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
