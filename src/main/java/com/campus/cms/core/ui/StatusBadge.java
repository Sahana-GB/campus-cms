package com.campus.cms.core.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/** Small colored pill for statuses (ACTIVE, PENDING, OPEN ...). Usable as a component or table renderer. */
public class StatusBadge extends JLabel {
    private Color fill = Theme.BORDER;

    public StatusBadge(String text) {
        setFont(Theme.SMALL_BOLD);
        setOpaque(false);
        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setStatus(text);
    }

    public final void setStatus(String text) {
        String t = text == null ? "" : text;
        setText(t);
        Color[] c = colorsFor(t);
        fill = c[0];
        setForeground(c[1]);
        repaint();
    }

    /** Returns {background, foreground} for a status string. */
    public static Color[] colorsFor(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        return switch (s) {
            case "ACTIVE", "APPROVED", "OPEN", "PUBLISHED", "COMPLETED" ->
                    new Color[]{new Color(220, 252, 231), new Color(21, 128, 61)};
            case "INACTIVE", "REJECTED", "CLOSED", "ARCHIVED" ->
                    new Color[]{new Color(254, 226, 226), new Color(185, 28, 28)};
            case "PENDING", "DRAFT" ->
                    new Color[]{new Color(254, 243, 199), new Color(180, 83, 9)};
            case "DROPPED" ->
                    new Color[]{new Color(241, 245, 249), new Color(71, 85, 105)};
            default -> new Color[]{Theme.PRIMARY_SOFT, Theme.PRIMARY};
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    /** Table cell renderer drawing the cell value as a badge. Cell values stay plain strings. */
    public static class Renderer extends DefaultTableCellRenderer {
        private final StatusBadge badge = new StatusBadge("");
        private final JPanel cell = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean sel,
                                                       boolean focus, int row, int col) {
            badge.setStatus(value == null ? "" : value.toString());
            cell.removeAll();
            cell.setBackground(sel ? Theme.ROW_SELECTED : (row % 2 == 0 ? Theme.SURFACE : Theme.ROW_ALT));
            cell.setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(0, 10, 0, 0);
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            cell.add(badge, gc);
            return cell;
        }
    }
}
