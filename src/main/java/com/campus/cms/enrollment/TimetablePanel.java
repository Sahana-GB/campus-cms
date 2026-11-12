package com.campus.cms.enrollment;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Student screen: my APPROVED courses by slot.
 * The final schema has no timetable table; a slot (A, B, C...) is only a label, so no clock times are shown.
 */
public class TimetablePanel extends JPanel {
    private static final String[] COLUMNS = {"Slot", "Code", "Title", "Faculty"};

    private final EnrollmentService service;
    private final SimpleTableModel model = new SimpleTableModel(COLUMNS);

    public TimetablePanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(table.getRowHeight() + 6);

        add(new JLabel("My courses by slot (two courses never share a slot, so there are no clashes)"),
                BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        UiUtil.refreshWhenShown(this, this::refresh);
    }

    public void refresh() {
        try {
            List<Object[]> data = new ArrayList<>();
            for (Enrollment e : service.myTimetable(service.myStudentId())) {
                data.add(new Object[]{e.getSlot(), e.getCourseCode(), e.getCourseTitle(), e.getFacultyName()});
            }
            model.setRows(data);
        } catch (EnrollmentException | IllegalStateException e) {
            UiUtil.error(this, e.getMessage());
        }
    }
}
