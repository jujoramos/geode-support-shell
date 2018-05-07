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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import org.apache.geode.internal.statistics.StatValue;
import org.apache.geode.internal.statistics.ValueFilter;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.Category;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.domain.statistics.filters.RegexValueFilter;
import org.apache.geode.support.domain.statistics.filters.SimpleValueFilter;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.test.MockUtils;

@RunWith(JUnitParamsRunner.class)
public class ShowStatisticsSummaryCommandTest {
  private Path mockedRootPath;
  private FilesService filesService;
  private StatisticsService statisticsService;
  private ShowStatisticsSummaryCommand showStatisticsSummaryCommand;

  @Before
  public void setUp() {
    mockedRootPath = MockUtils.mockPath("/samples", true);

    filesService = mock(FilesService.class);
    statisticsService = mock(StatisticsService.class);
    showStatisticsSummaryCommand = spy(new ShowStatisticsSummaryCommand(filesService, statisticsService));
  }

  @Test
  public void buildTableGroupedBySamplingShouldIterateOverSuccessfulParsingResultsOnly() {
    ParsingResult result = mock(ParsingResult.class);
    when(result.isSuccess()).thenReturn(false);
    Table emptyTableResult = showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, null, Collections.singletonList(result));
    assertThat(emptyTableResult).isNull();
  }

  @Test
  public void buildTableGroupedByStatisticShouldIterateOverSuccessfulParsingResultsOnly() {
    ParsingResult result = mock(ParsingResult.class);
    when(result.isSuccess()).thenReturn(false);
    Table emptyTableResult = showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, null, Collections.singletonList(result));
    assertThat(emptyTableResult).isNull();
  }

  @Test
  public void buildTableGroupedBySamplingShouldReturnNullWhenNoDataIsEffectivelyAddedToTheTable() {
    // No Parsing Results.
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, true, null, Collections.emptyList())).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, null, Collections.emptyList())).isNull();

    // No Categories.
    Path mockedFile = MockUtils.mockPath("/samples/file.gfs", false);
    Sampling mockedSampling = new Sampling(mock(SamplingMetadata.class), new HashMap<>());
    ParsingResult<Sampling> mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, true, null, Collections.singletonList(mockedParsingResult))).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();

    // No Statistics.
    Map<String, Category> categoryMap = new HashMap<>();
    Category mockedCategory = new Category("MockedName", "MockedDescription");
    categoryMap.put(mockedCategory.getName(), mockedCategory);
    mockedSampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, true, null, Collections.singletonList(mockedParsingResult))).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();

    // Statistics with Zero Values only
    Statistic mockedStatistic = mock(Statistic.class);
    when(mockedStatistic.isEmpty()).thenReturn(true);
    mockedCategory.addStatistic(mockedStatistic);
    categoryMap.put(mockedCategory.getName(), mockedCategory);
    mockedSampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();
  }

  @Test
  public void buildTableGroupedByStatisticShouldReturnNullWhenNoDataIsEffectivelyAddedToTheTable() {
    // No Parsing Results.
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, true, null, Collections.emptyList())).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, null, Collections.emptyList())).isNull();

    // No Categories.
    Path mockedFile = MockUtils.mockPath("/samples/file.gfs", false);
    Sampling mockedSampling = new Sampling(mock(SamplingMetadata.class), new HashMap<>());
    ParsingResult<Sampling> mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, true, null, Collections.singletonList(mockedParsingResult))).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();

    // No Statistics.
    Map<String, Category> categoryMap = new HashMap<>();
    Category mockedCategory = new Category("MockedName", "MockedDescription");
    categoryMap.put(mockedCategory.getName(), mockedCategory);
    mockedSampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, true, null, Collections.singletonList(mockedParsingResult))).isNull();
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();

    // Statistics with Zero Values only
    Statistic mockedStatistic = mock(Statistic.class);
    when(mockedStatistic.isEmpty()).thenReturn(true);
    mockedCategory.addStatistic(mockedStatistic);
    categoryMap.put(mockedCategory.getName(), mockedCategory);
    mockedSampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    mockedParsingResult = new ParsingResult<>(mockedFile, mockedSampling);
    assertThat(showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, null, Collections.singletonList(mockedParsingResult))).isNull();
  }

  @Test
  public void buildTableGroupedBySamplingShouldReturnResultsInOrderAndIgnoreEmptySamplingsWhenConfigured() {
    StatValue tokens = MockUtils.mockStatValue("tokens", "tokens", true, "tokens", 0, 0, 0, 0, 0);
    StatValue serversStatValue = MockUtils.mockStatValue("servers", "servers", true, "servers", 2, 2, 2, 2, 0);
    StatValue jvmPausesStatValue = MockUtils.mockStatValue("jvmPauses", "jvmPauses", false, "jvmPauses", 0, 0, 0, 0, 0);
    StatValue delayDurationStatValue = MockUtils.mockStatValue("delayDuration", "delayDuration", false, "delayDuration", 0, 10, 5, 10, 0);
    StatValue replyWaitsInProgressStatValue = MockUtils.mockStatValue("replyWaitsInProgress", "replyWaitsInProgress", true, "replyWaitsInProgress", 2, 8, 0.67, 0, 0);

    Statistic tokensStatistic = spy(new Statistic(tokens));
    Statistic serversStatistic = spy(new Statistic(serversStatValue));
    Statistic jvmPausesStatistic = spy(new Statistic(jvmPausesStatValue));
    Statistic delayDurationStatistic = spy(new Statistic(delayDurationStatValue));
    Statistic replyWaitsInProgressStatistic = spy(new Statistic(replyWaitsInProgressStatValue));

    Category vmStatsCategory = new Category("StatsSampler", "VMStatsCategory");
    vmStatsCategory.addStatistic(jvmPausesStatistic);
    vmStatsCategory.addStatistic(delayDurationStatistic);

    Category distributionStatsCategory = new Category("DistributionStats", "DistributionStatsCategory");
    distributionStatsCategory.addStatistic(replyWaitsInProgressStatistic);

    Category poolStatsCategory = new Category("PoolStats", "PoolStatsCategory");
    poolStatsCategory.addStatistic(serversStatistic);

    Category dlocksStatsCategory = new Category("DLockStats", "DLockStatsCategory");
    dlocksStatsCategory.addStatistic(tokensStatistic);

    Map<String, Category> clientCategories = new HashMap<>();
    clientCategories.put(poolStatsCategory.getName(), poolStatsCategory);

    Map<String, Category> serverCategories = new HashMap<>();
    serverCategories.put(vmStatsCategory.getName(), vmStatsCategory);
    serverCategories.put(distributionStatsCategory.getName(), distributionStatsCategory);

    Map<String, Category> locatorCategories = new HashMap<>();
    locatorCategories.put(dlocksStatsCategory.getName(), dlocksStatsCategory);

    Path mockedClientFile = MockUtils.mockPath("/samples/client.gfs", false);
    Path mockedServerFile = MockUtils.mockPath("/samples/server.gfs", false);
    Path mockedLocatorFile = MockUtils.mockPath("/samples/locator.gz", false);
    Sampling clientSampling = new Sampling(mock(SamplingMetadata.class), clientCategories);
    Sampling serverSampling = new Sampling(mock(SamplingMetadata.class), serverCategories);
    Sampling locatorSampling = new Sampling(mock(SamplingMetadata.class), locatorCategories);

    ParsingResult clientResult = new ParsingResult<>(mockedClientFile, clientSampling);
    ParsingResult serverResult = new ParsingResult<>(mockedServerFile, serverSampling);
    ParsingResult locatorResult = new ParsingResult<>(mockedLocatorFile, locatorSampling);

    List<ParsingResult<Sampling>> parsingResults = Arrays.asList(clientResult, serverResult, locatorResult);
    Table resultTable = null;
    TableModel resultTableModel = null;

    // ############ includeEmptyStatistics = false
    resultTable = showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, false, Statistic.Filter.None, parsingResults);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel).isNotNull();
    assertThat(resultTableModel.getRowCount()).isEqualTo(5);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    verify(serversStatistic, times(1)).setFilter(Statistic.Filter.None);
    verify(delayDurationStatistic, times(1)).setFilter(Statistic.Filter.None);
    verify(replyWaitsInProgressStatistic, times(1)).setFilter(Statistic.Filter.None);

    // Client Data
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("/client.gfs");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──PoolStats.servers");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");

    // Server Data
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("/server.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(2, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(2, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(2, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(2, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("└──DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(3, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(3, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(3, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(3, 5)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 0)).isEqualTo("└──StatsSampler.delayDuration");
    assertThat(resultTableModel.getValue(4, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 2)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(4, 3)).isEqualTo("5.00");
    assertThat(resultTableModel.getValue(4, 4)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(4, 5)).isEqualTo("0.00");

    // ############ includeEmptyStatistics = true
    resultTable = showStatisticsSummaryCommand.buildTableGroupedBySampling(mockedRootPath, true, Statistic.Filter.Sample, parsingResults);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel).isNotNull();
    assertThat(resultTableModel.getRowCount()).isEqualTo(8);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    verify(tokensStatistic, times(1)).setFilter(Statistic.Filter.Sample);
    verify(serversStatistic, times(1)).setFilter(Statistic.Filter.Sample);
    verify(jvmPausesStatistic, times(1)).setFilter(Statistic.Filter.Sample);
    verify(delayDurationStatistic, times(1)).setFilter(Statistic.Filter.Sample);
    verify(replyWaitsInProgressStatistic, times(1)).setFilter(Statistic.Filter.Sample);

    // Client Data
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("/client.gfs");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──PoolStats.servers");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");

    // Server Data
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("/server.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(2, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(2, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(2, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(2, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("└──DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(3, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(3, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(3, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(3, 5)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 0)).isEqualTo("└──StatsSampler.delayDuration");
    assertThat(resultTableModel.getValue(4, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 2)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(4, 3)).isEqualTo("5.00");
    assertThat(resultTableModel.getValue(4, 4)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(4, 5)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(5, 0)).isEqualTo("└──StatsSampler.jvmPauses");
    assertThat(resultTableModel.getValue(5, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(5, 2)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(5, 3)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(5, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(5, 5)).isEqualTo("0.00");

    // Locator Data
    assertThat(resultTableModel.getValue(6, 0)).isEqualTo("/locator.gz");
    assertThat(resultTableModel.getValue(6, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(6, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(6, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(6, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(6, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(7, 0)).isEqualTo("└──DLockStats.tokens");
    assertThat(resultTableModel.getValue(7, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(7, 2)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(7, 3)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(7, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(7, 5)).isEqualTo("0.00");
  }

  @Test
  public void buildTableGroupedByStatisticShouldReturnResultsInOrderAndIgnoreEmptySamplingsWhenConfigured() {
    StatValue tokens = MockUtils.mockStatValue("tokens", "tokens", true, "tokens", 0, 0, 0, 0, 0);
    StatValue serversStatValue = MockUtils.mockStatValue("servers", "servers", true, "servers", 2, 2, 2, 2, 0);
    StatValue jvmPausesStatValue = MockUtils.mockStatValue("jvmPauses", "jvmPauses", false, "jvmPauses", 0, 0, 0, 0, 0);
    StatValue delayDurationStatValue = MockUtils.mockStatValue("delayDuration", "delayDuration", false, "delayDuration", 0, 10, 5, 10, 0);

    StatValue replyWaitsInProgressStatValueServer = MockUtils.mockStatValue("replyWaitsInProgress", "replyWaitsInProgress", true, "replyWaitsInProgress", 0, 8, 0.67, 0, 0);
    StatValue replyWaitsInProgressStatValueLocator = MockUtils.mockStatValue("replyWaitsInProgress", "replyWaitsInProgress", true, "replyWaitsInProgress", 2, 8, 0.67, 0, 0);

    Statistic tokensStatistic = spy(new Statistic(tokens));
    Statistic serversStatistic = spy(new Statistic(serversStatValue));
    Statistic jvmPausesStatistic = spy(new Statistic(jvmPausesStatValue));
    Statistic delayDurationStatistic = spy(new Statistic(delayDurationStatValue));
    Statistic replyWaitsInProgressServerStatistic = spy(new Statistic(replyWaitsInProgressStatValueServer));
    Statistic replyWaitsInProgressLocatorStatistic = spy(new Statistic(replyWaitsInProgressStatValueLocator));

    Category vmStatsCategory = new Category("StatsSampler", "VMStatsCategory");
    vmStatsCategory.addStatistic(jvmPausesStatistic);
    vmStatsCategory.addStatistic(delayDurationStatistic);

    Category distributionStatsCategoryServer = new Category("DistributionStats", "DistributionStatsCategory");
    distributionStatsCategoryServer.addStatistic(replyWaitsInProgressServerStatistic);
    Category distributionStatsCategoryLocator = new Category("DistributionStats", "DistributionStatsCategory");
    distributionStatsCategoryLocator.addStatistic(replyWaitsInProgressLocatorStatistic);

    Category poolStatsCategory = new Category("PoolStats", "PoolStatsCategory");
    poolStatsCategory.addStatistic(serversStatistic);

    Category dlocksStatsCategory = new Category("DLockStats", "DLockStatsCategory");
    dlocksStatsCategory.addStatistic(tokensStatistic);

    Map<String, Category> clientCategories = new HashMap<>();
    clientCategories.put(poolStatsCategory.getName(), poolStatsCategory);

    Map<String, Category> serverCategories = new HashMap<>();
    serverCategories.put(vmStatsCategory.getName(), vmStatsCategory);
    serverCategories.put(distributionStatsCategoryServer.getName(), distributionStatsCategoryServer);

    Map<String, Category> locatorCategories = new HashMap<>();
    locatorCategories.put(dlocksStatsCategory.getName(), dlocksStatsCategory);
    locatorCategories.put(distributionStatsCategoryLocator.getName(), distributionStatsCategoryLocator);

    Path mockedClientFile = MockUtils.mockPath("/samples/client.gfs", false);
    Path mockedServerFile = MockUtils.mockPath("/samples/server.gfs", false);
    Path mockedLocatorFile = MockUtils.mockPath("/samples/locator.gz", false);
    Sampling clientSampling = new Sampling(mock(SamplingMetadata.class), clientCategories);
    Sampling serverSampling = new Sampling(mock(SamplingMetadata.class), serverCategories);
    Sampling locatorSampling = new Sampling(mock(SamplingMetadata.class), locatorCategories);

    ParsingResult clientResult = new ParsingResult<>(mockedClientFile, clientSampling);
    ParsingResult serverResult = new ParsingResult<>(mockedServerFile, serverSampling);
    ParsingResult locatorResult = new ParsingResult<>(mockedLocatorFile, locatorSampling);

    List<ParsingResult<Sampling>> parsingResults = Arrays.asList(clientResult, serverResult, locatorResult);
    Table resultTable = null;
    TableModel resultTableModel = null;

    // ############ includeEmptyStatistics = false
    resultTable = showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, false, Statistic.Filter.None, parsingResults);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel).isNotNull();
    assertThat(resultTableModel.getRowCount()).isEqualTo(7);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    verify(serversStatistic, times(1)).setFilter(Statistic.Filter.None);
    verify(delayDurationStatistic, times(1)).setFilter(Statistic.Filter.None);
    verify(replyWaitsInProgressServerStatistic, times(1)).setFilter(Statistic.Filter.None);
    verify(replyWaitsInProgressLocatorStatistic, times(1)).setFilter(Statistic.Filter.None);

    // DistributionStats.replyWaitsInProgress
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──/locator.gz");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(2, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(2, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(2, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(2, 5)).isEqualTo("0.00");

    // PoolStats.servers
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("PoolStats.servers");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(3, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(3, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(3, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(3, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(4, 0)).isEqualTo("└──/client.gfs");
    assertThat(resultTableModel.getValue(4, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(4, 2)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(4, 3)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(4, 4)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(4, 5)).isEqualTo("0.00");

    // StatsSampler.delayDuration
    assertThat(resultTableModel.getValue(5, 0)).isEqualTo("StatsSampler.delayDuration");
    assertThat(resultTableModel.getValue(5, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(5, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(5, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(5, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(5, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(6, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(6, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(6, 2)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(6, 3)).isEqualTo("5.00");
    assertThat(resultTableModel.getValue(6, 4)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(6, 5)).isEqualTo("0.00");

    // ############ includeEmptyStatistics = true
    resultTable = showStatisticsSummaryCommand.buildTableGroupedByStatistic(mockedRootPath, true, Statistic.Filter.Second, parsingResults);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel).isNotNull();
    assertThat(resultTableModel.getRowCount()).isEqualTo(11);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    verify(tokensStatistic, times(1)).setFilter(Statistic.Filter.Second);
    verify(serversStatistic, times(1)).setFilter(Statistic.Filter.Second);
    verify(jvmPausesStatistic, times(1)).setFilter(Statistic.Filter.Second);
    verify(delayDurationStatistic, times(1)).setFilter(Statistic.Filter.Second);
    verify(replyWaitsInProgressServerStatistic, times(1)).setFilter(Statistic.Filter.Second);
    verify(replyWaitsInProgressLocatorStatistic, times(1)).setFilter(Statistic.Filter.Second);

    // DLockStats.tokens
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("DLockStats.tokens");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──/locator.gz");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");

    // DistributionStats.replyWaitsInProgress
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(2, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(2, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(2, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(2, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("└──/locator.gz");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(3, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(3, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(3, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(3, 5)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(4, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(4, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(4, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(4, 5)).isEqualTo("0.00");

    // PoolStats.servers
    assertThat(resultTableModel.getValue(5, 0)).isEqualTo("PoolStats.servers");
    assertThat(resultTableModel.getValue(5, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(5, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(5, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(5, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(5, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(6, 0)).isEqualTo("└──/client.gfs");
    assertThat(resultTableModel.getValue(6, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(6, 2)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(6, 3)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(6, 4)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(6, 5)).isEqualTo("0.00");

    // StatsSampler.delayDuration
    assertThat(resultTableModel.getValue(7, 0)).isEqualTo("StatsSampler.delayDuration");
    assertThat(resultTableModel.getValue(7, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(7, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(7, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(7, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(7, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(8, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(8, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(8, 2)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(8, 3)).isEqualTo("5.00");
    assertThat(resultTableModel.getValue(8, 4)).isEqualTo("10.00");
    assertThat(resultTableModel.getValue(8, 5)).isEqualTo("0.00");

    // StatsSampler.jvmPauses
    assertThat(resultTableModel.getValue(9, 0)).isEqualTo("StatsSampler.jvmPauses");
    assertThat(resultTableModel.getValue(9, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(9, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(9, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(9, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(9, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(10, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(10, 1)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(10, 2)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(10, 3)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(10, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(10, 5)).isEqualTo("0.00");
  }

  @Test
  public void showStatisticsSummaryShouldThrowExceptionWhenCategoryIdAndStatisticIdAreBothEmpty() {
    assertThatThrownBy(() -> showStatisticsSummaryCommand
        .showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Either '--category' or '--statistic' parameter should be specified.");

    assertThatThrownBy(() -> showStatisticsSummaryCommand
        .showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Either '--category' or '--statistic' parameter should be specified.");

    assertThatThrownBy(() -> showStatisticsSummaryCommand
        .showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "     ", "    "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Either '--category' or '--statistic' parameter should be specified.");
  }

  @Test
  public void showStatisticsSummaryShouldThrowExceptionWhenFileIsNotReadable() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> showStatisticsSummaryCommand
        .showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "categoryId", "statisticId"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Mocked IllegalArgumentException.");
  }

  @Test
  public void showStatisticsSummaryShouldPropagateExceptionsThrownByTheServiceLayer() {
    doThrow(new RuntimeException()).when(statisticsService).parseSampling(any(), any());
    assertThatThrownBy(() -> showStatisticsSummaryCommand
        .showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "categoryId", "statisticId"))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @Parameters({ "true", "false" })
  public void showStatisticsSummaryShouldSetTheProperFilterWhenInvokingTheServiceLayer(String strictMatching) {
    Boolean strictMatchingFlag = Boolean.valueOf(strictMatching);
    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    when(statisticsService.parseSampling(any(), any())).thenReturn(Collections.emptyList());

    showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, strictMatchingFlag, false, "categoryId", "statisticId");
    verify(statisticsService, times(1)).parseSampling(any(), argumentCaptor.capture());
    List<ValueFilter> filtersUsed = argumentCaptor.getValue();
    assertThat(filtersUsed).isNotNull();
    assertThat(filtersUsed.size()).isEqualTo(1);
    assertThat(filtersUsed.get(0)).isInstanceOf(strictMatchingFlag ? SimpleValueFilter.class : RegexValueFilter.class);
  }

  @Test
  public void showStatisticsSummaryShouldReturnStringWhenNoStatisticsFilesAreFound() {
    when(statisticsService.parseSampling(any(), any())).thenReturn(Collections.emptyList());
    Object resultObject = showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, false, false, "categoryId", "statisticId");

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  @Parameters({ "Statistic", "Sampling" })
  public void showStatisticsSummaryShouldReturnStringWhenNoMatchingResultsAreFound(String groupingCriteria) {
    ShowStatisticsSummaryCommand.GroupCriteria criteria = ShowStatisticsSummaryCommand.GroupCriteria.valueOf(groupingCriteria);
    List<ParsingResult<Sampling>> mockedResults = Arrays.asList(new ParsingResult<>(MockUtils.mockPath("/samples/file.gfs", false), mock(Sampling.class)));
    when(statisticsService.parseSampling(any(), any())).thenReturn(mockedResults);
    doReturn(null).when(showStatisticsSummaryCommand).buildTableGroupedByStatistic(any(), anyBoolean(), any(), any());
    doReturn(null).when(showStatisticsSummaryCommand).buildTableGroupedBySampling(any(), anyBoolean(), any(), any());
    Object resultObject = showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), criteria, Statistic.Filter.None, false, false, "categoryId", "statisticId");

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No matching results found.");
  }

  @Test
  public void showStatisticsSummaryShouldGroupResultsByTheConfiguredGroupingCriteria() {
    List<ParsingResult<Sampling>> mockedResults = Arrays.asList(new ParsingResult<>(MockUtils.mockPath("/samples/file.gfs", false), mock(Sampling.class)));
    when(statisticsService.parseSampling(any(), any())).thenReturn(mockedResults);

    showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Sampling, Statistic.Filter.None, false, false, "categoryId", "statisticId");
    verify(showStatisticsSummaryCommand, times(1)).buildTableGroupedBySampling(mockedRootPath, false, Statistic.Filter.None, mockedResults);
    verify(showStatisticsSummaryCommand, times(0)).buildTableGroupedByStatistic(mockedRootPath, false, Statistic.Filter.None, mockedResults);

    reset(showStatisticsSummaryCommand);
    showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, false, false, "categoryId", "statisticId");
    verify(showStatisticsSummaryCommand, times(0)).buildTableGroupedBySampling(mockedRootPath, false, Statistic.Filter.None, mockedResults);
    verify(showStatisticsSummaryCommand, times(1)).buildTableGroupedByStatistic(mockedRootPath, false, Statistic.Filter.None, mockedResults);
  }

  @Test
  public void showStatisticsSummaryShouldReturnOnlyErrorTableIfParsingFailsForAllFiles() {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    List<ParsingResult<Sampling>> mockedResults = Collections.singletonList(new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception")));
    when(statisticsService.parseSampling(any(), any())).thenReturn(mockedResults);

    Object resultObject = showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "categoryId", "statisticId");
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table errorsResultTable = resultList.get(0);
    assertThat(errorsResultTable).isNotNull();
    TableModel errorsTableModel = errorsResultTable.getModel();
    assertThat(errorsTableModel.getRowCount()).isEqualTo(2);
    assertThat(errorsTableModel.getColumnCount()).isEqualTo(2);
    assertThat(errorsTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTableModel.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTableModel.getValue(1, 0)).isEqualTo("mockedUnparseableFile.gfs");
    assertThat(errorsTableModel.getValue(1, 1)).isEqualTo("Mocked Exception");
  }

  @Test
  public void showStatisticsSummaryShouldReturnOnlyResultsTableIfParsingSucceedsForAllFiles() {
    StatValue replyWaitsInProgressStatValue = MockUtils.mockStatValue("replyWaitsInProgress", "replyWaitsInProgress", true, "replyWaitsInProgress", 2, 8, 0.67, 0, 0);
    Statistic replyWaitsInProgressStatistic = spy(new Statistic(replyWaitsInProgressStatValue));
    Category distributionStatsCategory = new Category("DistributionStats", "DistributionStatsCategory");
    distributionStatsCategory.addStatistic(replyWaitsInProgressStatistic);
    Map<String, Category> categoryMap = new HashMap<>();
    categoryMap.put(distributionStatsCategory.getName(), distributionStatsCategory);
    Path mockedPath = MockUtils.mockPath("/samples/server.gfs", false);
    Sampling sampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    ParsingResult parsingResult = new ParsingResult<>(mockedPath, sampling);
    List<ParsingResult<Sampling>> mockedResults = Arrays.asList(parsingResult);
    when(statisticsService.parseSampling(any(), any())).thenReturn(mockedResults);
    Object resultObject = showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "categoryId", "statisticId");

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(2);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");
  }

  @Test
  public void showStatisticsSummaryShouldReturnErrorAndResultTablesInOrder() {
    Path mockedParseableFile = MockUtils.mockPath("/samples/server.gfs", false);
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    Statistic replyWaitsInProgressStatistic = spy(new Statistic(MockUtils.mockStatValue("replyWaitsInProgress", "replyWaitsInProgress", true, "replyWaitsInProgress", 2, 8, 0.67, 0, 0)));
    Category distributionStatsCategory = new Category("DistributionStats", "DistributionStatsCategory");
    distributionStatsCategory.addStatistic(replyWaitsInProgressStatistic);
    Map<String, Category> categoryMap = new HashMap<>();
    categoryMap.put(distributionStatsCategory.getName(), distributionStatsCategory);
    Sampling sampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    when(statisticsService.parseSampling(any(), any())).thenReturn(Arrays.asList(new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception")), new ParsingResult<>(mockedParseableFile, sampling)));

    Object resultObject = showStatisticsSummaryCommand.showStatisticsSummary(mockedRootPath.toFile(), ShowStatisticsSummaryCommand.GroupCriteria.Statistic, Statistic.Filter.None, true, false, "categoryId", "statisticId");
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    // Results Table First
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(2);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("DistributionStats.replyWaitsInProgress");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Minimum");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Maximum");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Average");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Last Value");
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Standard Deviation");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("└──/server.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("2.00");
    assertThat(resultTableModel.getValue(1, 2)).isEqualTo("8.00");
    assertThat(resultTableModel.getValue(1, 3)).isEqualTo("0.67");
    assertThat(resultTableModel.getValue(1, 4)).isEqualTo("0.00");
    assertThat(resultTableModel.getValue(1, 5)).isEqualTo("0.00");

    // Error Table Last
    Table errorsResultTable = resultList.get(1);
    assertThat(errorsResultTable).isNotNull();
    TableModel errorsTableModel = errorsResultTable.getModel();
    assertThat(errorsTableModel.getRowCount()).isEqualTo(2);
    assertThat(errorsTableModel.getColumnCount()).isEqualTo(2);
    assertThat(errorsTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTableModel.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTableModel.getValue(1, 0)).isEqualTo("mockedUnparseableFile.gfs");
    assertThat(errorsTableModel.getValue(1, 1)).isEqualTo("Mocked Exception");
  }
}