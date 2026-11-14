package com.campus.cms.core.ui;

import javax.swing.*;
import java.awt.*;

/** Page title with an optional muted subtitle and an optional right-aligned component. */
public class PageHeader extends JPanel {
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();

    public PageHeader(String titleText, String subtitleText) {
        super(new BorderLayout(Theme.SPACE_MD, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_LG, 0));

        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        subtitle.setFont(Theme.SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);
        add(text, BorderLayout.CENTER);

        setTitle(titleText);
        setSubtitle(subtitleText);
    }

    public PageHeader(String titleText) {
        this(titleText, null);
    }

    public void setTitle(String t) {
        title.setText(t == null ? "" : t);
    }

    public void setSubtitle(String s) {
        boolean has = s != null && !s.isBlank();
        subtitle.setText(has ? s : "");
        subtitle.setVisible(has);
    }

    public void setTrailing(JComponent c) {
        // Wrap so BorderLayout does not stretch the component to the header height.
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.add(c);
        add(holder, BorderLayout.EAST);
    }

    public JLabel getTitleLabel() {
        return title;
    }

    public JLabel getSubtitleLabel() {
        return subtitle;
    }
}
