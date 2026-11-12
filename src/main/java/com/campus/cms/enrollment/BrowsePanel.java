package com.campus.cms.enrollment;

import com.campus.cms.enrollment.EnrollmentDAO.BrowseRow;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/** Student screen: browse ACTIVE courses and request enrollment. */
public class BrowsePanel extends JPanel {
    private static final String[] COLUMNS =
            {"Code", "Title", "Faculty", "Credits", "Slot", "Seats left", "My status"};

    private final EnrollmentService service;
    private final int studentId;
    private final SimpleTableModel model = new SimpleTableModel(COLUMNS);
    private final JTable table = new JTable(model);
    private final TableRowSorter<SimpleTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(22);
    private List<BrowseRow> rows = List.of();

    public BrowsePanel(EnrollmentService service, int studentId) {
        this.service = service;
        this.studentId = studentId;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        top.add(refresh);

        JButton request = new JButton("Request Enrollment");
        request.addActionListener(e -> requestSelected());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(request);
        bottom.add(new JLabel("An admin must approve your request."));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        UiUtil.onChange(searchField, () -> UiUtil.setSearch(sorter, searchField.getText()));
        refresh();
    }

    /** Reload from the database. Call this whenever the screen is shown. */
    public void refresh() {
        try {
            rows = service.browseCourses(studentId);
            List<Object[]> data = new ArrayList<>();
            for (BrowseRow r : rows) {
                data.add(new Object[]{r.code(), r.title(), r.facultyName(), r.credits(), r.slot(),
                        r.seatsLeft(), r.myStatus() == null ? "" : r.myStatus()});
            }
            model.setRows(data);
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
    }

    private void requestSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            UiUtil.error(this, "Select a course first.");
            return;
        }
        BrowseRow course = rows.get(table.convertRowIndexToModel(viewRow));
        try {
            service.requestEnrollment(studentId, course.courseId());
            UiUtil.info(this, "Request sent for " + course.code() + ".\nStatus: PENDING (waiting for admin approval).");
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
        refresh();
    }
}
