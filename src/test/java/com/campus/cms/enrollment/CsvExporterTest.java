package com.campus.cms.enrollment;

import com.campus.cms.reports.CsvExporter;
import com.campus.cms.reports.ReportTable;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvExporterTest {

    private static ReportTable table(List<Object> row) {
        return new ReportTable("t", List.of("Name", "Marks"), List.of(row));
    }

    @Test
    void plainRow() {
        assertEquals("Name,Marks\r\nAsha,90\r\n", CsvExporter.toCsv(table(List.of("Asha", 90))));
    }

    @Test
    void commasAndQuotesAreEscaped() {
        assertEquals("Name,Marks\r\n\"Rao, K\",\"say \"\"hi\"\"\"\r\n",
                CsvExporter.toCsv(table(List.of("Rao, K", "say \"hi\""))));
    }

    @Test
    void nullBecomesEmptyCell() {
        assertEquals("Name,Marks\r\nAsha,\r\n", CsvExporter.toCsv(table(Arrays.asList("Asha", null))));
    }

    @Test
    void formulaLikeTextIsNeutralised() {
        assertEquals("Name,Marks\r\n'=SUM(A1),1\r\n", CsvExporter.toCsv(table(List.of("=SUM(A1)", 1))));
    }
}
