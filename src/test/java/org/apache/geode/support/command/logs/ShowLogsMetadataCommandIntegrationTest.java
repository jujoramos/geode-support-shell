/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.support.command.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.ReflectionUtils;

import org.apache.geode.support.service.TableExportService;
import org.apache.geode.support.test.LogsSampleDataUtils;
import org.apache.geode.support.test.junit.TimeZoneRule;
import org.apache.geode.support.utils.FormatUtils;

/**
 * Currently there's no easy way of doing proper integration tests with spring-boot + spring-shell.
 * See https://github.com/spring-projects/spring-shell/issues/204.
 */
@ActiveProfiles("test")
@RunWith(JUnitParamsRunner.class)
@SpringBootTest(properties = {
    ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT_ENABLED + "=false",
    InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
})
public class ShowLogsMetadataCommandIntegrationTest {
  @Autowired
  public Shell shell;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Rule
  public TimeZoneRule timeZoneRule = new TimeZoneRule(ZoneId.of("Europe/Dublin"));

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Test
  public void shellIntegrationTest() throws IOException {
    shell.run(new InputProvider() {
      private boolean invoked = false;

      @Override
      public Input readInput() {
        if (!invoked) {
          invoked = true;
          String command = "show logs metadata --path " + LogsSampleDataUtils.rootFolder.getAbsolutePath();
          return () -> command;
        } else {
          return () -> "exit";
        }
      }
    });
  }

  @Test
  public void showLogsMetadataShouldGracefullyIntegrateWithSpringShell() {
    MethodTarget methodTarget = shell.listCommands().get("show logs metadata");

    assertThat(methodTarget).isNotNull();
    assertThat(methodTarget.getAvailability().isAvailable()).isTrue();
    assertThat(methodTarget.getGroup()).isEqualTo("Logs Commands");
    assertThat(methodTarget.getHelp()).isEqualTo("Show general information about log files.");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(ShowLogsMetadataCommand.class, "showLogsMetadata", File.class, boolean.class, ZoneId.class, File.class));
  }

  @Test
  public void showLogsMetadataShouldThrowExceptionWhenFileDoesNotExist() {
    String command = "show logs metadata --path /temp/mock";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("File /temp/mock does not exist.");
  }

  @Test
  public void showLogsMetadataShouldReturnCorrectlyWhenNoFilesAreFound() {
    String command = "show logs metadata --path " + temporaryFolder.getRoot().getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No log files found.");
  }

  @Test
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai, false" })
  public void showLogsMetadataShouldReturnOnlyErrorsTableWhenParsingFailsForAllFiles(String timeZoneId, boolean intervalOnly) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";
    String intervalOnlyCommandOption = " --intervalOnly " + intervalOnly;

    File basePath = LogsSampleDataUtils.unparseableFolder;
    String command = "show logs metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption + intervalOnlyCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Error Table.
    TableModel errorsTable = resultList.get(0).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(2);
    assertThat(errorsColumnCount).isEqualTo(2);

    // Assert Table Data.
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(FormatUtils.relativizePath(basePath.toPath(), LogsSampleDataUtils.unknownLogPath));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Log format not recognized.");
  }

  @Test
  @Parameters({ "txt", "pdf", "csv", "tsv" })
  public void showLogsMetadataShouldReturnOnlyErrorsTableAndIgnoreExportParameterWhenParsingFailsForAllFiles(String format) {
    File basePath = LogsSampleDataUtils.unparseableFolder;
    String outputFile = temporaryFolder.getRoot().getAbsolutePath() + File.separator + "output." + format;
    String exportCommandOption = " --export " + outputFile;

    String command = "show logs metadata --path " + basePath.getAbsolutePath() + exportCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> resultList = (List<Object>) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isInstanceOf(Table.class);

    // File Should Not Exist.
    assertThat(Files.exists(Paths.get(outputFile))).isFalse();
  }

  @Test
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai, false" })
  public void showLogsMetadataShouldReturnOnlyMetadataTableWhenParsingSucceedsForAllFiles(String timeZoneId, boolean intervalOnly) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";
    String intervalOnlyCommandOption = " --intervalOnly " + intervalOnly;

    File basePath = LogsSampleDataUtils.parseableFolder;
    String command = "show logs metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption + intervalOnlyCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Results Table.
    TableModel resultsTable = resultList.get(0).getModel();
    int rowCount = resultsTable.getRowCount();
    int columnCount = resultsTable.getColumnCount();
    assertThat(rowCount).isEqualTo(4);
    assertThat(columnCount).isEqualTo(6);

    // Assert Titles.
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);

    // Output should be ordered by file name.
    LogsSampleDataUtils.assertMember8XMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(1, 0), (String) resultsTable.getValue(1, 1), (String) resultsTable.getValue(1, 2), (String) resultsTable.getValue(1, 3), (String) resultsTable.getValue(1, 4), (String) resultsTable.getValue(1, 5), intervalOnly);
    LogsSampleDataUtils.assertMember9XMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(2, 0), (String) resultsTable.getValue(2, 1), (String) resultsTable.getValue(2, 2), (String) resultsTable.getValue(2, 3), (String) resultsTable.getValue(2, 4), (String) resultsTable.getValue(2, 5), intervalOnly);
    LogsSampleDataUtils.assertNoHeaderMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(3, 0), (String) resultsTable.getValue(3, 1), (String) resultsTable.getValue(3, 2), (String) resultsTable.getValue(3, 3), (String) resultsTable.getValue(3, 4), (String) resultsTable.getValue(3, 5));
  }

  @Test
  @Parameters({ "txt", "pdf", "csv", "tsv" })
  public void showLogsMetadataShouldReturnMetadataTableAndExportResultWhenParsingSucceedsForAllFiles(String format) {
    File basePath = LogsSampleDataUtils.parseableFolder;
    String outputFile = temporaryFolder.getRoot().getAbsolutePath() + File.separator + "output." + format;
    String exportCommandOption = " --export " + outputFile;

    String command = "show logs metadata --path " + basePath.getAbsolutePath() + exportCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> resultList = (List<Object>) commandResult;
    assertThat(resultList.size()).isEqualTo(2);
    assertThat(resultList.get(0)).isInstanceOf(Table.class);
    assertThat(resultList.get(1)).isInstanceOf(String.class);

    // File Should (Not) Exist for (Un) Known Formats.
    if (TableExportService.Format.isSupported(format)) {
      assertThat(Files.exists(Paths.get(outputFile))).isTrue();
      assertThat(resultList.get(1)).isInstanceOf(String.class).isEqualTo("Data successfully exported to " + outputFile + ".");
    } else {
      assertThat(Files.exists(Paths.get(outputFile))).isFalse();
      assertThat(resultList.get(1)).isInstanceOf(String.class).isEqualTo("There was en error while exporting the data to " + outputFile + ": No exporter found for extension " + format + ".");
    }
  }

  @Test
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai, false" })
  public void showLogsMetadataShouldReturnErrorAndMetadataTablesInOrder(String timeZoneId, boolean intervalOnly) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";
    String intervalOnlyCommandOption = " --intervalOnly " + intervalOnly;

    File basePath = LogsSampleDataUtils.rootFolder;
    String command = "show logs metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption + intervalOnlyCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Correct Results should come first.
    TableModel resultsTable = resultList.get(0).getModel();
    int rowCount = resultsTable.getRowCount();
    int columnCount = resultsTable.getColumnCount();
    assertThat(rowCount).isEqualTo(4);
    assertThat(columnCount).isEqualTo(6);

    // Assert Titles
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);

    // Output should be ordered by file name.
    LogsSampleDataUtils.assertMember8XMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(1, 0), (String) resultsTable.getValue(1, 1), (String) resultsTable.getValue(1, 2), (String) resultsTable.getValue(1, 3), (String) resultsTable.getValue(1, 4), (String) resultsTable.getValue(1, 5), intervalOnly);
    LogsSampleDataUtils.assertMember9XMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(2, 0), (String) resultsTable.getValue(2, 1), (String) resultsTable.getValue(2, 2), (String) resultsTable.getValue(2, 3), (String) resultsTable.getValue(2, 4), (String) resultsTable.getValue(2, 5), intervalOnly);
    LogsSampleDataUtils.assertNoHeaderMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(3, 0), (String) resultsTable.getValue(3, 1), (String) resultsTable.getValue(3, 2), (String) resultsTable.getValue(3, 3), (String) resultsTable.getValue(3, 4), (String) resultsTable.getValue(3, 5));

    // Error Results should come last.
    TableModel errorsTable = resultList.get(1).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(2);
    assertThat(errorsColumnCount).isEqualTo(2);

    // Assert Table Data
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(FormatUtils.relativizePath(basePath.toPath(), LogsSampleDataUtils.unknownLogPath));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Log format not recognized.");
  }

  @Test
  @Parameters({ "txt", "pdf", "csv", "tsv" })
  public void showLogsMetadataShouldReturnBothTablesAndExportMessageInOrder(String format) {
    File basePath = LogsSampleDataUtils.rootFolder;
    String outputFile = temporaryFolder.getRoot().getAbsolutePath() + File.separator + "output." + format;
    String exportCommandOption = " --export " + outputFile;

    String command = "show logs metadata --path " + basePath.getAbsolutePath() + exportCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> resultList = (List<Object>) commandResult;
    assertThat(resultList.size()).isEqualTo(3);
    assertThat(resultList.get(0)).isInstanceOf(Table.class);
    assertThat(resultList.get(1)).isInstanceOf(Table.class);
    assertThat(resultList.get(2)).isInstanceOf(String.class);

    // File Should (Not) Exist for (Un) Known Formats.
    if (TableExportService.Format.isSupported(format)) {
      assertThat(Files.exists(Paths.get(outputFile))).isTrue();
      assertThat(resultList.get(2)).isInstanceOf(String.class).isEqualTo("Data successfully exported to " + outputFile + ".");
    } else {
      assertThat(Files.exists(Paths.get(outputFile))).isFalse();
      assertThat(resultList.get(2)).isInstanceOf(String.class).isEqualTo("There was en error while exporting the data to " + outputFile + ": No exporter found for extension " + format + ".");
    }
  }
}
