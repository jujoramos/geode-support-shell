package org.apache.geode.support.service.tableExport;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.table.Table;
import org.springframework.stereotype.Service;

import org.apache.geode.support.service.TableExportService;

@Service
class DefaultTableExportService implements TableExportService {
  private static final Logger logger = LoggerFactory.getLogger(DefaultTableExportService.class);

  AbstractExporter getExporterFromFormat(Format format) {
    switch (format) {
      case TXT: return new TextExporter();
      case TSV: return new CsvExporter(CSVFormat.TDF);
      case CSV: return new CsvExporter(CSVFormat.DEFAULT);

      default: throw new IllegalArgumentException(String.format("Format %s not recognized", format));
    }
  }

  @Override
  public void export(Path path, Format format, Table table) throws IOException  {
    Objects.requireNonNull(path, "Path can not be null.");
    Objects.requireNonNull(format, "Format can not be null.");
    Objects.requireNonNull(table, "Table can not be null.");

    logger.debug(String.format("Exporting data to file %s using format %s...", path.toString(), format));
    AbstractExporter exporter = getExporterFromFormat(format);
    exporter.export(path, table);
    logger.debug(String.format("Exporting data to file %s using format %s... Done!.", path.toString(), format));
  }
}
