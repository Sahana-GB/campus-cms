package com.campus.cms.enrollment;

import com.campus.cms.core.ui.StatusBadge;
import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;

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
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(table);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadge.Renderer());

        JButton drop = Styles.destructive(new JButton("Drop Selected Course"));
        drop.addActionListener(e -> dropSelected());
        JButton refresh = Styles.secondary(new JButton("Refresh"));
        refresh.addActionListener(e -> refresh());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_SM));
        bottom.setOpaque(false);
        bottom.add(drop);
        bottom.add(refresh);
        JLabel hint = new JLabel("PENDING = waiting for approval, APPROVED = enrolled.");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        bottom.add(hint);

        add(Styles.scroll(table), BorderLayout.CENTER);
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
