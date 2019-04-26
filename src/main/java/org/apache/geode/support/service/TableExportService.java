package org.apache.geode.support.service;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.shell.table.Table;

public interface TableExportService {
  enum Format {
    TXT,
    CSV,
    TSV
  }

  /**
   * Exports the table to the specific file using the selected format.
   *
   * @param file File to which the table should be exported.
   * @param format Format which will be used to store the table.
   * @param table The actual data that should be written to the file using the selected format.
   * @throws IOException If an exception occurs while writing the results to disk.
   */
  public void export(Path file, Format format, Table table) throws IOException;
}
