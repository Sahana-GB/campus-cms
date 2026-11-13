package com.campus.cms.core;

import com.campus.cms.core.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Reusable modern metric card component for dashboards displaying a title, prominent metric value,
 * optional descriptive subtitle, and top accent color indicator.
 */
public class DashboardCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;
    private Color accentColor;
    private boolean isHovered = false;

    public DashboardCard(String title, String value) {
        this(title, value, null, Theme.INFO);
    }

    public DashboardCard(String title, String value, String subtitle) {
        this(title, value, subtitle, Theme.INFO);
    }

    public DashboardCard(String title, String value, String subtitle, Color accentColor) {
        this.accentColor = accentColor != null ? accentColor : Theme.INFO;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                new EmptyBorder(16, 18, 14, 18)
        ));

        // Title Label (Muted, upper-case header)
        titleLabel = new JLabel(title != null ? title.toUpperCase() : "");
        titleLabel.setFont(Theme.SMALL_BOLD);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Value Label (Large, bold metric)
        valueLabel = new JLabel(value != null ? value : "0");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(Theme.TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle Label (Contextual note)
        subtitleLabel = new JLabel();
        subtitleLabel.setFont(Theme.SMALL);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        setSubtitle(subtitle);

        add(titleLabel);
        add(Box.createVerticalStrut(6));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(subtitleLabel);

        // Subtle hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(accentColor, 1),
                        new EmptyBorder(16, 18, 14, 18)
                ));
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Theme.BORDER, 1),
                        new EmptyBorder(16, 18, 14, 18)
                ));
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw top accent bar (4px height)
        if (accentColor != null) {
            g2.setColor(accentColor);
            g2.fillRect(0, 0, getWidth(), 4);
        }
        g2.dispose();
    }

    /**
     * Updates the metric value dynamically.
     *
     * @param value new string representation of the value
     */
    public void setValue(String value) {
        valueLabel.setText(value != null ? value : "0");
    }

    /**
     * Updates the metric value dynamically with numeric input.
     *
     * @param value numeric value to display
     */
    public void setValue(long value) {
        setValue(String.valueOf(value));
    }

    /**
     * Updates the card title.
     *
     * @param title new title
     */
    public void setTitle(String title) {
        titleLabel.setText(title != null ? title.toUpperCase() : "");
    }

    /**
     * Sets or updates the optional subtitle. Hides the label if null or empty.
     *
     * @param subtitle explanatory subtitle text
     */
    public void setSubtitle(String subtitle) {
        if (subtitle == null || subtitle.trim().isEmpty()) {
            subtitleLabel.setText("");
            subtitleLabel.setVisible(false);
        } else {
            subtitleLabel.setText(subtitle.trim());
            subtitleLabel.setVisible(true);
        }
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(Color accentColor) {
        this.accentColor = accentColor;
        repaint();
    }

    public String getTitle() {
        return titleLabel.getText();
    }

    public String getValue() {
        return valueLabel.getText();
    }

    public String getSubtitle() {
        return subtitleLabel.getText();
    }

    JLabel getTitleLabel() {
        return titleLabel;
    }

    JLabel getValueLabel() {
        return valueLabel;
    }

    JLabel getSubtitleLabel() {
        return subtitleLabel;
    }
}
