package org.apache.geode.support.service.tableExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnitParamsRunner.class)
@PrepareForTest({ Files.class, TextExporter.class })
public class TextExporterIntegrationTest {
  private Table dummyTable;
  private TextExporter textExporter;

  @Rule
  public TestName testName = new TestName();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    PowerMockito.spy(Files.class);

    TableModelBuilder<String> modelBuilder = new TableModelBuilder<>();
    modelBuilder.addRow().addValue("Title1").addValue("Title2_ReallyLongForTestingPurposes");
    modelBuilder.addRow().addValue("Cell_1_1_ReallyLongForTestingPurposes").addValue("Cell_1_2");
    modelBuilder.addRow().addValue("Cell_2_1").addValue("Cell_2_2_ReallyLongForTestingPurposes");

    dummyTable = new TableBuilder(modelBuilder.build()).addFullBorder(BorderStyle.fancy_double).build();
  }

  @Test
  public void exportShouldPropagateExceptions() throws Exception {
    PowerMockito.doThrow(new IOException("Dummy IOException")).when(Files.class);
    Files.newBufferedWriter(any());

    textExporter = new TextExporter();
    assertThatThrownBy(() -> textExporter.export(temporaryFolder.getRoot().toPath(), dummyTable)).isInstanceOf(IOException.class).hasMessage("Dummy IOException");
  }

  @Test
  public void exportShouldWorkCorrectly() throws IOException {
    textExporter = new TextExporter();
    File csvFile = temporaryFolder.newFile(testName.getMethodName());
    textExporter.export(csvFile.toPath(), dummyTable);

    // Check contents.
    List<String> lines = Files.readAllLines(csvFile.toPath());
    assertThat(lines.size()).isEqualTo(7);
    assertThat(lines.get(0)).isEqualTo("╔═════════════════════════════════════╦═════════════════════════════════════╗");
    assertThat(lines.get(1)).isEqualTo("║Title1                               ║Title2_ReallyLongForTestingPurposes  ║");
    assertThat(lines.get(2)).isEqualTo("╠═════════════════════════════════════╬═════════════════════════════════════╣");
    assertThat(lines.get(3)).isEqualTo("║Cell_1_1_ReallyLongForTestingPurposes║Cell_1_2                             ║");
    assertThat(lines.get(4)).isEqualTo("╠═════════════════════════════════════╬═════════════════════════════════════╣");
    assertThat(lines.get(5)).isEqualTo("║Cell_2_1                             ║Cell_2_2_ReallyLongForTestingPurposes║");
    assertThat(lines.get(6)).isEqualTo("╚═════════════════════════════════════╩═════════════════════════════════════╝");
  }
}
