package com.campus.cms.core;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Reusable metric card component for dashboards displaying a title, prominent metric value,
 * and optional descriptive subtitle.
 */
public class DashboardCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;

    public DashboardCard(String title, String value) {
        this(title, value, null);
    }

    public DashboardCard(String title, String value, String subtitle) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1),
                new EmptyBorder(14, 16, 14, 16)
        ));

        // Title Label (Muted, upper-case header)
        titleLabel = new JLabel(title != null ? title.toUpperCase() : "");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLabel.setForeground(new Color(105, 115, 130));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Value Label (Large, bold metric)
        valueLabel = new JLabel(value != null ? value : "0");
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueLabel.setForeground(new Color(30, 35, 45));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle Label (Contextual note)
        subtitleLabel = new JLabel();
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        setSubtitle(subtitle);

        add(titleLabel);
        add(Box.createVerticalStrut(6));
        add(valueLabel);
        add(Box.createVerticalStrut(4));
        add(subtitleLabel);
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
