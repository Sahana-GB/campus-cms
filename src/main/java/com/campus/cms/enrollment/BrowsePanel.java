package com.campus.cms.enrollment;

import com.campus.cms.core.ui.Styles;
import com.campus.cms.core.ui.Theme;
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
    private final SimpleTableModel model = new SimpleTableModel(COLUMNS);
    private final JTable table = new JTable(model);
    private final TableRowSorter<SimpleTableModel> sorter = new TableRowSorter<>(model);
    private final JTextField searchField = new JTextField(22);
    private List<BrowseRow> rows = List.of();

    public BrowsePanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG, Theme.SPACE_LG));

        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Styles.table(table);

        Styles.input(searchField, 260);
        searchField.putClientProperty("JTextField.placeholderText", "Search courses...");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_SM));
        top.setOpaque(false);
        top.add(Styles.fieldLabel("SEARCH"));
        top.add(searchField);
        JButton refresh = Styles.secondary(new JButton("Refresh"));
        refresh.addActionListener(e -> refresh());
        top.add(refresh);

        JButton request = Styles.primary(new JButton("Request Enrollment"));
        request.addActionListener(e -> requestSelected());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, Theme.SPACE_SM));
        bottom.setOpaque(false);
        bottom.add(request);
        JLabel hint = new JLabel("An admin must approve your request.");
        hint.setFont(Theme.SMALL);
        hint.setForeground(Theme.TEXT_MUTED);
        bottom.add(hint);

        add(top, BorderLayout.NORTH);
        add(Styles.scroll(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        UiUtil.onChange(searchField, () -> UiUtil.setSearch(sorter, searchField.getText()));
        UiUtil.refreshWhenShown(this, this::refresh);
    }

    /** Reload from the database. Call this whenever the screen is shown. */
    public void refresh() {
        try {
            rows = service.browseCourses(service.myStudentId());
            List<Object[]> data = new ArrayList<>();
            for (BrowseRow r : rows) {
                data.add(new Object[]{r.code(), r.title(), r.facultyName(), r.credits(), r.slot(),
                        r.seatsLeft(), r.myStatus() == null ? "" : r.myStatus()});
            }
            model.setRows(data);
        } catch (EnrollmentException | IllegalStateException e) {
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
            service.requestEnrollment(service.myStudentId(), course.courseId());
            UiUtil.info(this, "Request sent for " + course.code() + ".\nStatus: PENDING (waiting for admin approval).");
        } catch (EnrollmentException | IllegalStateException e) {
            UiUtil.error(this, e.getMessage());
        }
        refresh();
    }
}
