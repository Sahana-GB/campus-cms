package com.campus.cms.students;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class StudentsPanelTest {

    private StubStudentDAO stubDAO;

    private static class StubStudentDAO extends StudentDAO {
        final List<Student> list = new ArrayList<>();
        boolean findAllCalled = false;
        boolean searchCalled = false;
        boolean deactivateCalled = false;
        boolean createCalled = false;
        boolean updateCalled = false;

        @Override
        public List<Student> findAll() {
            findAllCalled = true;
            return new ArrayList<>(list);
        }

        @Override
        public List<Student> search(String query, String branch, Integer year) {
            searchCalled = true;
            return list.stream()
                    .filter(s -> query == null || query.isEmpty() ||
                            s.getName().toLowerCase().contains(query.toLowerCase()) ||
                            s.getRollNumber().toLowerCase().contains(query.toLowerCase()))
                    .filter(s -> branch == null || s.getBranch().equalsIgnoreCase(branch))
                    .filter(s -> year == null || s.getYear() == year)
                    .toList();
        }

        @Override
        public Student createStudent(String username, String password, String rollNumber,
                                     String name, String branch, int year, String email) {
            createCalled = true;
            Student s = new Student(list.size() + 1, list.size() + 10, rollNumber, name, branch, year, email, "ACTIVE", username);
            list.add(s);
            return s;
        }

        @Override
        public boolean update(int studentId, String name, String branch, int year, String email) {
            updateCalled = true;
            for (int i = 0; i < list.size(); i++) {
                Student s = list.get(i);
                if (s.getStudentId() == studentId) {
                    list.set(i, new Student(s.getStudentId(), s.getUserId(), s.getRollNumber(), name, branch, year, email, s.getStatus(), s.getUsername()));
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean deactivate(int studentId) {
            deactivateCalled = true;
            for (int i = 0; i < list.size(); i++) {
                Student s = list.get(i);
                if (s.getStudentId() == studentId) {
                    list.set(i, new Student(s.getStudentId(), s.getUserId(), s.getRollNumber(), s.getName(), s.getBranch(), s.getYear(), s.getEmail(), "INACTIVE", s.getUsername()));
                    return true;
                }
            }
            return false;
        }

        @Override
        public Optional<Student> findById(int studentId) {
            return list.stream().filter(s -> s.getStudentId() == studentId).findFirst();
        }
    }

    @BeforeEach
    void setUp() {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        stubDAO = new StubStudentDAO();
        stubDAO.list.add(new Student(1, 10, "CS2401", "Asha Reddy", "CSE", 2, "asha@campus.edu", "ACTIVE", "asha"));
        stubDAO.list.add(new Student(2, 11, "EC2401", "Karthik Rao", "ECE", 3, "karthik@campus.edu", "INACTIVE", "karthik"));
    }

    @Test
    void constructingStudentsPanelDoesNotCallFindAllAndInitializesEmptyTable() {
        StudentsPanel panel = new StudentsPanel(stubDAO);

        assertFalse(stubDAO.findAllCalled, "Constructor must not call StudentDAO.findAll()");
        assertFalse(stubDAO.searchCalled, "Constructor must not call StudentDAO.search()");

        assertEquals(6, panel.getStudentTable().getColumnCount());
        assertEquals("Roll Number", panel.getStudentTable().getColumnName(0));
        assertEquals("Name", panel.getStudentTable().getColumnName(1));
        assertEquals("Branch", panel.getStudentTable().getColumnName(2));
        assertEquals("Year", panel.getStudentTable().getColumnName(3));
        assertEquals("Email", panel.getStudentTable().getColumnName(4));
        assertEquals("Status", panel.getStudentTable().getColumnName(5));

        assertEquals(0, panel.getTableModel().getRowCount(), "Table model must be empty on initialization");
        assertEquals("No students loaded.", panel.getCountLabel().getText());
    }

    @Test
    void tableInitializesWithColumnsAndLoadsAllStudents() {
        StudentsPanel panel = new StudentsPanel(stubDAO);
        panel.loadStudents();

        assertTrue(stubDAO.findAllCalled);
        assertEquals(6, panel.getStudentTable().getColumnCount());
        assertEquals("Roll Number", panel.getStudentTable().getColumnName(0));
        assertEquals("Name", panel.getStudentTable().getColumnName(1));
        assertEquals("Branch", panel.getStudentTable().getColumnName(2));
        assertEquals("Year", panel.getStudentTable().getColumnName(3));
        assertEquals("Email", panel.getStudentTable().getColumnName(4));
        assertEquals("Status", panel.getStudentTable().getColumnName(5));

        assertEquals(2, panel.getTableModel().getRowCount());
        assertEquals("CS2401", panel.getTableModel().getValueAt(0, 0));
        assertEquals("EC2401", panel.getTableModel().getValueAt(1, 0));
        assertTrue(panel.getCountLabel().getText().contains("2 students"));
    }

    @Test
    void searchFilterUpdatesTable() {
        StudentsPanel panel = new StudentsPanel(stubDAO);

        panel.getSearchField().setText("asha");
        panel.loadStudents();

        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("CS2401", panel.getTableModel().getValueAt(0, 0));
    }

    @Test
    void statusFilterSelectsActiveOrInactiveOnly() {
        StudentsPanel panel = new StudentsPanel(stubDAO);

        // Active only
        panel.getStatusFilter().setSelectedItem("Active Only");
        panel.loadStudents();
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("CS2401", panel.getTableModel().getValueAt(0, 0));

        // Inactive only
        panel.getStatusFilter().setSelectedItem("Inactive Only");
        panel.loadStudents();
        assertEquals(1, panel.getTableModel().getRowCount());
        assertEquals("EC2401", panel.getTableModel().getValueAt(0, 0));
    }

    @Test
    void selectionEnablesActionButtonsAccordingToActiveStatus() {
        StudentsPanel panel = new StudentsPanel(stubDAO);
        panel.loadStudents();

        // Initially no selection
        assertFalse(panel.getEditStudentButton().isEnabled());
        assertFalse(panel.getDeactivateStudentButton().isEnabled());

        // Select row 0 (Active student)
        panel.getStudentTable().setRowSelectionInterval(0, 0);
        assertTrue(panel.getEditStudentButton().isEnabled());
        assertTrue(panel.getDeactivateStudentButton().isEnabled());

        // Select row 1 (Inactive student)
        panel.getStudentTable().setRowSelectionInterval(1, 1);
        assertTrue(panel.getEditStudentButton().isEnabled());
        assertFalse(panel.getDeactivateStudentButton().isEnabled()); // Inactive student cannot be deactivated again
    }

    @Test
    void emptyResultsHandledGracefully() {
        StudentsPanel panel = new StudentsPanel(stubDAO);

        panel.getSearchField().setText("NonExistentStudent");
        panel.loadStudents();

        assertEquals(0, panel.getTableModel().getRowCount());
        assertTrue(panel.getCountLabel().getText().contains("No students found"));
    }

    @Test
    void deactivateStudentTriggersDAOAndRefreshes() {
        AtomicBoolean confirmAsked = new AtomicBoolean(false);

        StudentsPanel panel = new StudentsPanel(stubDAO) {
            @Override
            protected int showConfirmDialog(Object message, String title, int optionType, int messageType) {
                confirmAsked.set(true);
                return javax.swing.JOptionPane.YES_OPTION;
            }

            @Override
            protected void showSuccessMessage(String message) {
                // Suppress visual dialog popup
            }
        };

        panel.loadStudents();

        // Select active student (row 0)
        panel.getStudentTable().setRowSelectionInterval(0, 0);
        panel.handleDeactivateStudent();

        assertTrue(confirmAsked.get());
        assertTrue(stubDAO.deactivateCalled);
        // Record at row 0 is now INACTIVE
        assertEquals("INACTIVE", panel.getTableModel().getValueAt(0, 5));
    }
}
