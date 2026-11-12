package com.campus.cms.enrollment;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/** Read-only table model shared by the enrollment panels. */
class SimpleTableModel extends AbstractTableModel {
    private final String[] columns;
    private List<Object[]> rows = new ArrayList<>();

    SimpleTableModel(String... columns) {
        this.columns = columns;
    }

    void setRows(List<Object[]> newRows) {
        this.rows = newRows;
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return rows.size(); }

    @Override public int getColumnCount() { return columns.length; }

    @Override public String getColumnName(int column) { return columns[column]; }

    @Override public Object getValueAt(int row, int column) { return rows.get(row)[column]; }

    @Override public boolean isCellEditable(int row, int column) { return false; }

    /** Lets numeric columns (credits, seats left, CGPA) sort as numbers instead of text. */
    @Override
    public Class<?> getColumnClass(int column) {
        if (rows.isEmpty() || rows.get(0)[column] == null) {
            return Object.class;
        }
        return rows.get(0)[column].getClass();
    }
}
