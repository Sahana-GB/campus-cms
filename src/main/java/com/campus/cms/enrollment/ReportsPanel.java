package com.campus.cms.enrollment;

import com.campus.cms.core.ui.Styles;
import com.campus.cms.enrollment.EnrollmentDAO.CourseOption;
import com.campus.cms.reports.CsvExporter;
import com.campus.cms.reports.ReportTable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

/** Admin screen: view an enrollment report and export it as a CSV file. */
public class ReportsPanel extends JPanel {
    private static final String COUNTS = "Enrollment counts per course";
    private static final String ROSTER = "Course roster (students in one course)";

    private final EnrollmentService service;
    private final JComboBox<String> reportBox = new JComboBox<>(new String[]{COUNTS, ROSTER});
    private final JComboBox<CourseOption> courseBox = new JComboBox<>();
    private final JLabel courseLabel = new JLabel("Course:");
    private final JTable table = new JTable();
    private final JLabel summary = new JLabel(" ");
    private ReportTable current;
    private boolean loading;

    public ReportsPanel(EnrollmentService service) {
        this.service = service;

        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        Styles.table(table);

        JButton show = Styles.primary(new JButton("Show report"));
        show.addActionListener(e -> showReport());
        JButton export = Styles.secondary(new JButton("Export CSV"));
        export.addActionListener(e -> exportCsv());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Report:"));
        top.add(reportBox);
        top.add(courseLabel);
        top.add(courseBox);
        top.add(show);
        top.add(export);

        add(top, BorderLayout.NORTH);
        add(Styles.scroll(table), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);

        reportBox.addActionListener(e -> updateCourseVisibility());
        updateCourseVisibility();
        UiUtil.refreshWhenShown(this, this::refresh);
    }

    private boolean rosterSelected() {
        return ROSTER.equals(reportBox.getSelectedItem());
    }

    private void updateCourseVisibility() {
        courseLabel.setVisible(rosterSelected());
        courseBox.setVisible(rosterSelected());
    }

    /** Reload the course list and the current report. Runs every time the screen is shown. */
    public void refresh() {
        loading = true;
        try {
            CourseOption previous = (CourseOption) courseBox.getSelectedItem();
            courseBox.removeAllItems();
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
        showReport();
    }

    private void showReport() {
        if (loading) {
            return;
        }
        try {
            if (rosterSelected()) {
                CourseOption course = (CourseOption) courseBox.getSelectedItem();
                if (course == null) {
                    UiUtil.error(this, "Choose a course first.");
                    return;
                }
                current = service.courseRosterReport(course.courseId());
            } else {
                current = service.enrollmentCountsReport();
            }
            DefaultTableModel model = new DefaultTableModel(current.headers().toArray(), 0) {
                @Override public boolean isCellEditable(int row, int column) { return false; }
            };
            for (List<Object> row : current.rows()) {
                model.addRow(row.toArray());
            }
            table.setModel(model);
            summary.setText(current.title() + ": " + current.rows().size() + " row(s)");
        } catch (EnrollmentException e) {
            UiUtil.error(this, e.getMessage());
        }
    }

    private void exportCsv() {
        if (current == null) {
            UiUtil.error(this, "Show a report first.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files (*.csv)", "csv"));
        chooser.setSelectedFile(new File("report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getParentFile(), file.getName() + ".csv");
        }
        try {
            CsvExporter.export(current, file.toPath());
            UiUtil.info(this, "Saved " + current.rows().size() + " row(s) to " + file.getName());
        } catch (IOException e) {
            UiUtil.error(this, "Could not save the file: " + e.getMessage());
        }
    }
}
