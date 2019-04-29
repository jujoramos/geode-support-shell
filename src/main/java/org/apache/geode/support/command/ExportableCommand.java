package org.apache.geode.support.command;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.springframework.shell.table.Table;

import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.TableExportService;

public abstract class ExportableCommand extends AbstractCommand {
  private TableExportService tableExportService;
  protected final String EXPORT_OPTION = "--export";
  protected final String EXPORT_OPTION_HELP = "Path to file where command results should be written to (extension determines the output format: txt, csv, tsv).";

  public ExportableCommand(FilesService filesService, TableExportService tableExportService) {
    super(filesService);
    this.tableExportService = tableExportService;
  }

  TableExportService.Format getTargetFormatFromFileExtension(Path targetPath) {
    String fileExtension = FilenameUtils.getExtension(targetPath.toAbsolutePath().toString());

    if ("txt".equalsIgnoreCase(fileExtension)) return TableExportService.Format.TXT;
    if ("csv".equalsIgnoreCase(fileExtension)) return TableExportService.Format.CSV;
    if ("tsv".equalsIgnoreCase(fileExtension)) return TableExportService.Format.TSV;

    throw new IllegalArgumentException(String.format("No exporter found for extension %s", fileExtension));
  }

  protected void exportResultsTable(Table resultTable, File targetFile, List<Object> commandResult) {
    // Do nothing.
    if ((targetFile == null) || (resultTable == null)) return;

    // Use paths from now on.
    Path targetPath = targetFile.toPath();

    // Execute the export.
    try {
      TableExportService.Format targetFormat = getTargetFormatFromFileExtension(targetPath);
      tableExportService.export(targetPath, targetFormat, resultTable);
      commandResult.add(String.format("Data successfully exported to %s.", targetPath.toAbsolutePath().toString()));
    } catch (Exception exception) {
      commandResult.add(String.format("There was en error while exporting the data to %s: %s.", targetPath.toAbsolutePath().toString(), exception.getMessage()));
    }
  }
}
