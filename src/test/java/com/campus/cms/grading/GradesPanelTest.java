package com.campus.cms.grading;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class GradesPanelTest {

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
    }

    @Test
    void studentModeHidesStudentIdColumnAndActionButtons() {
        // student mode: isFaculty = false, currentUserId = 1
        GradesPanel panel = new GradesPanel(1, false, 1);

        assertNotNull(panel.getGradesTable());
        // Hidden "Student ID" column means visible column count is 3
        assertEquals(3, panel.getGradesTable().getColumnCount());

        assertEquals("Marks", panel.getGradesTable().getColumnName(0));
        assertEquals("Max Marks", panel.getGradesTable().getColumnName(1));
        assertEquals("Feedback", panel.getGradesTable().getColumnName(2));

        for (int i = 0; i < panel.getGradesTable().getColumnCount(); i++) {
            assertNotEquals("Student ID", panel.getGradesTable().getColumnName(i));
        }

        // Faculty-only action buttons should be null in student mode
        assertNull(panel.getSaveBtn());
        assertNull(panel.getPublishBtn());
    }

    @Test
    void facultyModeKeepsStudentIdColumnAndActionButtons() {
        // faculty mode: isFaculty = true, currentUserId = 2
        GradesPanel panel = new GradesPanel(1, true, 2);

        assertNotNull(panel.getGradesTable());
        // In faculty mode, visible column count is 4
        assertEquals(4, panel.getGradesTable().getColumnCount());

        assertEquals("Student ID", panel.getGradesTable().getColumnName(0));
        assertEquals("Marks", panel.getGradesTable().getColumnName(1));
        assertEquals("Max Marks", panel.getGradesTable().getColumnName(2));
        assertEquals("Feedback", panel.getGradesTable().getColumnName(3));

        // Faculty-only action buttons should be initialized
        assertNotNull(panel.getSaveBtn());
        assertNotNull(panel.getPublishBtn());
    }

    @Test
    void assignmentComboBoxDisplaysTitleThroughCustomRenderer() {
        GradesPanel panel = new GradesPanel(1, false, 1);
        JComboBox<Assignment> combo = panel.getAssignmentCombo();
        assertNotNull(combo);

        ListCellRenderer<? super Assignment> renderer = combo.getRenderer();
        assertNotNull(renderer);

        Assignment assignment = new Assignment(101, 1, "Midterm Exam", "ASSIGNMENT", "Exam description", "2026-11-30", 100.0, 25.0, "OPEN", null);

        JList<Assignment> dummyList = new JList<>();
        Component comp = renderer.getListCellRendererComponent(dummyList, assignment, 0, false, false);

        assertTrue(comp instanceof JLabel);
        JLabel label = (JLabel) comp;
        assertEquals("Midterm Exam", label.getText());
        assertFalse(label.getText().contains("com.campus.cms.grading.Assignment@"));
    }
}
