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
package org.apache.geode.support.command.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
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

import org.apache.geode.support.test.SampleDataUtils;
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
public class ShowStatisticsMetadataCommandIntegrationTest {
  @Autowired
  public Shell shell;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

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
          String command = "show statistics metadata --path " + SampleDataUtils.rootFolder.getAbsolutePath();
          return () -> command;
        } else {
          return () -> "exit";
        }
      }
    });
  }

  @Test
  public void showStatisticsMetadataShouldGracefullyIntegrateWithSpringShell() {
    MethodTarget methodTarget = shell.listCommands().get("show statistics metadata");

    assertThat(methodTarget).isNotNull();
    assertThat(methodTarget.getAvailability().isAvailable()).isTrue();
    assertThat(methodTarget.getGroup()).isEqualTo("Statistics Commands");
    assertThat(methodTarget.getHelp()).isEqualTo("Show general information about statistics files.");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(
        ShowStatisticsMetadataCommand.class, "showStatisticsMetadata", File.class, ZoneId.class));
  }

  @Test
  public void showStatisticsMetadataShouldThrowExceptionWhenFileDoesNotExist() {
    String command = "show statistics metadata --path /temp/mock";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("File /temp/mock does not exist.");
  }

  @Test
  public void showStatisticsMetadataShouldReturnCorrectlyWhenNoFilesAreFound() {
    String command = "show statistics metadata --path " + temporaryFolder.getRoot().getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);

    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnOnlyMetadataTableIfParsingSucceedsForAllFiles(String timeZoneId) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";

    File basePath = SampleDataUtils.uncorruptedFolder;
    String command = "show statistics metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Correct Results.
    TableModel resultsTable = resultList.get(0).getModel();
    int rowCount = resultsTable.getRowCount();
    int columnCount = resultsTable.getColumnCount();
    assertThat(rowCount).isEqualTo(8);
    assertThat(columnCount).isEqualTo(6);

    // Assert Titles
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);

    // Output should be ordered by file name.
    SampleDataUtils.assertClusterOneLocatorMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(1, 0), (String) resultsTable.getValue(1, 1), (String) resultsTable.getValue(1, 2), (String) resultsTable.getValue(1, 3), (String) resultsTable.getValue(1, 4), (String) resultsTable.getValue(1, 5));
    SampleDataUtils.assertClusterOneServerOneMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(2, 0), (String) resultsTable.getValue(2, 1), (String) resultsTable.getValue(2, 2), (String) resultsTable.getValue(2, 3), (String) resultsTable.getValue(2, 4), (String) resultsTable.getValue(2, 5));
    SampleDataUtils.assertClusterOneServerTwoMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(3, 0), (String) resultsTable.getValue(3, 1), (String) resultsTable.getValue(3, 2), (String) resultsTable.getValue(3, 3), (String) resultsTable.getValue(3, 4), (String) resultsTable.getValue(3, 5));
    SampleDataUtils.assertClusterTwoLocatorMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(4, 0), (String) resultsTable.getValue(4, 1), (String) resultsTable.getValue(4, 2), (String) resultsTable.getValue(4, 3), (String) resultsTable.getValue(4, 4), (String) resultsTable.getValue(4, 5));
    SampleDataUtils.assertClusterTwoServerOneMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(5, 0), (String) resultsTable.getValue(5, 1), (String) resultsTable.getValue(5, 2), (String) resultsTable.getValue(5, 3), (String) resultsTable.getValue(5, 4), (String) resultsTable.getValue(5, 5));
    SampleDataUtils.assertClusterTwoServerTwoMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(6, 0), (String) resultsTable.getValue(6, 1), (String) resultsTable.getValue(6, 2), (String) resultsTable.getValue(6, 3), (String) resultsTable.getValue(6, 4), (String) resultsTable.getValue(6, 5));
    SampleDataUtils.assertClientMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(7, 0), (String) resultsTable.getValue(7, 1), (String) resultsTable.getValue(7, 2), (String) resultsTable.getValue(7, 3), (String) resultsTable.getValue(7, 4), (String) resultsTable.getValue(7, 5));
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnOnlyErrorsTableIfParsingFailsForAllFiles(String timeZoneId) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";

    File basePath = SampleDataUtils.corruptedFolder;
    String command = "show statistics metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Error Results should come last.
    TableModel errorsTable = resultList.get(0).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(3);
    assertThat(errorsColumnCount).isEqualTo(2);

    // Assert Table Data
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath.toPath()));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Unexpected token byte value: 67");
    assertThat(errorsTable.getValue(2, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath.toPath()));
    assertThat(errorsTable.getValue(2, 1)).isEqualTo("Not in GZIP format");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnErrorAndMetadataTablesInOrder(String timeZoneId) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    String zoneIdCommandOption = zoneId != null ? " --timeZone " + timeZoneId : "";

    File basePath = SampleDataUtils.rootFolder;
    String command = "show statistics metadata --path " + basePath.getAbsolutePath() + zoneIdCommandOption;
    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Correct Results should come first.
    TableModel resultsTable = resultList.get(0).getModel();
    int rowCount = resultsTable.getRowCount();
    int columnCount = resultsTable.getColumnCount();
    assertThat(rowCount).isEqualTo(8);
    assertThat(columnCount).isEqualTo(6);

    // Assert Titles
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);

    // Output should be ordered by file name.
    SampleDataUtils.assertClusterOneLocatorMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(1, 0), (String) resultsTable.getValue(1, 1), (String) resultsTable.getValue(1, 2), (String) resultsTable.getValue(1, 3), (String) resultsTable.getValue(1, 4), (String) resultsTable.getValue(1, 5));
    SampleDataUtils.assertClusterOneServerOneMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(2, 0), (String) resultsTable.getValue(2, 1), (String) resultsTable.getValue(2, 2), (String) resultsTable.getValue(2, 3), (String) resultsTable.getValue(2, 4), (String) resultsTable.getValue(2, 5));
    SampleDataUtils.assertClusterOneServerTwoMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(3, 0), (String) resultsTable.getValue(3, 1), (String) resultsTable.getValue(3, 2), (String) resultsTable.getValue(3, 3), (String) resultsTable.getValue(3, 4), (String) resultsTable.getValue(3, 5));
    SampleDataUtils.assertClusterTwoLocatorMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(4, 0), (String) resultsTable.getValue(4, 1), (String) resultsTable.getValue(4, 2), (String) resultsTable.getValue(4, 3), (String) resultsTable.getValue(4, 4), (String) resultsTable.getValue(4, 5));
    SampleDataUtils.assertClusterTwoServerOneMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(5, 0), (String) resultsTable.getValue(5, 1), (String) resultsTable.getValue(5, 2), (String) resultsTable.getValue(5, 3), (String) resultsTable.getValue(5, 4), (String) resultsTable.getValue(5, 5));
    SampleDataUtils.assertClusterTwoServerTwoMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(6, 0), (String) resultsTable.getValue(6, 1), (String) resultsTable.getValue(6, 2), (String) resultsTable.getValue(6, 3), (String) resultsTable.getValue(6, 4), (String) resultsTable.getValue(6, 5));
    SampleDataUtils.assertClientMetadata(basePath.toPath(), zoneId, (String) resultsTable.getValue(7, 0), (String) resultsTable.getValue(7, 1), (String) resultsTable.getValue(7, 2), (String) resultsTable.getValue(7, 3), (String) resultsTable.getValue(7, 4), (String) resultsTable.getValue(7, 5));

    // Error Results should come last.
    TableModel errorsTable = resultList.get(1).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(3);
    assertThat(errorsColumnCount).isEqualTo(2);

    // Assert Table Data
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath.toPath()));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Unexpected token byte value: 67");
    assertThat(errorsTable.getValue(2, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath.toPath()));
    assertThat(errorsTable.getValue(2, 1)).isEqualTo("Not in GZIP format");
  }
}
