package com.campus.cms.enrollment;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Student screen: my enrollments with their status, and a Drop button. */
public class StudentCoursesPanel extends JPanel {
    private static final String[] COLUMNS = {"Code", "Title", "Slot", "Faculty", "Status", "Requested on"};

    private final EnrollmentService service;
    private final SimpleTableModel model = new SimpleTableModel(COLUMNS);
    private final JTable table = new JTable(model);
    private List<Enrollment> rows = List.of();

    public StudentCoursesPanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JButton drop = new JButton("Drop Selected Course");
        drop.addActionListener(e -> dropSelected());
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(drop);
        bottom.add(refresh);
        bottom.add(new JLabel("PENDING = waiting for approval, APPROVED = enrolled."));

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        UiUtil.refreshWhenShown(this, this::refresh);
    }

    public void refresh() {
        try {
            rows = service.myEnrollments(service.myStudentId());
            List<Object[]> data = new ArrayList<>();
            for (Enrollment e : rows) {
                data.add(new Object[]{e.getCourseCode(), e.getCourseTitle(), e.getSlot(),
                        e.getFacultyName(), e.getStatus(), e.getRequestDate()});
            }
            model.setRows(data);
        } catch (EnrollmentException | IllegalStateException e) {
            UiUtil.error(this, e.getMessage());
        }
    }

    private void dropSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtil.error(this, "Select a course first.");
            return;
        }
        Enrollment e = rows.get(table.convertRowIndexToModel(viewRow));
        if (!UiUtil.confirm(this, "Drop " + e.getCourseCode() + " - " + e.getCourseTitle() + "?")) {
            return;
        }
        try {
            service.drop(e.getEnrollmentId());
        } catch (EnrollmentException | IllegalStateException ex) {
            UiUtil.error(this, ex.getMessage());
        }
        refresh();
    }
}
