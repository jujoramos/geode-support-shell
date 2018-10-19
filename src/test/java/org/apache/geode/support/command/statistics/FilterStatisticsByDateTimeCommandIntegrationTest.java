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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;

import junitparams.JUnitParamsRunner;
import org.junit.Before;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.ReflectionUtils;

import org.apache.geode.support.test.StatisticsSampleDataUtils;
import org.apache.geode.support.test.assertj.TableAssert;

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
public class FilterStatisticsByDateTimeCommandIntegrationTest {
  @Autowired
  public Shell shell;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private File matchingFolder;
  private File nonMatchingFolder;

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  @Before
  public void setUp() throws IOException {
    matchingFolder = temporaryFolder.newFolder("matchingFiles");
    nonMatchingFolder = temporaryFolder.newFolder("nonMatchingFiles");
  }

  @Test
  public void shellIntegrationTest() throws IOException {
    shell.run(new InputProvider() {
      private boolean invoked = false;

      @Override
      public Input readInput() {
        if (!invoked) {
          invoked = true;
          String command = "filter statistics by date-time"
              + " --sourceFolder " + StatisticsSampleDataUtils.rootFolder.getAbsolutePath()
              + " --matchingFolder " + matchingFolder.getAbsolutePath()
              + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
              + " --year 2018 --month 3 --day 22";
          return () -> command;
        } else {
          return () -> "exit";
        }
      }
    });

    assertThat(Files.list(StatisticsSampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(StatisticsSampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
    assertThat(Files.list(matchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
    assertThat(Files.list(nonMatchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(0);
  }

  @Test
  public void filterStatisticsByDateTimeShouldGracefullyIntegrateWithSpringShell() {
    MethodTarget methodTarget = shell.listCommands().get("filter statistics by date-time");

    assertThat(methodTarget).isNotNull();
    assertThat(methodTarget.getAvailability().isAvailable()).isTrue();
    assertThat(methodTarget.getGroup()).isEqualTo("Statistics Commands");
    assertThat(methodTarget.getHelp()).isEqualTo("Scan the statistics files contained within the source folder and copy them to different folders, depending on whether they match the specified date time or not.");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(FilterStatisticsByDateTimeCommand.class, "filterStatisticsByDateTime", Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, Integer.class, File.class, File.class, File.class, ZoneId.class));
  }

  @Test
  public void filterStatisticsByDateTimeShouldThrowExceptionWhenSourceFolderDoesNotExist() {
    String command = "filter statistics by date-time"
        + " --sourceFolder /temp/mock"
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("Folder /temp/mock does not exist.");
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnCorrectlyWhenNoFilesAreFound() throws IOException {
    File emptyFolder = temporaryFolder.newFolder("emptyFolder");
    String command = "filter statistics by date-time"
        + " --sourceFolder " + emptyFolder.getAbsolutePath()
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnOnlyResultsTableIfParsingSucceedsForAllFiles() throws IOException {
    Path basePath = StatisticsSampleDataUtils.uncorruptedFolder.toPath();
    String command = "filter statistics by date-time"
        + " --sourceFolder " + basePath.toString()
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Results Table.
    Table resultTable = resultList.get(0);
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(8).columnCountIsEqualsTo(2);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Matches");
    TableAssert.assertThat(resultTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(3).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(4).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(5).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(6).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(7).isEqualTo(StatisticsSampleDataUtils.SampleType.CLIENT.getRelativeFilePath(basePath), "true");

    // Assert Copy of Files.
    assertThat(Files.list(basePath).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
    assertThat(Files.list(matchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
    assertThat(Files.list(nonMatchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(0);
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnOnlyErrorsTableIfParsingFailsForAllFiles() throws IOException {
    Path basePath = StatisticsSampleDataUtils.corruptedFolder.toPath();
    String command = "filter statistics by date-time"
        + " --sourceFolder " + basePath.toString()
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22 --hour 14";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Errors Table.
    Table resultTable = resultList.get(0);
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(resultTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath), "Unexpected token byte value: 67");
    TableAssert.assertThat(resultTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath), "Not in GZIP format");

    // Assert Copy of Files.
    assertThat(Files.list(basePath).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(matchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(0);
    assertThat(Files.list(nonMatchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(0);
  }

  @Test
  public void filterStatisticsByDateTimeShouldUseFileZoneIdForFilteringWhenNoCustomZoneIdIsSpecified() throws IOException {
    Path basePath = StatisticsSampleDataUtils.rootFolder.toPath();
    String command = "filter statistics by date-time"
        + " --sourceFolder " + basePath.toString()
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22 --hour 14";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Results Table should come first.
    Table resultTable = resultList.get(0);
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(8).columnCountIsEqualsTo(2);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Matches");
    TableAssert.assertThat(resultTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(3).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(4).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "false");
    TableAssert.assertThat(resultTable).row(5).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "false");
    TableAssert.assertThat(resultTable).row(6).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "false");
    TableAssert.assertThat(resultTable).row(7).isEqualTo(StatisticsSampleDataUtils.SampleType.CLIENT.getRelativeFilePath(basePath), "true");

    // Errors Table should come last.
    Table errorsTable = resultList.get(1);
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath), "Unexpected token byte value: 67");
    TableAssert.assertThat(errorsTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath), "Not in GZIP format");

    // Assert File Copy.
    assertThat(Files.list(matchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(4);
    assertThat(Files.list(nonMatchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(3);
    assertThat(Files.list(StatisticsSampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(StatisticsSampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
  }

  @Test
  public void filterStatisticsByDateTimeShouldUseCustomZoneIdForFilteringWhenSpecified() throws IOException {
    Path basePath = StatisticsSampleDataUtils.rootFolder.toPath();
    String command = "filter statistics by date-time"
        + " --sourceFolder " + basePath.toString()
        + " --matchingFolder " + matchingFolder.getAbsolutePath()
        + " --nonMatchingFolder " + nonMatchingFolder.getAbsolutePath()
        + " --year 2018 --month 3 --day 22 --hour 15 --minute 30 --timeZone Europe/Rome";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Results Table should come first.
    Table resultTable = resultList.get(0);
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(8).columnCountIsEqualsTo(2);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Matches");
    TableAssert.assertThat(resultTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(3).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(4).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(5).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(6).isEqualTo(StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "true");
    TableAssert.assertThat(resultTable).row(7).isEqualTo(StatisticsSampleDataUtils.SampleType.CLIENT.getRelativeFilePath(basePath), "true");

    // Errors Table should come last.
    Table errorsTable = resultList.get(1);
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsTable).row(1).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath), "Unexpected token byte value: 67");
    TableAssert.assertThat(errorsTable).row(2).isEqualTo(StatisticsSampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath), "Not in GZIP format");

    // Assert File Copy.
    assertThat(Files.list(matchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
    assertThat(Files.list(nonMatchingFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(0);
    assertThat(Files.list(StatisticsSampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(StatisticsSampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
  }
}
