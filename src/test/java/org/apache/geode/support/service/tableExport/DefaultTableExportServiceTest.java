package org.apache.geode.support.service.tableExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.shell.table.Table;

import org.apache.geode.support.service.TableExportService;

public class DefaultTableExportServiceTest {
  private DefaultTableExportService tableExportService;

  @Before
  public void setUp() {
    tableExportService = spy(new DefaultTableExportService());
  }

  @Test
  public void getExporterFromFormatShouldInstantiateTheCorrectExporter() {
    AbstractExporter textExporter = tableExportService.getExporterFromFormat(TableExportService.Format.TXT);
    assertThat(textExporter).isInstanceOf(TextExporter.class);

    AbstractExporter tsvExporter = tableExportService.getExporterFromFormat(TableExportService.Format.TSV);
    assertThat(tsvExporter).isInstanceOf(CsvExporter.class);
    assertThat(((CsvExporter) tsvExporter).getFormat()).isEqualTo(CSVFormat.TDF);

    AbstractExporter csvExporter = tableExportService.getExporterFromFormat(TableExportService.Format.CSV);
    assertThat(csvExporter).isInstanceOf(CsvExporter.class);
    assertThat(((CsvExporter) csvExporter).getFormat()).isEqualTo(CSVFormat.DEFAULT);
  }

  @Test
  public void exportShouldThrowNullPointerExceptionWhenAnyParameterIsNull() {
    assertThatThrownBy(() -> tableExportService.export(null, TableExportService.Format.CSV, mock(Table.class))).isInstanceOf(NullPointerException.class).hasMessageMatching("Path can not be null.");
    assertThatThrownBy(() -> tableExportService.export(mock(Path.class), null, mock(Table.class))).isInstanceOf(NullPointerException.class).hasMessageMatching("Format can not be null.");
    assertThatThrownBy(() -> tableExportService.export(mock(Path.class), TableExportService.Format.TSV, null)).isInstanceOf(NullPointerException.class).hasMessageMatching("Table can not be null.");
  }

  @Test
  public void exportShouldWorkProperly() {
    AbstractExporter mockExporter = mock(AbstractExporter.class);
    doReturn(mockExporter).when(tableExportService).getExporterFromFormat(any());

    assertThatCode(() -> tableExportService.export(mock(Path.class), TableExportService.Format.CSV, mock(Table.class))).doesNotThrowAnyException();
  }
}
