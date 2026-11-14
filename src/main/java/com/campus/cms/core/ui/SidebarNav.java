package com.campus.cms.core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shared sidebar: brand, user chip, navigation buttons with an active indicator, and a pinned logout.
 * Button text is never altered, so callers' labels (and tests) stay valid.
 */
public class SidebarNav extends JPanel {
    private final JPanel navArea = new JPanel();
    private JButton active;

    public SidebarNav(String brand, String roleLabel, String displayName) {
        setLayout(new BorderLayout());
        setBackground(Theme.SURFACE);
        setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH, 0));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER),
                BorderFactory.createEmptyBorder(Theme.SPACE_XL, Theme.SPACE_MD + 2, Theme.SPACE_LG, Theme.SPACE_MD + 2)));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JLabel brandLabel = new JLabel(brand);
        brandLabel.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 18));
        brandLabel.setForeground(Theme.PRIMARY);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(brandLabel);
        top.add(Box.createVerticalStrut(Theme.SPACE_MD));
        top.add(userChip(roleLabel, displayName));
        top.add(Box.createVerticalStrut(Theme.SPACE_XL));

        navArea.setOpaque(false);
        navArea.setLayout(new BoxLayout(navArea, BoxLayout.Y_AXIS));
        navArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(navArea);

        add(top, BorderLayout.NORTH);
    }

    private JComponent userChip(String roleLabel, String displayName) {
        String name = displayName == null || displayName.isBlank() ? roleLabel : displayName;
        JPanel chip = new JPanel(new BorderLayout(Theme.SPACE_SM + 2, 0));
        chip.setBackground(new Color(248, 250, 252));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER, 1),
                BorderFactory.createEmptyBorder(Theme.SPACE_SM, Theme.SPACE_SM, Theme.SPACE_SM, Theme.SPACE_SM)));
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        String initials = initials(name);
        JLabel avatar = new JLabel(initials, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PRIMARY);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setOpaque(false);
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font(Theme.FONT_FAMILY, Font.BOLD, 12));
        avatar.setPreferredSize(new Dimension(34, 34));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel n = new JLabel(name);
        n.setFont(Theme.BODY_BOLD);
        n.setForeground(Theme.TEXT);
        JLabel r = new JLabel(roleLabel);
        r.setFont(Theme.SMALL);
        r.setForeground(Theme.TEXT_MUTED);
        text.add(n);
        text.add(r);

        chip.add(avatar, BorderLayout.WEST);
        chip.add(text, BorderLayout.CENTER);
        return chip;
    }

    static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        String p = parts[0];
        return p.substring(0, Math.min(2, p.length())).toUpperCase();
    }

    /** Styles a button as a nav item and appends it to the navigation list. */
    public JButton addNav(JButton button) {
        styleNav(button);
        if (navArea.getComponentCount() > 0) {
            navArea.add(Box.createVerticalStrut(Theme.SPACE_XS));
        }
        navArea.add(button);
        return button;
    }

    /** Styles a button as the logout item pinned to the bottom. */
    public JButton setFooter(JButton button) {
        styleNav(button);
        button.setForeground(Theme.DANGER);
        add(button, BorderLayout.SOUTH);
        return button;
    }

    private void styleNav(JButton b) {
        b.setFont(Theme.BODY);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(true);
        b.setOpaque(true);
        b.setBackground(Theme.SURFACE);
        b.setForeground(Theme.TEXT);
        b.setMargin(new Insets(0, 14, 0, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setPreferredSize(new Dimension(Theme.SIDEBAR_WIDTH - 32, Theme.NAV_HEIGHT));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.NAV_HEIGHT));
        b.putClientProperty("FlatLaf.style", "arc: 8; focusWidth: 0");
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b != active && b.isEnabled()) {
                    b.setBackground(new Color(241, 245, 249));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (b != active) {
                    b.setBackground(Theme.SURFACE);
                }
            }
        });
    }

    /** Highlights the given nav button; resets the previously active one. */
    public void setActive(JButton button) {
        if (active != null) {
            active.setBackground(Theme.SURFACE);
            active.setForeground(Theme.TEXT);
            active.setFont(Theme.BODY);
        }
        active = button;
        if (active != null) {
            active.setBackground(Theme.PRIMARY_SOFT);
            active.setForeground(Theme.PRIMARY);
            active.setFont(Theme.BODY_BOLD);
        }
    }

    public JButton getActive() {
        return active;
    }
}
