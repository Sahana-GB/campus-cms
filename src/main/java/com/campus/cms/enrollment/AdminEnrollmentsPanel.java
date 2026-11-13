package com.campus.cms.enrollment;

import com.campus.cms.core.ui.FilterBar;
import com.campus.cms.core.ui.PageHeader;
import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;
import com.campus.cms.enrollment.EnrollmentDAO.CourseOption;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
    private final JButton periodButton = new JButton();
    private final JComboBox<String> statusBox = new JComboBox<>(
            new String[]{ALL_STATUS, "PENDING", "APPROVED", "REJECTED", "DROPPED", "COMPLETED"});
    private List<Enrollment> rows = List.of();
    private boolean loading;

    public AdminEnrollmentsPanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(0, Theme.SPACE_MD));
        setBorder(Styles.pagePadding());

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadge.Renderer());
        statusBox.setSelectedItem("PENDING");

        Styles.input(courseBox, 240);
        Styles.input(statusBox, 150);

        FilterBar filters = new FilterBar();
        filters.addLabeled("COURSE", courseBox);
        filters.addLabeled("STATUS", statusBox);
        JButton refresh = Styles.secondary(new JButton("Refresh"));
        refresh.addActionListener(e -> refresh());
        filters.add(refresh);
        Styles.secondary(periodButton);
        periodButton.addActionListener(e -> togglePeriod());
        filters.add(periodButton);

        JButton approve = Styles.primary(new JButton("Approve"));
        approve.addActionListener(e -> approveSelected());
        JButton reject = Styles.secondary(new JButton("Reject"));
        reject.addActionListener(e -> rejectSelected());
        JButton remove = Styles.destructive(new JButton("Remove"));
        remove.addActionListener(e -> removeSelected());
        JButton allocate = Styles.secondary(new JButton("Auto-allocate by CGPA"));
        allocate.addActionListener(e -> autoAllocate());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        buttons.setOpaque(false);
        buttons.add(approve);
        buttons.add(reject);
        buttons.add(remove);
        buttons.add(allocate);
        JLabel hint = new JLabel("Pick one course and PENDING to see CGPA and auto-allocate.");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        buttons.add(hint);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        PageHeader header = new PageHeader("Enrollment Management",
                "Review requests, approve seats and control the enrollment period.");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        filters.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(header);
        top.add(filters);

        add(top, BorderLayout.NORTH);
        add(Styles.scroll(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        courseBox.addActionListener(e -> loadTable());
        statusBox.addActionListener(e -> loadTable());
        UiUtil.refreshWhenShown(this, this::refresh);
    }

    /** Reload the course list and the table. Call this whenever the screen is shown. */
    public void refresh() {
        updatePeriodButton();
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

    private void updatePeriodButton() {
        try {
            periodButton.setText(service.isEnrollmentOpen()
                    ? "Enrollment: OPEN (click to close)" : "Enrollment: CLOSED (click to open)");
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
    }

    private void togglePeriod() {
        try {
            boolean open = service.isEnrollmentOpen();
            String question = open
                    ? "Close enrollment? Students will not be able to request courses."
                    : "Open enrollment? Students will be able to request courses.";
            if (UiUtil.confirm(this, question)) {
                service.setEnrollmentOpen(!open);
            }
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
        updatePeriodButton();
    }
}
