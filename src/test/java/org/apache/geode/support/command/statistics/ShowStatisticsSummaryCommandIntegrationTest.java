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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.ReflectionUtils;

import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.test.SampleDataUtils;
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
    assertThat(methodTarget.getHelp()).isEqualTo("Shows Minimum, Maximum, Average, Last Value and Standard Deviation values for a (set of) defined statistics.");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(ShowStatisticsSummaryCommand.class, "showStatisticsSummary", File.class, ShowStatisticsSummaryCommand.GroupCriteria.class, Statistic.Filter.class, boolean.class, boolean.class, String.class, String.class));
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

    // Results Table.
    Table resultsTable = resultList.get(0);
    TableAssert.assertThat(resultsTable).rowCountIsEqualsTo(14).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultsTable).row(0).isEqualTo("VMStats.fdLimit", "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(1).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(2).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(3).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(4).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(5).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(6).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "10240.00", "10240.00", "10240.00", "10240.00", "0.00");
    TableAssert.assertThat(resultsTable).row(7).isEqualTo("VMStats.fdsOpen", "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(8).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "88.00", "165.00", "161.88", "162.00", "1.97");
    TableAssert.assertThat(resultsTable).row(9).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "91.00", "113.00", "112.76", "96.00", "1.08");
    TableAssert.assertThat(resultsTable).row(10).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "91.00", "114.00", "113.76", "99.00", "1.13");
    TableAssert.assertThat(resultsTable).row(11).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "61.00", "121.00", "118.13", "118.00", "1.66");
    TableAssert.assertThat(resultsTable).row(12).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "69.00", "88.00", "85.12", "85.00", "0.77");
    TableAssert.assertThat(resultsTable).row(13).isEqualTo("└──" + SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "69.00", "90.00", "86.11", "86.00", "0.78");
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
    Table errorsTable = resultList.get(0);
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsTable).row(1).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath), "Unexpected token byte value: 67");
    TableAssert.assertThat(errorsTable).row(2).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath), "Not in GZIP format");
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
    Table resultsTable = resultList.get(0);
    TableAssert.assertThat(resultsTable).rowCountIsEqualsTo(14).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultsTable).row(0).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(1).isEqualTo("└──StatSampler.delayDuration", "0.00", "1009.00", "999.91", "1003.00", "21.73");
    TableAssert.assertThat(resultsTable).row(2).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_SERVER1.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(3).isEqualTo("└──StatSampler.delayDuration", "0.00", "1009.00", "999.90", "999.00", "21.77");
    TableAssert.assertThat(resultsTable).row(4).isEqualTo(SampleDataUtils.SampleType.CLUSTER1_SERVER2.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(5).isEqualTo("└──StatSampler.delayDuration","0.00", "1010.00", "999.87", "1003.00", "21.77");
    TableAssert.assertThat(resultsTable).row(6).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(7).isEqualTo("└──StatSampler.delayDuration", "0.00", "1009.00", "999.92", "1003.00", "21.78");
    TableAssert.assertThat(resultsTable).row(8).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_SERVER1.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(9).isEqualTo("└──StatSampler.delayDuration", "0.00", "1009.00", "999.81", "1000.00", "21.80");
    TableAssert.assertThat(resultsTable).row(10).isEqualTo(SampleDataUtils.SampleType.CLUSTER2_SERVER2.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(11).isEqualTo("└──StatSampler.delayDuration", "0.00", "5249.00", "1002.25", "1000.00", "97.48");
    TableAssert.assertThat(resultsTable).row(12).isEqualTo(SampleDataUtils.SampleType.CLIENT.getRelativeFilePath(basePath), "Minimum", "Maximum", "Average", "Last Value", "Standard Deviation");
    TableAssert.assertThat(resultsTable).row(13).isEqualTo("└──StatSampler.delayDuration", "0.00", "1009.00", "999.77", "1000.00", "23.56");

    // Errors Table.
    Table errorsTable = resultList.get(1);
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsTable).row(1).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE.getRelativeFilePath(basePath), "Unexpected token byte value: 67");
    TableAssert.assertThat(errorsTable).row(2).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath), "Not in GZIP format");
  }
}
