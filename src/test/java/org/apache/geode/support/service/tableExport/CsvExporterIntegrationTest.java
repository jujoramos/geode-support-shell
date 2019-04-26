package org.apache.geode.support.service.tableExport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnitParamsRunner.class)
@PrepareForTest({ Files.class, CsvExporter.class })
public class CsvExporterIntegrationTest {
  private Table dummyTable;
  private CsvExporter csvExporter;

  @Rule
  public TestName testName = new TestName();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    PowerMockito.spy(Files.class);

    TableModelBuilder<String> modelBuilder = new TableModelBuilder<>();
    modelBuilder.addRow().addValue("Title1").addValue("Title2").addValue("Title3_ReallyLongForTestingPurposes").addValue("Title4");
    modelBuilder.addRow().addValue("Cell_1_1").addValue("Cell_1_2_ReallyLongForTestingPurposes").addValue("Cell_1_3").addValue("Cell_1_4");
    modelBuilder.addRow().addValue("Cell_2_1").addValue("Cell_2_2_ReallyLongForTestingPurposes").addValue("Cell_2_3").addValue("Cell_2_4");

    dummyTable = new TableBuilder(modelBuilder.build()).build();
  }

  private void assertRowContent(String line, int rowNumber, char separator) {
    if (rowNumber == 0) assertThat(line).isEqualTo("Title1" + separator + "Title2" + separator + "Title3_ReallyLongForTestingPurposes" + separator + "Title4");
    else assertThat(line).isEqualTo("Cell_" + rowNumber + "_1" + separator + "Cell_" + rowNumber + "_2_ReallyLongForTestingPurposes" + separator + "Cell_" + rowNumber + "_3" + separator + "Cell_" + rowNumber + "_4");
  }

  @Test
  @Parameters( { "TDF", "Default" })
  public void exportShouldPropagateExceptions(CSVFormat.Predefined format) throws Exception {
    PowerMockito.doThrow(new IOException("Dummy IOException")).when(Files.class);
    Files.newBufferedWriter(any());

    csvExporter = new CsvExporter(format.getFormat());
    assertThatThrownBy(() -> csvExporter.export(temporaryFolder.getRoot().toPath(), dummyTable)).isInstanceOf(IOException.class).hasMessage("Dummy IOException");
  }

  @Test
  @Parameters( { "TDF", "Default" })
  public void exportShouldWorkCorrectly(CSVFormat.Predefined format) throws IOException {
    csvExporter = new CsvExporter(format.getFormat());
    File csvFile = temporaryFolder.newFile(testName.getMethodName());
    csvExporter.export(csvFile.toPath(), dummyTable);

    // Check contents.
    int rowNumber = 0;
    List<String> lines = Files.readAllLines(csvFile.toPath());
    for (String line : lines) {
      assertRowContent(line, rowNumber, CSVFormat.DEFAULT.equals(format.getFormat()) ? ',' : '\t');
      rowNumber++;
    }

    // Assert that the file can be read programmatically.
    Reader reader = Files.newBufferedReader(csvFile.toPath());
    CSVParser csvParser = new CSVParser(reader, format.getFormat().withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
    List<CSVRecord> records = csvParser.getRecords();
    for (CSVRecord csvRecord : records) {
      long recordNumber = csvRecord.getRecordNumber();
      String value1 = csvRecord.get("Title1");
      String value2 = csvRecord.get("Title2");
      String value3 = csvRecord.get("Title3_ReallyLongForTestingPurposes");
      String value4 = csvRecord.get("Title4");

      assertThat(value1).isEqualTo("Cell_" + recordNumber + "_1");
      assertThat(value2).isEqualTo("Cell_" + recordNumber + "_2_ReallyLongForTestingPurposes");
      assertThat(value3).isEqualTo("Cell_" + recordNumber + "_3");
      assertThat(value4).isEqualTo("Cell_" + recordNumber + "_4");
    }
  }
}
