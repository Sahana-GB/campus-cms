package com.campus.cms.reports;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes a ReportTable as a CSV file that opens correctly in Excel. */
public final class CsvExporter {
    private static final String BOM = "﻿";   // makes Excel read the file as UTF-8

    private CsvExporter() { }

    public static void export(ReportTable table, Path file) throws IOException {
        Files.writeString(file, BOM + toCsv(table), StandardCharsets.UTF_8);
    }

    public static String toCsv(ReportTable table) {
        StringBuilder sb = new StringBuilder();
        appendRow(sb, table.headers());
        for (List<Object> row : table.rows()) {
            appendRow(sb, row);
        }
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(values.get(i)));
        }
        sb.append("\r\n");
    }

    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        // Stop a cell from being run as a spreadsheet formula (e.g. a name starting with "=")
        if (value instanceof String && !s.isEmpty() && "=+@\t\r".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
