package com.campus.cms.reports;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A report as plain data: a title, column headers and rows.
 * Person A's report screen can show it in a JTable; CsvExporter can write it to a file.
 */
public record ReportTable(String title, List<String> headers, List<List<Object>> rows) {

    /** Builds a table from a query result. Column headers are the SQL column aliases. */
    public static ReportTable from(String title, ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<String> headers = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            headers.add(md.getColumnLabel(i));
        }
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(n);
            for (int i = 1; i <= n; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
        }
        return new ReportTable(title, headers, rows);
    }
}
