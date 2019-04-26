package org.apache.geode.support.service.tableExport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

abstract class AbstractExporter {

  /**
   * Gets an entire row from a tableModel.
   *
   * @param row Row to extract from the table.
   * @param tableModel Table with the data to extract.
   * @return The requested row from the table.
   */
  List<String> getRowContent(int row, TableModel tableModel) {
    int columnCount = tableModel.getColumnCount();
    List<String> rowContent = new ArrayList<>();
    for (int column = 0; column < columnCount; column++) rowContent.add(tableModel.getValue(row, column) + "");

    return rowContent;
  }

  abstract void export(Path file, Table table) throws IOException;
}
