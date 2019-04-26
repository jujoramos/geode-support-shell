package org.apache.geode.support.service.tableExport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

class CsvExporter extends AbstractExporter {
  private final CSVFormat format;

  CSVFormat getFormat() {
    return format;
  }

  CsvExporter(CSVFormat format) {
    this.format = format;
  }

  @Override
  void export(Path path, Table table) throws IOException {
    TableModel tableModel = table.getModel();
    int rowCount = tableModel.getRowCount();

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      // It's a UI table so the first row should always be the header.
      CSVPrinter csvPrinter = new CSVPrinter(writer, format.withHeader(getRowContent(0, tableModel).toArray(new String[]{})));

      // Add Rows
      for (int row = 1; row < rowCount; row++) csvPrinter.printRecord(getRowContent(row, tableModel));

      csvPrinter.flush();
    }
  }
}
