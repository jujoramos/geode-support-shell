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
package org.apache.geode.support.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.geode.internal.statistics.ArchiveInfo;
import org.apache.geode.internal.statistics.StatArchiveFile;
import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.internal.statistics.StatValue;
import org.apache.geode.internal.statistics.ValueFilter;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.Category;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.domain.statistics.filters.RegexValueFilter;
import org.apache.geode.support.domain.statistics.filters.SimpleValueFilter;
import org.apache.geode.support.test.mockito.MockUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Files.class, DefaultStatisticsService.class })
public class DefaultStatisticsServicesTest {
  private Path mockedRegularPath;
  private Path mockedCompressedPath;
  private Path mockedDirectoryPath;
  private Stream<Path> mockedDirectoryStream;
  private DefaultStatisticsService statisticsService;

  @Before
  public void setUp() throws IOException {
    statisticsService = spy(new DefaultStatisticsService());

    mockedDirectoryPath = MockUtils.mockPath("/mockedDirectory", true);
    mockedRegularPath = MockUtils.mockPath("/mockedDirectory/mockedFile.gfs", false);
    mockedCompressedPath = MockUtils.mockPath("/mockedDirectory/mockedFile.gz", false);

    mockedDirectoryStream = Stream.of(mockedRegularPath, mockedCompressedPath);
    PowerMockito.mockStatic(Files.class);

    when(Files.isRegularFile(any())).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
  }

  @Test
  public void isStatisticFileTest() {
    Path mockedPath = MockUtils.mockPath("mockFile.txt", false);

    when(Files.isRegularFile(any())).thenReturn(false);
    assertThat(statisticsService.isStatisticsFile().test(mockedPath)).isFalse();
    assertThat(statisticsService.isStatisticsFile().test(mockedRegularPath)).isFalse();
    assertThat(statisticsService.isStatisticsFile().test(mockedCompressedPath)).isFalse();

    when(Files.isRegularFile(any())).thenReturn(true);
    assertThat(statisticsService.isStatisticsFile().test(mockedPath)).isFalse();
    assertThat(statisticsService.isStatisticsFile().test(mockedRegularPath)).isTrue();
    assertThat(statisticsService.isStatisticsFile().test(mockedCompressedPath)).isTrue();
  }

  @Test
  public void isSearchedResourceInstanceTest() {
    SimpleValueFilter filter = mock(SimpleValueFilter.class);
    when(filter.getTypeId()).thenReturn("matchingType");
    StatArchiveReader.ResourceInst matchingResourceInst = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("matchingType", ""), null);
    StatArchiveReader.ResourceInst nonMatchingResourceInst = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("mockType", ""), null);

    // Null Resource Check
    assertThat(statisticsService.isSearchedResourceInstance(filter).test(null)).isFalse();

    // Not Loaded Resource
    when(matchingResourceInst.isLoaded()).thenReturn(false);
    when(nonMatchingResourceInst.isLoaded()).thenReturn(false);
    assertThat(statisticsService.isSearchedResourceInstance(filter).test(matchingResourceInst)).isFalse();
    assertThat(statisticsService.isSearchedResourceInstance(filter).test(nonMatchingResourceInst)).isFalse();

    // Matching Resource
    when(matchingResourceInst.isLoaded()).thenReturn(true);
    assertThat(statisticsService.isSearchedResourceInstance(filter).test(matchingResourceInst)).isTrue();
  }

  @Test
  public void parseSamplingMetadataShouldThrowExceptionWhenStatFileIsNull() {
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(null)).isInstanceOf(NullPointerException.class).hasMessage("StatArchiveFile can not be null.");
  }

  @Test
  public void parseSamplingMetadataShouldThrowExceptionWhenResourceInstancesTableIsNotLoaded() {
    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);

    // Null ResourceInstancesTable.
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(null);
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");

    // Empty ResourceInstancesTable.
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[0]);
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");
  }

  @Test
  public void parseSamplingMetadataShouldThrowExceptionWhenStatValueIsNullOrIncomplete() {
    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    StatArchiveReader.ResourceInst mockedResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("VMStats", ""), null);
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { mockedResourceInstance });

    // Null StatValue.
    when(mockedResourceInstance.getStatValue(any())).thenReturn(null);
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, StatValue for cpus should not be null.");

    // Null TimeStamps.
    StatValue cpusStatValue = mock(StatValue.class);
    when(cpusStatValue.getRawAbsoluteTimeStamps()).thenReturn(null);
    when(mockedResourceInstance.getStatValue(any())).thenReturn(cpusStatValue);
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, array of TimeStamps for cpus should not be null.");

    // Empty TimeStamps.
    when(cpusStatValue.getRawAbsoluteTimeStamps()).thenReturn(new long[0]);
    assertThatThrownBy(() -> statisticsService.parseSamplingMetadata(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, array of TimeStamps for cpus should not be null.");
  }

  @Test
  public void parseSamplingMetadataShouldWorkCorrectly() {
    ArchiveInfo mockedArchiveInfo = mock(ArchiveInfo.class);
    when(mockedArchiveInfo.getArchiveFileName()).thenReturn("mockedFile.gfs");
    when(mockedArchiveInfo.getArchiveFormatVersion()).thenReturn(1);
    when(mockedArchiveInfo.isCompressed()).thenReturn(false);
    when(mockedArchiveInfo.getTimeZone()).thenReturn(TimeZone.getDefault());
    when(mockedArchiveInfo.getProductVersion()).thenReturn("Geode-1.0");
    when(mockedArchiveInfo.getOs()).thenReturn("Linux 2.6.32-696.el6.x86_64");

    StatValue cpusStatValue = mock(StatValue.class);
    when(cpusStatValue.getRawAbsoluteTimeStamps()).thenReturn(new long[] { 10, 2, 1, 5, 11, 6, 3 });
    StatArchiveReader.ResourceInst mockedResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("VMStats", null), new StatValue[] { cpusStatValue });
    when(mockedResourceInstance.getStatValue(any())).thenReturn(cpusStatValue);

    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    when(mockedStatArchiveFile.getArchiveInfo()).thenReturn(mockedArchiveInfo);
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { mockedResourceInstance });

    SamplingMetadata samplingMetadata = statisticsService.parseSamplingMetadata(mockedStatArchiveFile);
    assertThat(samplingMetadata.getVersion()).isEqualTo(1);
    assertThat(samplingMetadata.getFileName()).isEqualTo("mockedFile.gfs");
    assertThat(samplingMetadata.getTimeZoneId()).isEqualTo(ZoneId.systemDefault());
    assertThat(samplingMetadata.isCompressed()).isFalse();
    assertThat(samplingMetadata.getStartTimeStamp()).isEqualTo(1);
    assertThat(samplingMetadata.getFinishTimeStamp()).isEqualTo(11);
    assertThat(samplingMetadata.getProductVersion()).isEqualTo("Geode-1.0");
    assertThat(samplingMetadata.getOperatingSystem()).isEqualTo("Linux 2.6.32-696.el6.x86_64");
  }

  @Test
  public void parseSamplingStatisticalDataShouldThrowExceptionWhenStatFileIsNull() {
    assertThatThrownBy(() -> statisticsService.parseSamplingStatisticalData(null)).isInstanceOf(NullPointerException.class).hasMessage("StatArchiveFile can not be null.");
  }

  @Test
  public void parseSamplingStatisticalDataShouldThrowExceptionWhenResourceInstancesTableIsNotLoaded() {
    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);

    // Null ResourceInstancesTable.
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(null);
    assertThatThrownBy(() -> statisticsService.parseSamplingStatisticalData(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");

    // Empty ResourceInstancesTable.
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[0]);
    assertThatThrownBy(() -> statisticsService.parseSamplingStatisticalData(mockedStatArchiveFile)).isInstanceOf(IllegalStateException.class).hasMessage("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");
  }

  @Test
  public void parseSamplingStatisticalDataShouldWorkCorrectly() {
    StatArchiveReader.ResourceType vmStatsResourceType = MockUtils.mockResourceType("VMStats", "Stats available on any java virtual machine.");
    StatArchiveReader.ResourceType distributionStatsResourceType = MockUtils.mockResourceType("DistributionStats", "Statistics on the gemfire distribution layer.");
    StatArchiveReader.ResourceType cachePerfStatsResourceType = MockUtils.mockResourceType("CachePerfStats", "Statistics about GemFire cache performance.");
    StatValue cpusValue = MockUtils.mockStatValue("cpus", "Number of CPUs available to the member on its machine.", false, "#cpus");
    StatValue fdsOpenValue = MockUtils.mockStatValue("fdsOpen", "Current number of open file descriptors.", true, "#fdsOpen");
    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    StatArchiveReader.ResourceInst cachePerfStatsResourceInstance = MockUtils.mockResourceInstance(false, cachePerfStatsResourceType, null);
    StatArchiveReader.ResourceInst vmStatsResourceInstance = MockUtils.mockResourceInstance(true, vmStatsResourceType, new StatValue[] { cpusValue, null, fdsOpenValue });
    StatArchiveReader.ResourceInst distributionStatsResourceInstance = MockUtils.mockResourceInstance(true, distributionStatsResourceType, new StatValue[] { null, null, null, null });
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { vmStatsResourceInstance, null, distributionStatsResourceInstance, cachePerfStatsResourceInstance });

    Map<String, Category> statisticalData = statisticsService.parseSamplingStatisticalData(mockedStatArchiveFile);
    assertThat(statisticalData.size()).isEqualTo(2);

    // VMStats
    assertThat(statisticalData.containsKey("VMStats")).isTrue();
    Category vmStatsCategory = statisticalData.get("VMStats");
    assertThat(vmStatsCategory.getName()).isEqualTo("VMStats");
    assertThat(vmStatsCategory.getDescription()).isEqualTo("Stats available on any java virtual machine.");
    assertThat(vmStatsCategory.hasStatistic("cpus")).isTrue();
    assertThat(vmStatsCategory.hasStatistic("fdsOpen")).isTrue();

    Statistic cpuStatistics = vmStatsCategory.getStatistic("cpus");
    assertThat(cpuStatistics.getName()).isEqualTo("cpus");
    assertThat(cpuStatistics.getDescription()).isEqualTo("Number of CPUs available to the member on its machine.");
    assertThat(cpuStatistics.isCounter()).isFalse();
    assertThat(cpuStatistics.getUnits()).isEqualTo("#cpus");

    Statistic fdsOpenStatistics = vmStatsCategory.getStatistic("fdsOpen");
    assertThat(fdsOpenStatistics.getName()).isEqualTo("fdsOpen");
    assertThat(fdsOpenStatistics.getDescription()).isEqualTo("Current number of open file descriptors.");
    assertThat(fdsOpenStatistics.isCounter()).isTrue();
    assertThat(fdsOpenStatistics.getUnits()).isEqualTo("#fdsOpen");

    // DistributionStats
    assertThat(statisticalData.containsKey("DistributionStats")).isTrue();
    Category distributionStatsCategory = statisticalData.get("DistributionStats");
    assertThat(distributionStatsCategory.getName()).isEqualTo(distributionStatsResourceType.getName());
    assertThat(distributionStatsCategory.getDescription()).isEqualTo(distributionStatsResourceType.getDescription());
    assertThat(distributionStatsCategory.getStatistics().isEmpty()).isTrue();
  }

  @Test
  public void parseIndividualSamplingShouldAddTheDefaultFilterOnlyWhenItIsNotAlreadyIncluded() throws Exception {
    doReturn(mock(StatArchiveFile.class)).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());
    doReturn(mock(Map.class)).when(statisticsService).parseSamplingStatisticalData(any());

    statisticsService.parseIndividualSampling(mockedRegularPath, Collections.emptyList());
    verify(statisticsService, times(1)).initializeStatArchiveFile(mockedRegularPath, Arrays.asList(statisticsService.defaultValueFilter));

    ValueFilter simpleFilter = new SimpleValueFilter("VMStats", null, "cpus", null);
    statisticsService.parseIndividualSampling(mockedRegularPath, Arrays.asList(simpleFilter));
    verify(statisticsService, times(2)).initializeStatArchiveFile(mockedRegularPath, Arrays.asList(simpleFilter));

    ValueFilter regexFilter = new RegexValueFilter("VMStats", null, "cpus", null);
    statisticsService.parseIndividualSampling(mockedRegularPath, Arrays.asList(regexFilter));
    verify(statisticsService, times(3)).initializeStatArchiveFile(mockedRegularPath, Arrays.asList(regexFilter));
  }

  @Test
  public void parseIndividualSamplingShouldRemoveTheStatisticAddedByTheDefaultFilterWhenTheDefaultFilterIsNotAlreadyIncluded() throws Exception {
    List<ValueFilter> filters = new ArrayList<>();
    StatValue cpusValue = MockUtils.mockStatValue("cpus", "Number of CPUs available to the member on its machine.", false, "#cpus");
    StatValue fdsOpenValue = MockUtils.mockStatValue("fdsOpen", "Current number of open file descriptors.", true, "#fdsOpen");
    StatValue replyWaitsInProgressValue = MockUtils.mockStatValue("replyWaitsInProgress", "Current number of threads waiting for a reply.", true, "operations");

    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    StatArchiveReader.ResourceInst vmStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("VMStats", ""), new StatValue[] { cpusValue, fdsOpenValue });
    StatArchiveReader.ResourceInst distributionStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("DistributionStats", ""), new StatValue[] { replyWaitsInProgressValue });
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { vmStatsResourceInstance, distributionStatsResourceInstance });

    doReturn(mockedStatArchiveFile).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());

    filters.add(new SimpleValueFilter("VMStats", null, "fdsOpen", null));
    filters.add(new SimpleValueFilter("DistributionStats", null, "replyWaitsInProgress", null));

    Sampling sampling = statisticsService.parseIndividualSampling(mockedRegularPath, filters);
    assertThat(sampling.hasCategory("VMStats")).isTrue();
    assertThat(sampling.getCategory("VMStats").hasStatistic("cpus")).isFalse();
    assertThat(sampling.getCategory("VMStats").hasStatistic("fdsOpen")).isTrue();
    assertThat(sampling.hasCategory("DistributionStats")).isTrue();
    assertThat(sampling.getCategory("DistributionStats").hasStatistic("replyWaitsInProgress")).isTrue();
  }

  @Test
  public void parseIndividualSamplingShouldRemoveTheCategoryAddedByTheDefaultFilterWhenTheDefaultFilterIsNotAlreadyIncludedAndWhenNoOtherStatisticsForThatCategoryAreIncludedInOtherFilters() throws Exception {
    List<ValueFilter> filters = new ArrayList<>();
    StatValue cpusValue = MockUtils.mockStatValue("cpus", "Number of CPUs available to the member on its machine.", false, "#cpus");
    StatValue replyWaitsInProgressValue = MockUtils.mockStatValue("replyWaitsInProgress", "Current number of threads waiting for a reply.", true, "operations");

    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    StatArchiveReader.ResourceInst vmStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("VMStats", ""), new StatValue[] { cpusValue });
    StatArchiveReader.ResourceInst distributionStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("DistributionStats", ""), new StatValue[] { replyWaitsInProgressValue });
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { vmStatsResourceInstance, distributionStatsResourceInstance });

    doReturn(mockedStatArchiveFile).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());

    filters.add(new SimpleValueFilter("DistributionStats", null, "replyWaitsInProgress", null));
    Sampling sampling = statisticsService.parseIndividualSampling(mockedRegularPath, filters);
    assertThat(sampling.hasCategory("VMStats")).isFalse();
    assertThat(sampling.hasCategory("DistributionStats")).isTrue();
    assertThat(sampling.getCategory("DistributionStats").hasStatistic("replyWaitsInProgress")).isTrue();
  }

  @Test
  public void parseIndividualSamplingShouldNotRemoveTheStatisticAddedByTheDefaultFilterWhenTheDefaultFilterIsAlreadyIncluded() throws Exception {
    List<ValueFilter> filters = new ArrayList<>();
    StatValue cpusValue = MockUtils.mockStatValue("cpus", "Number of CPUs available to the member on its machine.", false, "#cpus");
    StatValue fdsOpenValue = MockUtils.mockStatValue("fdsOpen", "Current number of open file descriptors.", true, "#fdsOpen");
    StatValue replyWaitsInProgressValue = MockUtils.mockStatValue("replyWaitsInProgress", "Current number of threads waiting for a reply.", true, "operations");

    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    StatArchiveReader.ResourceInst vmStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("VMStats", ""), new StatValue[] { cpusValue, fdsOpenValue });
    StatArchiveReader.ResourceInst distributionStatsResourceInstance = MockUtils.mockResourceInstance(true, MockUtils.mockResourceType("DistributionStats", ""), new StatValue[] { replyWaitsInProgressValue });
    when(mockedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { vmStatsResourceInstance, distributionStatsResourceInstance });

    doReturn(mockedStatArchiveFile).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());

    filters.add(new SimpleValueFilter("VMStats", null, "cpus", null));
    filters.add(new SimpleValueFilter("VMStats", null, "fdsOpen", null));
    filters.add(new SimpleValueFilter("DistributionStats", null, "replyWaitsInProgress", null));
    Sampling sampling = statisticsService.parseIndividualSampling(mockedRegularPath, filters);

    assertThat(filters.size()).isEqualTo(3);
    assertThat(sampling.hasCategory("VMStats")).isTrue();
    assertThat(sampling.getCategory("VMStats").hasStatistic("cpus")).isTrue();
    assertThat(sampling.getCategory("VMStats").hasStatistic("fdsOpen")).isTrue();
    assertThat(sampling.hasCategory("DistributionStats")).isTrue();
    assertThat(sampling.getCategory("DistributionStats").hasStatistic("replyWaitsInProgress")).isTrue();
  }

  @Test
  public void parseIndividualSamplingShouldInvokeParseStatisticalDataOnlyWhenThereIsAtLeastOneNonDefaultFilter() throws Exception {
    doReturn(mock(StatArchiveFile.class)).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());
    doReturn(mock(Map.class)).when(statisticsService).parseSamplingStatisticalData(any());

    List<ValueFilter> filters = new ArrayList<>();
    statisticsService.parseIndividualSampling(mockedRegularPath, filters);
    verify(statisticsService, times(0)).parseSamplingStatisticalData(any());

    List<ValueFilter> simpleFilters = Arrays.asList(new SimpleValueFilter("VMStats", null, "cpus", null));
    statisticsService.parseIndividualSampling(mockedRegularPath, simpleFilters);
    verify(statisticsService, times(0)).parseSamplingStatisticalData(any());

    List<ValueFilter> regexFilters = Arrays.asList(new RegexValueFilter("VMStats", null, "cpus", null));
    statisticsService.parseIndividualSampling(mockedRegularPath, regexFilters);
    verify(statisticsService, times(0)).parseSamplingStatisticalData(any());

    List<ValueFilter> customFilters = new ArrayList<>();
    customFilters.add(mock(ValueFilter.class));
    statisticsService.parseIndividualSampling(mockedRegularPath, customFilters);
    verify(statisticsService, times(1)).parseSamplingStatisticalData(any());
  }

  @Test
  public void parseIndividualSamplingShouldPropagateAllExceptionsAndCloseTheStatArchiveFileWhenPossible() throws Exception {
    List<ValueFilter> filters = new ArrayList<>();
    filters.add(new SimpleValueFilter(null, null, null, null));

    doThrow(new IOException("Mocked IOException when calling initializeStatArchiveFile.")).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    assertThatThrownBy(() -> statisticsService.parseIndividualSampling(mockedRegularPath, filters)).isInstanceOf(IOException.class).hasMessage("Mocked IOException when calling initializeStatArchiveFile.");

    StatArchiveFile mockedStatArchiveFile = mock(StatArchiveFile.class);
    doReturn(mockedStatArchiveFile).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doThrow(new NullPointerException("Mocked NullPointerException when calling parseSamplingMetadata.")).when(statisticsService).parseSamplingMetadata(any());
    assertThatThrownBy(() -> statisticsService.parseIndividualSampling(mockedRegularPath, filters)).isInstanceOf(NullPointerException.class).hasMessage("Mocked NullPointerException when calling parseSamplingMetadata.");
    verify(mockedStatArchiveFile, times(1)).close();

    StatArchiveFile anotherMockedStatArchiveFile = mock(StatArchiveFile.class);
    doReturn(anotherMockedStatArchiveFile).when(statisticsService).initializeStatArchiveFile(any(), anyList());
    doReturn(mock(SamplingMetadata.class)).when(statisticsService).parseSamplingMetadata(any());
    doThrow(new NullPointerException("Mocked NullPointerException when calling parseSamplingStatisticalData.")).when(statisticsService).parseSamplingStatisticalData(any());
    assertThatThrownBy(() -> statisticsService.parseIndividualSampling(mockedRegularPath, filters)).isInstanceOf(NullPointerException.class).hasMessage("Mocked NullPointerException when calling parseSamplingStatisticalData.");
    verify(anotherMockedStatArchiveFile, times(1)).close();
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorWhenSourcePathCanNotBeTraversed() throws Exception {
    when(Files.walk(mockedDirectoryPath)).thenThrow(new IOException("Mocked IOException"));
    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<SamplingMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getFile()).isNotNull();
    assertThat(parsingResult.getFile()).isEqualTo(mockedDirectoryPath);
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseMetadataShouldReturnOnlyParsingErrorsWhenParseIndividualSamplingFailsForAllFiles() throws Exception {
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(statisticsService).parseIndividualSampling(any(), any());

    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<SamplingMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isFalse();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedRegularPath);
    assertThat(parsingResult1.getException()).isNotNull();
    assertThat(parsingResult1.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");

    ParsingResult<SamplingMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isFalse();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedCompressedPath);
    assertThat(parsingResult2.getException()).isNotNull();
    assertThat(parsingResult2.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }

  @Test
  public void parseMetadataShouldReturnOnlyParsingSuccessesWhenParseIndividualSamplingSucceedsForAllFiles() throws Exception {
    Sampling mockedSampling = mock(Sampling.class);
    SamplingMetadata mockedMetadata = mock(SamplingMetadata.class);
    when(mockedSampling.getMetadata()).thenReturn(mockedMetadata);
    doReturn(mockedSampling).when(statisticsService).parseIndividualSampling(any(), anyList());

    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<SamplingMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isTrue();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedRegularPath);
    assertThat(parsingResult1.getData()).isNotNull();

    ParsingResult<SamplingMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isTrue();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedCompressedPath);
    assertThat(parsingResult2.getData()).isNotNull();
  }

  @Test
  public void parseMetadataShouldReturnBothParsingErrorsAndParsingSuccessesWhenParseIndividualSamplingSucceedsForSomeFilesAndFailsForOthers() throws Exception {
    Sampling mockedSampling = mock(Sampling.class);
    SamplingMetadata mockedMetadata = mock(SamplingMetadata.class);
    when(mockedSampling.getMetadata()).thenReturn(mockedMetadata);
    doReturn(mockedSampling).when(statisticsService).parseIndividualSampling(mockedCompressedPath, new ArrayList<>());
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(statisticsService).parseIndividualSampling(mockedRegularPath, new ArrayList<>());

    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    verify(statisticsService, times(2)).parseIndividualSampling(any(), any());

    ParsingResult<SamplingMetadata> succeededResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gz")).findAny().get();
    assertThat(succeededResult.isSuccess()).isTrue();
    assertThat(succeededResult.getData()).isNotNull();

    ParsingResult<SamplingMetadata> failedResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gfs")).findAny().get();
    assertThat(failedResult.isSuccess()).isFalse();
    assertThat(failedResult.getException()).isNotNull();
    assertThat(failedResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }

  @Test
  public void parseSamplingShouldReturnParsingErrorWhenSourcePathCanNotBeTraversed() throws Exception {
    when(Files.walk(mockedDirectoryPath)).thenThrow(new IOException("Mocked IOException"));
    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(mockedDirectoryPath, new ArrayList<>());

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<Sampling> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getFile()).isNotNull();
    assertThat(parsingResult.getFile()).isEqualTo(mockedDirectoryPath);
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseSamplingShouldReturnOnlyParsingErrorsWhenParseIndividualSamplingFailsForAllFiles() throws Exception {
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(statisticsService).parseIndividualSampling(any(), any());

    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(mockedDirectoryPath, new ArrayList<>());

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<Sampling> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isFalse();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedRegularPath);
    assertThat(parsingResult1.getException()).isNotNull();
    assertThat(parsingResult1.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");

    ParsingResult<Sampling> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isFalse();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedCompressedPath);
    assertThat(parsingResult2.getException()).isNotNull();
    assertThat(parsingResult2.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }

  @Test
  public void parseSamplingShouldReturnOnlyParsingSuccessesWhenParseIndividualSamplingSucceedsForAllFiles() throws Exception {
    doReturn(mock(Sampling.class)).when(statisticsService).parseIndividualSampling(any(), anyList());

    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(mockedDirectoryPath, new ArrayList<>());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<Sampling> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isTrue();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedRegularPath);
    assertThat(parsingResult1.getData()).isNotNull();

    ParsingResult<Sampling> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isTrue();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedCompressedPath);
    assertThat(parsingResult2.getData()).isNotNull();
  }

  @Test
  public void parseSamplingShouldReturnBothParsingErrorsAndParsingSuccessesWhenParseIndividualSamplingSucceedsForSomeFilesAndFailsForOthers() throws Exception {
    doReturn(mock(Sampling.class)).when(statisticsService).parseIndividualSampling(mockedCompressedPath, new ArrayList<>());
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(statisticsService).parseIndividualSampling(mockedRegularPath, new ArrayList<>());

    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(mockedDirectoryPath, new ArrayList<>());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    verify(statisticsService, times(2)).parseIndividualSampling(any(), any());

    ParsingResult<Sampling> succeededResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gz")).findAny().get();
    assertThat(succeededResult.isSuccess()).isTrue();
    assertThat(succeededResult.getData()).isNotNull();

    ParsingResult<Sampling> failedResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gfs")).findAny().get();
    assertThat(failedResult.isSuccess()).isFalse();
    assertThat(failedResult.getException()).isNotNull();
    assertThat(failedResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }
}
