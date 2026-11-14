package com.campus.cms.core.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/** Reusable styling helpers: buttons, tables, inputs, panels. Purely visual. */
public final class Styles {
    private Styles() { }

    public static Border pagePadding() {
        return new EmptyBorder(Theme.PAGE_PAD_V, Theme.PAGE_PAD_H, Theme.PAGE_PAD_V, Theme.PAGE_PAD_H);
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));
    }

    /** White rounded-looking surface panel with a hairline border. */
    public static JPanel surface(LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBackground(Theme.SURFACE);
        p.setBorder(cardBorder());
        return p;
    }

    /**
     * Responsive action panel that wraps buttons cleanly across rows with vertical spacing
     * and dynamic height, preventing buttons from getting clipped on window resize.
     */
    public static JPanel actionBar() {
        JPanel p = new JPanel(new FilterBar.WrapLayout(FlowLayout.LEFT, Theme.SPACE_MD, Theme.SPACE_SM)) {
            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                return new Dimension(Integer.MAX_VALUE, pref.height);
            }
        };
        p.setBackground(Theme.SURFACE);
        p.setBorder(cardBorder());
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    // ---- Buttons ----
    private static void baseButton(AbstractButton b) {
        b.setFont(Theme.BODY);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 14, 4, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static <T extends AbstractButton> T primary(T b) {
        baseButton(b);
        b.setFont(Theme.BODY_BOLD);
        b.setBackground(Theme.PRIMARY);
        b.setForeground(Color.WHITE);
        b.putClientProperty("FlatLaf.style",
                "background: #4F46E5; foreground: #FFFFFF; hoverBackground: #4338CA; "
                        + "pressedBackground: #3730A3; borderWidth: 0; focusWidth: 0; arc: 8; minimumHeight: 32; minimumWidth: 80");
        return b;
    }

    public static <T extends AbstractButton> T secondary(T b) {
        baseButton(b);
        b.setBackground(Theme.SURFACE);
        b.setForeground(Theme.TEXT);
        b.putClientProperty("FlatLaf.style",
                "background: #FFFFFF; foreground: #0F172A; hoverBackground: #F1F5F9; "
                        + "borderColor: #CBD5E1; focusWidth: 0; arc: 8; minimumHeight: 32; minimumWidth: 80");
        return b;
    }

    public static <T extends AbstractButton> T destructive(T b) {
        baseButton(b);
        b.setBackground(Theme.SURFACE);
        b.setForeground(Theme.DANGER);
        b.putClientProperty("FlatLaf.style",
                "background: #FFFFFF; foreground: #DC2626; hoverBackground: #FEF2F2; "
                        + "borderColor: #FCA5A5; focusWidth: 0; arc: 8; minimumHeight: 32; minimumWidth: 80");
        return b;
    }

    // ---- Inputs ----
    public static void input(JComponent c, int width) {
        c.setFont(Theme.BODY);
        c.setPreferredSize(new Dimension(width, Theme.CONTROL_HEIGHT));
        c.putClientProperty("FlatLaf.style", "arc: 8");
    }

    public static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_BOLD);
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }

    // ---- Tables ----
    public static void table(JTable t) {
        t.setFont(Theme.BODY);
        t.setRowHeight(Theme.ROW_HEIGHT);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(238, 241, 246));
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setSelectionBackground(Theme.ROW_SELECTED);
        t.setSelectionForeground(Theme.TEXT);
        t.setFillsViewportHeight(true);
        if (t.getRowSorter() == null) {
            t.setAutoCreateRowSorter(true);
        }

        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 34));
        h.setFont(Theme.SMALL_BOLD);
        h.setBackground(new Color(248, 250, 252));
        h.setForeground(Theme.TEXT_MUTED);
        h.putClientProperty("FlatLaf.style", "bottomSeparatorColor: #E1E6EE; separatorColor: #F1F5F9");

        t.setDefaultRenderer(Object.class, new ZebraRenderer());
        ZebraRenderer numbers = new ZebraRenderer();
        numbers.setHorizontalAlignment(SwingConstants.RIGHT);
        t.setDefaultRenderer(Number.class, numbers);
    }

    public static JScrollPane scroll(JComponent view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER, 1));
        sp.getViewport().setBackground(Theme.SURFACE);
        return sp;
    }

    /** Alternating row renderer with cell padding. */
    private static class ZebraRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, false, row, col);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            if (sel) {
                setBackground(Theme.ROW_SELECTED);
                setForeground(Theme.TEXT);
            } else {
                setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT);
                setForeground(Theme.TEXT);
            }
            return this;
        }
    }

    // ---- Dialogs / forms ----
    public static JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.SURFACE);
        p.setBorder(new EmptyBorder(Theme.SPACE_SM, Theme.SPACE_SM, Theme.SPACE_SM, Theme.SPACE_SM));
        return p;
    }

    /** Adds a label/field row to a GridBagLayout form panel. */
    public static void addFormRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.insets = new Insets(5, 4, 5, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        JLabel l = new JLabel(label);
        l.setFont(Theme.BODY);
        form.add(l, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        field.setPreferredSize(new Dimension(Math.max(field.getPreferredSize().width, 240), Theme.CONTROL_HEIGHT));
        form.add(field, gc);
    }
}
