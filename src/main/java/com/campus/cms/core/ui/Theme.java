package com.campus.cms.core.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/** Design tokens (colors, typography, spacing, sizes) shared by every CCMS screen. */
public final class Theme {
    private Theme() { }

    // ---- Colors ----
    public static final Color PRIMARY = new Color(79, 70, 229);        // indigo-600
    public static final Color PRIMARY_HOVER = new Color(67, 56, 202);  // indigo-700
    public static final Color PRIMARY_SOFT = new Color(224, 231, 255); // indigo-100
    public static final Color BACKGROUND = new Color(244, 246, 250);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(225, 230, 238);
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color TEXT_MUTED = new Color(100, 116, 139);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color DANGER_HOVER = new Color(185, 28, 28);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color INFO = new Color(59, 130, 246);
    public static final Color ROW_ALT = new Color(249, 250, 253);
    public static final Color ROW_SELECTED = PRIMARY_SOFT;

    // ---- Typography ----
    public static final String FONT_FAMILY = "SansSerif";
    public static final Font TITLE = new Font(FONT_FAMILY, Font.BOLD, 22);
    public static final Font SUBTITLE = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font HEADING = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 11);

    // ---- Spacing ----
    public static final int SPACE_XS = 4;
    public static final int SPACE_SM = 8;
    public static final int SPACE_MD = 12;
    public static final int SPACE_LG = 16;
    public static final int SPACE_XL = 24;
    public static final int PAGE_PAD_V = 20;
    public static final int PAGE_PAD_H = 24;

    // ---- Component sizes ----
    public static final int CONTROL_HEIGHT = 32;
    public static final int NAV_HEIGHT = 38;
    public static final int SIDEBAR_WIDTH = 220;
    public static final int ROW_HEIGHT = 28;
    public static final int RADIUS = 8;

    public static Dimension controlSize(int width) {
        return new Dimension(width, CONTROL_HEIGHT);
    }
}
