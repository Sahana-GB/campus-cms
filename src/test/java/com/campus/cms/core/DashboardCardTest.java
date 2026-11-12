package com.campus.cms.core;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class DashboardCardTest {

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @Test
    void cardInitializesWithTitleAndValue() {
        DashboardCard card = new DashboardCard("Active Courses", "18");

        assertEquals("ACTIVE COURSES", card.getTitle());
        assertEquals("18", card.getValue());
        assertEquals("", card.getSubtitle());
        assertFalse(card.getSubtitleLabel().isVisible());
    }

    @Test
    void cardInitializesWithSubtitle() {
        DashboardCard card = new DashboardCard("Enrollments", "450", "Fall 2026 Semester");

        assertEquals("ENROLLMENTS", card.getTitle());
        assertEquals("450", card.getValue());
        assertEquals("Fall 2026 Semester", card.getSubtitle());
        assertTrue(card.getSubtitleLabel().isVisible());
    }

    @Test
    void dynamicValueUpdatesReflectedInComponent() {
        DashboardCard card = new DashboardCard("Students", "100");

        card.setValue("150");
        assertEquals("150", card.getValue());
        assertEquals("150", card.getValueLabel().getText());

        card.setValue(275L);
        assertEquals("275", card.getValue());
        assertEquals("275", card.getValueLabel().getText());

        card.setValue(null);
        assertEquals("0", card.getValue());
    }

    @Test
    void dynamicTitleAndSubtitleUpdatesReflected() {
        DashboardCard card = new DashboardCard("Pending Approvals", "5");

        card.setTitle("Total Approvals");
        assertEquals("TOTAL APPROVALS", card.getTitle());

        card.setSubtitle("Needs action by Friday");
        assertEquals("Needs action by Friday", card.getSubtitle());
        assertTrue(card.getSubtitleLabel().isVisible());

        card.setSubtitle(null);
        assertEquals("", card.getSubtitle());
        assertFalse(card.getSubtitleLabel().isVisible());

        card.setSubtitle("   ");
        assertEquals("", card.getSubtitle());
        assertFalse(card.getSubtitleLabel().isVisible());
    }
}
