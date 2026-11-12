package com.campus.cms.enrollment;

import com.campus.cms.enrollment.EnrollmentDAO.CourseOption;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Admin screen: approve, reject or remove enrollments, and auto-allocate seats by CGPA. */
public class AdminEnrollmentsPanel extends JPanel {
    private static final String[] COLUMNS =
            {"Roll no.", "Student", "Course", "Slot", "Status", "Requested on", "CGPA"};
    private static final CourseOption ALL_COURSES = new CourseOption(-1, "", "All courses");
    private static final String ALL_STATUS = "All statuses";

    private interface Task {
        void run() throws EnrollmentException;
    }

    private final EnrollmentService service;
    private final SimpleTableModel model = new SimpleTableModel(COLUMNS);
    private final JTable table = new JTable(model);
    private final JComboBox<CourseOption> courseBox = new JComboBox<>();
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[]{ALL_STATUS, "PENDING", "APPROVED", "REJECTED", "DROPPED", "COMPLETED"});
    private List<Enrollment> rows = List.of();
    private boolean loading;

    public AdminEnrollmentsPanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        statusBox.setSelectedItem("PENDING");

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("Course:"));
        filters.add(courseBox);
        filters.add(new JLabel("Status:"));
        filters.add(statusBox);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        filters.add(refresh);

        JButton approve = new JButton("Approve");
        approve.addActionListener(e -> approveSelected());
        JButton reject = new JButton("Reject");
        reject.addActionListener(e -> rejectSelected());
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> removeSelected());
        JButton allocate = new JButton("Auto-allocate by CGPA");
        allocate.addActionListener(e -> autoAllocate());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(approve);
        buttons.add(reject);
        buttons.add(remove);
        buttons.add(allocate);
        buttons.add(new JLabel("Pick one course and PENDING to see CGPA and auto-allocate."));

        add(filters, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        refresh();
        courseBox.addActionListener(e -> loadTable());
        statusBox.addActionListener(e -> loadTable());
    }

    /** Reload the course list and the table. Call this whenever the screen is shown. */
    public void refresh() {
        loading = true;
        try {
            CourseOption previous = (CourseOption) courseBox.getSelectedItem();
            courseBox.removeAllItems();
            courseBox.addItem(ALL_COURSES);
            for (CourseOption o : service.courseOptions()) {
                courseBox.addItem(o);
            }
            if (previous != null) {
                for (int i = 0; i < courseBox.getItemCount(); i++) {
                    if (courseBox.getItemAt(i).courseId() == previous.courseId()) {
                        courseBox.setSelectedIndex(i);
                    }
                }
            }
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        } finally {
            loading = false;
        }
        loadTable();
    }

    private Integer selectedCourseId() {
        CourseOption course = (CourseOption) courseBox.getSelectedItem();
        return (course == null || course.courseId() < 0) ? null : course.courseId();
    }

    private String selectedStatus() {
        String status = (String) statusBox.getSelectedItem();
        return ALL_STATUS.equals(status) ? null : status;
    }

    private void loadTable() {
        if (loading) {
            return;
        }
        Integer courseId = selectedCourseId();
        String status = selectedStatus();
        boolean withCgpa = courseId != null && Enrollment.PENDING.equals(status);
        try {
            rows = service.listEnrollments(courseId, status, withCgpa);
            List<Object[]> data = new ArrayList<>();
            for (Enrollment e : rows) {
                data.add(new Object[]{e.getRollNumber(), e.getStudentName(),
                        e.getCourseCode() + " - " + e.getCourseTitle(), e.getSlot(), e.getStatus(),
                        e.getRequestDate(), withCgpa ? (Object) e.getCgpa() : ""});
            }
            model.setRows(data);
        } catch (EnrollmentException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private Enrollment selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtil.error(this, "Select an enrollment first.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(viewRow));
    }

    private void perform(Task task) {
        try {
            task.run();
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
        refresh();
    }

    private void approveSelected() {
        Enrollment e = selected();
        if (e != null) {
            perform(() -> service.approve(e.getEnrollmentId()));
        }
    }

    private void rejectSelected() {
        Enrollment e = selected();
        if (e != null && UiUtil.confirm(this, "Reject " + e.getStudentName() + " for " + e.getCourseCode() + "?")) {
            perform(() -> service.reject(e.getEnrollmentId()));
        }
    }

    private void removeSelected() {
        Enrollment e = selected();
        if (e != null && UiUtil.confirm(this, "Remove " + e.getStudentName() + " from " + e.getCourseCode() + "?")) {
            perform(() -> service.drop(e.getEnrollmentId()));
        }
    }

    private void autoAllocate() {
        Integer courseId = selectedCourseId();
        if (courseId == null) {
            UiUtil.error(this, "Choose one course first.");
            return;
        }
        if (!UiUtil.confirm(this, "Approve pending requests for this course, highest CGPA first, until it is full?")) {
            return;
        }
        perform(() -> {
            EnrollmentService.AllocationResult result = service.autoAllocate(courseId);
            UiUtil.info(this, result.approved() + " approved, " + result.stillPending() + " still pending.");
        });
    }
}
