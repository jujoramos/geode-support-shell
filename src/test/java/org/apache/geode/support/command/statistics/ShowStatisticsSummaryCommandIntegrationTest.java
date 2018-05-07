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
import java.nio.file.Path;
import java.util.List;

import junitparams.JUnitParamsRunner;
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

import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.test.SampleDataUtils;

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
public class ShowStatisticsSummaryCommandIntegrationTest {
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
          String command = "show statistics summary"
              + " --path " + SampleDataUtils.rootFolder.getAbsolutePath()
              + " --strictMatching false"
              + " --statistic .*InProgress";

          return () -> command;
        } else {
          return () -> "exit";
        }
      }
    });
  }

  @Test
  public void showStatisticsSummaryShouldGracefullyIntegrateWithSpringShell() {
    MethodTarget methodTarget = shell.listCommands().get("show statistics summary");

    assertThat(methodTarget).isNotNull();
    assertThat(methodTarget.getAvailability().isAvailable()).isTrue();
    assertThat(methodTarget.getGroup()).isEqualTo("Statistics Commands");
    assertThat(methodTarget.getHelp()).isEqualTo("Shows Minimum, Maximum, Average and Standard Deviation values for a (set of) defined statistics.");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(
        ShowStatisticsSummaryCommand.class, "showStatisticsSummary", File.class, ShowStatisticsSummaryCommand.GroupCriteria.class,
        Statistic.Filter.class, boolean.class, boolean.class, String.class, String.class));
  }

  @Test
  public void showStatisticsSummaryShouldThrowExceptionWhenCategoryIdAndStatisticIdAreBothEmpty() {
    Object commandResult = shell.evaluate(() -> "show statistics summary --path " + SampleDataUtils.rootFolder.getAbsolutePath());
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("Either '--category' or '--statistic' parameter should be specified.");
  }

  @Test
  public void showStatisticsSummaryShouldThrowExceptionWhenSourcePathDoesNotExist() {
    String command = "show statistics summary"
        + " --path /temp/mock"
        + " --category DistributionStats"
        + " --statistic replyWaitsInProgress";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("File /temp/mock does not exist.");
  }

  @Test
  public void showStatisticsSummaryShouldReturnCorrectlyWhenNoFilesAreFound() throws IOException {
    String command = "show statistics summary"
        + " --path " + temporaryFolder.newFolder("emptyFolder").getAbsolutePath()
        + " --category DistributionStats"
        + " --statistic replyWaitsInProgress";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  public void showStatisticsSummaryShouldReturnCorrectlyWhenNoMatchingStatisticsAreFound() throws IOException {
    String command = "show statistics summary"
        + " --path " + SampleDataUtils.uncorruptedFolder.toPath()
        + " --category NonExistingStatCategory"
        + " --statistic nonExistingStatistic";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No matching results found.");
  }

  @Test
  public void showStatisticsSummaryShouldReturnOnlyResultsTableIfParsingSucceedsForAllFiles() throws IOException {
    Path basePath = SampleDataUtils.uncorruptedFolder.toPath();
    String command = "show statistics summary"
        + " --path " + basePath.toString()
        + " --category VMStats"
        + " --statistic .*fd.*"
        + " --groupCriteria Statistic";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Correct Results.
    TableModel resultsTable = resultList.get(0).getModel();
    assertThat(resultsTable.getRowCount()).isEqualTo(14);
    assertThat(resultsTable.getColumnCount()).isEqualTo(6);

    // #### VMStats.fdLimit
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("VMStats.fdLimit");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(1, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(1, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(1, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(1, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(1, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(1, 5)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(2, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(2, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(2, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(2, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(2, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(2, 5)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(3, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(3, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(3, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(3, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(3, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(3, 5)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(4, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(4, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(4, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(4, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(4, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(4, 5)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(5, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(5, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(5, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(5, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(5, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(5, 5)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(6, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(6, 1)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(6, 2)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(6, 3)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(6, 4)).isEqualTo("10240.00");
    assertThat(resultsTable.getValue(6, 5)).isEqualTo("0.00");

    // #### VMStats.fdsOpen
    assertThat(resultsTable.getValue(7, 0)).isEqualTo("VMStats.fdsOpen");
    assertThat(resultsTable.getValue(7, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(7, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(7, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(7, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(7, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(8, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(8, 1)).isEqualTo("88.00");
    assertThat(resultsTable.getValue(8, 2)).isEqualTo("165.00");
    assertThat(resultsTable.getValue(8, 3)).isEqualTo("161.88");
    assertThat(resultsTable.getValue(8, 4)).isEqualTo("162.00");
    assertThat(resultsTable.getValue(8, 5)).isEqualTo("1.97");
    assertThat(resultsTable.getValue(9, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(9, 1)).isEqualTo("91.00");
    assertThat(resultsTable.getValue(9, 2)).isEqualTo("113.00");
    assertThat(resultsTable.getValue(9, 3)).isEqualTo("112.76");
    assertThat(resultsTable.getValue(9, 4)).isEqualTo("96.00");
    assertThat(resultsTable.getValue(9, 5)).isEqualTo("1.08");
    assertThat(resultsTable.getValue(10, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(10, 1)).isEqualTo("91.00");
    assertThat(resultsTable.getValue(10, 2)).isEqualTo("114.00");
    assertThat(resultsTable.getValue(10, 3)).isEqualTo("113.76");
    assertThat(resultsTable.getValue(10, 4)).isEqualTo("99.00");
    assertThat(resultsTable.getValue(10, 5)).isEqualTo("1.13");
    assertThat(resultsTable.getValue(11, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(11, 1)).isEqualTo("61.00");
    assertThat(resultsTable.getValue(11, 2)).isEqualTo("121.00");
    assertThat(resultsTable.getValue(11, 3)).isEqualTo("118.13");
    assertThat(resultsTable.getValue(11, 4)).isEqualTo("118.00");
    assertThat(resultsTable.getValue(11, 5)).isEqualTo("1.66");
    assertThat(resultsTable.getValue(12, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(12, 1)).isEqualTo("69.00");
    assertThat(resultsTable.getValue(12, 2)).isEqualTo("88.00");
    assertThat(resultsTable.getValue(12, 3)).isEqualTo("85.12");
    assertThat(resultsTable.getValue(12, 4)).isEqualTo("85.00");
    assertThat(resultsTable.getValue(12, 5)).isEqualTo("0.77");
    assertThat(resultsTable.getValue(13, 0)).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(13, 1)).isEqualTo("69.00");
    assertThat(resultsTable.getValue(13, 2)).isEqualTo("90.00");
    assertThat(resultsTable.getValue(13, 3)).isEqualTo("86.11");
    assertThat(resultsTable.getValue(13, 4)).isEqualTo("86.00");
    assertThat(resultsTable.getValue(13, 5)).isEqualTo("0.78");
  }

  @Test
  public void showStatisticsSummaryShouldReturnOnlyErrorsTableIfParsingFailsForAllFiles() throws IOException {
    Path basePath = SampleDataUtils.corruptedFolder.toPath();
    String command = "show statistics summary"
        + " --path " + basePath.toString()
        + " --category VMStats"
        + " --statistic .*fd.*"
        + " --groupCriteria Statistic";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);

    // Errors Table.
    TableModel errorsTable = resultList.get(0).getModel();
    assertThat(errorsTable.getRowCount()).isEqualTo(3);
    assertThat(errorsTable.getColumnCount()).isEqualTo(2);
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Unexpected token byte value: 67");
    assertThat(errorsTable.getValue(2, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath));
    assertThat(errorsTable.getValue(2, 1)).isEqualTo("Not in GZIP format");
  }

  @Test
  public void showStatisticsSummaryShouldReturnErrorsAndResultsTablesInOrder() {
    Path basePath = SampleDataUtils.rootFolder.toPath();
    String command = "show statistics summary"
        + " --path " + basePath.toString()
        + " --groupCriteria Sampling"
        + " --statistic delayDuration"
        + " --showEmptyStatistics true";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isInstanceOf(List.class);
    List<Table> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Results Table.
    TableModel resultsTable = resultList.get(0).getModel();
    assertThat(resultsTable.getRowCount()).isEqualTo(14);
    assertThat(resultsTable.getColumnCount()).isEqualTo(6);
    assertThat(resultsTable.getValue(0, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(1, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(1, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(1, 2)).isEqualTo("1009.00");
    assertThat(resultsTable.getValue(1, 3)).isEqualTo("999.91");
    assertThat(resultsTable.getValue(1, 4)).isEqualTo("1003.00");
    assertThat(resultsTable.getValue(1, 5)).isEqualTo("21.73");
    assertThat(resultsTable.getValue(2, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(2, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(2, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(2, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(2, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(2, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(3, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(3, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(3, 2)).isEqualTo("1009.00");
    assertThat(resultsTable.getValue(3, 3)).isEqualTo("999.90");
    assertThat(resultsTable.getValue(3, 4)).isEqualTo("999.00");
    assertThat(resultsTable.getValue(3, 5)).isEqualTo("21.77");
    assertThat(resultsTable.getValue(4, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(4, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(4, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(4, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(4, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(4, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(5, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(5, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(5, 2)).isEqualTo("1010.00");
    assertThat(resultsTable.getValue(5, 3)).isEqualTo("999.87");
    assertThat(resultsTable.getValue(5, 4)).isEqualTo("1003.00");
    assertThat(resultsTable.getValue(5, 5)).isEqualTo("21.77");
    assertThat(resultsTable.getValue(6, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(6, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(6, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(6, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(6, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(6, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(7, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(7, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(7, 2)).isEqualTo("1009.00");
    assertThat(resultsTable.getValue(7, 3)).isEqualTo("999.92");
    assertThat(resultsTable.getValue(7, 4)).isEqualTo("1003.00");
    assertThat(resultsTable.getValue(7, 5)).isEqualTo("21.78");
    assertThat(resultsTable.getValue(8, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(8, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(8, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(8, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(8, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(8, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(9, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(9, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(9, 2)).isEqualTo("1009.00");
    assertThat(resultsTable.getValue(9, 3)).isEqualTo("999.81");
    assertThat(resultsTable.getValue(9, 4)).isEqualTo("1000.00");
    assertThat(resultsTable.getValue(9, 5)).isEqualTo("21.80");
    assertThat(resultsTable.getValue(10, 0)).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(10, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(10, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(10, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(10, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(10, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(11, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(11, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(11, 2)).isEqualTo("5249.00");
    assertThat(resultsTable.getValue(11, 3)).isEqualTo("1002.25");
    assertThat(resultsTable.getValue(11, 4)).isEqualTo("1000.00");
    assertThat(resultsTable.getValue(11, 5)).isEqualTo("97.48");
    assertThat(resultsTable.getValue(12, 0)).isEqualTo(SampleDataUtils.SampleType.CLIENT.getRelativeFilePath(basePath));
    assertThat(resultsTable.getValue(12, 1)).isEqualTo("Minimum");
    assertThat(resultsTable.getValue(12, 2)).isEqualTo("Maximum");
    assertThat(resultsTable.getValue(12, 3)).isEqualTo("Average");
    assertThat(resultsTable.getValue(12, 4)).isEqualTo("Last Value");
    assertThat(resultsTable.getValue(12, 5)).isEqualTo("Standard Deviation");
    assertThat(resultsTable.getValue(13, 0)).isEqualTo("└──StatSampler.delayDuration");
    assertThat(resultsTable.getValue(13, 1)).isEqualTo("0.00");
    assertThat(resultsTable.getValue(13, 2)).isEqualTo("1009.00");
    assertThat(resultsTable.getValue(13, 3)).isEqualTo("999.77");
    assertThat(resultsTable.getValue(13, 4)).isEqualTo("1000.00");
    assertThat(resultsTable.getValue(13, 5)).isEqualTo("23.56");

    // Errors Table.
    TableModel errorsTable = resultList.get(1).getModel();
    assertThat(errorsTable.getRowCount()).isEqualTo(3);
    assertThat(errorsTable.getColumnCount()).isEqualTo(2);
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Unexpected token byte value: 67");
    assertThat(errorsTable.getValue(2, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath));
    assertThat(errorsTable.getValue(2, 1)).isEqualTo("Not in GZIP format");
  }
}
