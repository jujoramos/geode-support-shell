package org.apache.geode.support.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.springframework.shell.table.Table;

import org.apache.geode.support.service.TableExportService;
import org.apache.geode.support.test.mockito.MockUtils;

public abstract class AbstractExportableCommandTest {
  protected File mockedExportFile;
  protected TableExportService exportService;
  private ExportableCommand exportableCommand;

  @Rule
  public TestName testName = new TestName();

  protected abstract ExportableCommand getCommand();

  @Before
  public void setUp() {
    exportableCommand = spy(getCommand());

    mockedExportFile = mock(File.class);
    Path mockedExportPath = mock(Path.class);
    when(mockedExportFile.toPath()).thenReturn(mockedExportPath);
    when(mockedExportPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedExportPath.toAbsolutePath().toString()).thenReturn("/export.txt");
  }

  protected void setExportServiceAnswer(boolean exportSucceeds) throws IOException {
    doAnswer(invocation -> {
      if (!exportSucceeds) throw new IOException("Mock IOException");
      return null;
    }).when(exportService).export(any(), any(), any());
  }

  protected void assertExportServiceResultMessageAndInvocation(List<Object> commandResult, boolean exportSucceeds) throws IOException {
    // Should always be last.
    String exportMessage = (String) commandResult.get(commandResult.size() - 1);

    if (exportSucceeds) {
      assertThat(exportMessage).isEqualTo("Data successfully exported to " + mockedExportFile.toPath().toAbsolutePath().toString() + ".");
    } else {
      assertThat(exportMessage).isEqualTo("There was en error while exporting the data to " + mockedExportFile.toPath().toAbsolutePath().toString() + ": Mock IOException.");
    }

    // Results Table always at the very beginning.
    verify(exportService, times(1)).export(mockedExportFile.toPath(), TableExportService.Format.TXT, (Table) commandResult.get(0));
  }

  @Test
  public void getTargetFormatFromFileExtensionShouldThrowExceptionForUnknownExtensions() {
    assertThatThrownBy(() -> exportableCommand.getTargetFormatFromFileExtension(Paths.get(testName.getMethodName() + ".pdf")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("No exporter found for extension pdf");
  }

  @Test
  public void getTargetFormatFromFileExtensionShouldReturnTheCorrectFormatForSupportedExtensions() {
    assertThat(exportableCommand.getTargetFormatFromFileExtension(Paths.get(testName.getMethodName() + ".txt"))).isEqualTo(TableExportService.Format.TXT);
    assertThat(exportableCommand.getTargetFormatFromFileExtension(Paths.get(testName.getMethodName() + ".csv"))).isEqualTo(TableExportService.Format.CSV);
    assertThat(exportableCommand.getTargetFormatFromFileExtension(Paths.get(testName.getMethodName() + ".tsv"))).isEqualTo(TableExportService.Format.TSV);
  }

  @Test
  public void exportResultsTableShouldDoNothingIfTargetFileIsNull() {
    exportableCommand.exportResultsTable(mock(Table.class), null, Collections.emptyList());
  }

  @Test
  public void exportResultsTableShouldDoNothingIfResultsTableIsNull() {
    exportableCommand.exportResultsTable(null, mock(File.class), Collections.emptyList());
  }

  @Test
  public void exportResultsTableShouldAddErrorMessageToResultIfFormatParsingFails() {
    List<Object> results = new ArrayList<>();
    Path targetFile = MockUtils.mockPath(testName.getMethodName() + ".pdf", false);

    exportableCommand.exportResultsTable(mock(Table.class), targetFile.toFile(), results);
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0)).isEqualTo("There was en error while exporting the data to " + testName.getMethodName() + ".pdf: No exporter found for extension pdf.");
  }

  @Test
  public void exportResultsTableShouldAddErrorMessageToResultIfExportFails() throws IOException {
    List<Object> results = new ArrayList<>();
    doThrow(new IOException("Mock IOException")).when(exportService).export(any(), any(), any());
    Path targetFile = MockUtils.mockPath(testName.getMethodName() + ".txt", false);

    exportableCommand.exportResultsTable(mock(Table.class), targetFile.toFile(), results);
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0)).isEqualTo("There was en error while exporting the data to " + testName.getMethodName() + ".txt: Mock IOException.");
  }

  @Test
  public void exportResultsTableShouldAddInformationMessageToResultIfExportSucceeds() {
    List<Object> results = new ArrayList<>();
    Path targetFile = MockUtils.mockPath(testName.getMethodName() + ".txt", false);

    exportableCommand.exportResultsTable(mock(Table.class), targetFile.toFile(), results);
    assertThat(results.size()).isEqualTo(1);
    assertThat(results.get(0)).isEqualTo("Data successfully exported to " + testName.getMethodName() + ".txt.");
  }
}
