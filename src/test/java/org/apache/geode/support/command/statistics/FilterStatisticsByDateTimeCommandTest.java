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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.test.MockUtils;

@RunWith(JUnitParamsRunner.class)
public class FilterStatisticsByDateTimeCommandTest {
  private File mockedSourceFolder;
  private Path mockedSourceFolderPath;
  private File mockedMatchingFolder;
  private Path mockedMatchingFolderPath;
  private File mockedNonMatchingFolder;
  private Path mockedNonMatchingFolderPath;

  private FilesService filesService;
  private StatisticsService statisticsService;
  private FilterStatisticsByDateTimeCommand statisticsCommands;

  @Before
  public void setUp() {
    mockedSourceFolder = mock(File.class);
    mockedSourceFolderPath = mock(Path.class);
    when(mockedSourceFolder.toPath()).thenReturn(mockedSourceFolderPath);
    when(mockedSourceFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedSourceFolderPath.toAbsolutePath().toString()).thenReturn("/mocked/source");

    mockedMatchingFolder = mock(File.class);
    mockedMatchingFolderPath = mock(Path.class);
    when(mockedMatchingFolder.toPath()).thenReturn(mockedMatchingFolderPath);
    when(mockedMatchingFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedMatchingFolderPath.toAbsolutePath().toString()).thenReturn("/mocked/matching");

    mockedNonMatchingFolder = mock(File.class);
    mockedNonMatchingFolderPath = mock(Path.class);
    when(mockedNonMatchingFolder.toPath()).thenReturn(mockedNonMatchingFolderPath);
    when(mockedNonMatchingFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedNonMatchingFolderPath.toAbsolutePath().toString()).thenReturn("/mocked/nonMatching");

    filesService = mock(FilesService.class);
    statisticsService = mock(StatisticsService.class);
    statisticsCommands = new FilterStatisticsByDateTimeCommand(filesService, statisticsService);
  }

  @Test
  public void filterStatisticsByDateTimeShouldThrowExceptionWhenSourceFolderIsNotReadable() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFolderReadability(mockedSourceFolderPath);
    assertThatThrownBy(() -> statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Mocked IllegalArgumentException.$");
  }

  @Test
  public void filterStatisticsByDateTimeShouldThrowExceptionWhenFoldersAreEquals() {
    doThrow(new IllegalArgumentException("sourceFolder can't be the same as matchingFolder.")).when(filesService).assertPathsInequality(mockedSourceFolderPath, mockedMatchingFolderPath, "sourceFolder", "matchingFolder");
    assertThatThrownBy(() -> statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^sourceFolder can't be the same as matchingFolder.$");

    doNothing().when(filesService).assertPathsInequality(mockedSourceFolderPath, mockedMatchingFolderPath, "sourceFolder", "matchingFolder");
    doThrow(new IllegalArgumentException("sourceFolder can't be the same as nonMatchingFolder.")).when(filesService).assertPathsInequality(mockedSourceFolderPath, mockedNonMatchingFolderPath, "sourceFolder", "nonMatchingFolder");
    assertThatThrownBy(() -> statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^sourceFolder can't be the same as nonMatchingFolder.$");

    doNothing().when(filesService).assertPathsInequality(mockedSourceFolderPath, mockedMatchingFolderPath, "sourceFolder", "matchingFolder");
    doNothing().when(filesService).assertPathsInequality(mockedSourceFolderPath, mockedNonMatchingFolderPath, "sourceFolder", "nonMatchingFolder");
    doThrow(new IllegalArgumentException("matchingFolder can't be the same as nonMatchingFolder.")).when(filesService).assertPathsInequality(mockedMatchingFolderPath, mockedNonMatchingFolderPath, "matchingFolder", "nonMatchingFolder");
    assertThatThrownBy(() -> statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^matchingFolder can't be the same as nonMatchingFolder.$");
  }

  @Test
  public void filterStatisticsByDateTimeShouldPropagateExceptionsThrownByTheServiceLayer() {
    doThrow(new RuntimeException()).when(statisticsService).parseMetadata(any());
    assertThatThrownBy(() -> statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnStringWhenNoFilesAreFound() {
    when(statisticsService.parseMetadata(any())).thenReturn(Collections.emptyList());

    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnOnlyErrorTableIfParsingFailsForAllFiles() {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    ParsingResult<SamplingMetadata> errorResult = new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception"));
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);

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
  public void filterStatisticsByDateTimeShouldReturnOnlyResultsTableIfParsingSucceedsForAllFiles() {
    SamplingMetadata mockedMetadata = mock(SamplingMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    ParsingResult<SamplingMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.gfs", false), mockedMetadata);
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2010, 01, 01, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(2);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
  }

  @Test
  public void filterStatisticsByDateTimeShouldReturnErrorsAndResultsTableIfParsingSucceedsButFileCopyFails() throws Exception {
    ZoneId mockedZoneId = ZoneId.systemDefault();
    Path mockedMatchingPath = MockUtils.mockPath("matching.gfs", false);
    SamplingMetadata mockedMatchingMetadata = mock(SamplingMetadata.class);
    when(mockedMatchingMetadata.getTimeZoneId()).thenReturn(mockedZoneId);
    when(mockedMatchingMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    when(mockedMatchingMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 01, 00, 00, 00, mockedZoneId));
    when(mockedMatchingMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 02, 01, 00, 00, 00, mockedZoneId)));

    Path mockedNonMatchingPath = MockUtils.mockPath("nonMatching.gfs", false);
    SamplingMetadata mockedNonMatchingMetadata = mock(SamplingMetadata.class);
    when(mockedNonMatchingMetadata.getTimeZoneId()).thenReturn(mockedZoneId);
    when(mockedNonMatchingMetadata.getProductVersion()).thenReturn("GemFire 8.2.0 #build 0");
    when(mockedNonMatchingMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2019, 01, 01, 00, 00, 00, mockedZoneId));
    when(mockedNonMatchingMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2019, 02, 01, 00, 00, 00, mockedZoneId)));

    doThrow(new IOException("Mocked IOException for mockedMatchingPath.")).when(filesService).copyFile(mockedMatchingPath, mockedMatchingFolderPath);
    doThrow(new IOException("Mocked IOException for mockedNonMatchingPath.")).when(filesService).copyFile(mockedNonMatchingPath, mockedNonMatchingFolderPath);

    ParsingResult<SamplingMetadata> mockedMatchingResult = new ParsingResult<>(mockedMatchingPath, mockedMatchingMetadata);
    ParsingResult<SamplingMetadata> mockedNonMatchingResult = new ParsingResult<>(mockedNonMatchingPath, mockedNonMatchingMetadata);
    List<ParsingResult<SamplingMetadata>> mockedResults = Arrays.asList(mockedMatchingResult, mockedNonMatchingResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 25, 00, 00, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    // Correct Results should come first.
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(3);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("matching.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("nonMatching.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("false");

    // Error Results should come last.
    TableModel errorsTable = resultList.get(1).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(3);
    assertThat(errorsColumnCount).isEqualTo(2);
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo("matching.gfs");
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Mocked IOException for mockedMatchingPath.");
    assertThat(errorsTable.getValue(2, 0)).isEqualTo("nonMatching.gfs");
    assertThat(errorsTable.getValue(2, 1)).isEqualTo("Mocked IOException for mockedNonMatchingPath.");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void filterStatisticsByDateTimeShouldUseFileZoneIdForFilteringWhenNoCustomZoneIdIsSpecified(String timeZoneId) throws Exception {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? ZoneId.systemDefault() : ZoneId.of(timeZoneId);

    // 05/01/2018 12:00 - 15/01/2018 16:00
    Path januaryFifthTwelveAMToJanuaryFifteenthFourPMPath = MockUtils.mockPath("januaryFifthTwelveAMToJanuaryFifteenthFourPM.gfs", false);
    SamplingMetadata januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata = mock(SamplingMetadata.class);
    when(januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata.getTimeZoneId()).thenReturn(zoneId);
    when(januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    when(januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 05, 12, 00, 00, zoneId));
    when(januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 01, 15, 04, 00, 00, zoneId)));

    // 10/01/2018 13:30 - 10/01/2018 14:30
    Path januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPath = MockUtils.mockPath("januaryTenthHalfOnePMToJanuaryTenthHalfTwoPM.gfs", false);
    SamplingMetadata januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata = mock(SamplingMetadata.class);
    when(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata.getTimeZoneId()).thenReturn(zoneId);
    when(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata.getProductVersion()).thenReturn("GemFire 7.0.2 #build 0");
    when(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 10, 13, 30, 00, zoneId));
    when(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 01, 10, 14, 30, 00, zoneId)));

    // 10/01/2018 14:30 - 13/01/2018 17:30
    Path januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMPath = MockUtils.mockPath("januaryTenthHalfTwoAMToJanuaryThirteenthHalfFive.gfs", false);
    SamplingMetadata januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata = mock(SamplingMetadata.class);
    when(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata.getTimeZoneId()).thenReturn(zoneId);
    when(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata.getProductVersion()).thenReturn("GemFire 7.0.2 #build 0");
    when(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 10, 14, 30, 00, zoneId));
    when(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 01, 13, 17, 30, 00, zoneId)));

    ParsingResult<SamplingMetadata> januaryFifthTwelveAMToJanuaryFifteenthFourPMResult = new ParsingResult<>(januaryFifthTwelveAMToJanuaryFifteenthFourPMPath, januaryFifthTwelveAMToJanuaryFifteenthFourPMMetadata);
    ParsingResult<SamplingMetadata> januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMResult = new ParsingResult<>(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMPath, januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMMetadata);
    ParsingResult<SamplingMetadata> januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMResult = new ParsingResult<>(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPath, januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPathMetadata);

    List<ParsingResult<SamplingMetadata>> mockedResults = Arrays.asList(januaryFifthTwelveAMToJanuaryFifteenthFourPMResult, januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMResult, januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    // Filter by deterministic point in time: 14/01/2018 12:05 AM
    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 14, 12, 05, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(4);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("januaryFifthTwelveAMToJanuaryFifteenthFourPM.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("januaryTenthHalfOnePMToJanuaryTenthHalfTwoPM.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("false");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("januaryTenthHalfTwoAMToJanuaryThirteenthHalfFive.gfs");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("false");
    verify(filesService, times(1)).copyFile(januaryFifthTwelveAMToJanuaryFifteenthFourPMPath, mockedMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPath, mockedNonMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMPath, mockedNonMatchingFolderPath);
    reset(filesService);

    // Filter by day and hour : 10/01/2018 14 PM
    resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 10, 14, null, null, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(4);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("januaryFifthTwelveAMToJanuaryFifteenthFourPM.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("januaryTenthHalfOnePMToJanuaryTenthHalfTwoPM.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("januaryTenthHalfTwoAMToJanuaryThirteenthHalfFive.gfs");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("true");
    verify(filesService, times(1)).copyFile(januaryFifthTwelveAMToJanuaryFifteenthFourPMPath, mockedMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPath, mockedMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMPath, mockedMatchingFolderPath);
    reset(filesService);

    // Filter by day only: 13/01/2018
    resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 13, null, null, null, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(4);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("januaryFifthTwelveAMToJanuaryFifteenthFourPM.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("januaryTenthHalfOnePMToJanuaryTenthHalfTwoPM.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("false");
    assertThat(resultTableModel.getValue(3, 0)).isEqualTo("januaryTenthHalfTwoAMToJanuaryThirteenthHalfFive.gfs");
    assertThat(resultTableModel.getValue(3, 1)).isEqualTo("true");
    verify(filesService, times(1)).copyFile(januaryFifthTwelveAMToJanuaryFifteenthFourPMPath, mockedMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfOnePMToJanuaryTenthHalfTwoPMPath, mockedNonMatchingFolderPath);
    verify(filesService, times(1)).copyFile(januaryTenthHalfTwoPMToJanuaryThirteenthHalfFivePMPath, mockedMatchingFolderPath);
    reset(filesService);
  }

  @Test
  public void filterStatisticsByDateTimeShouldUseCustomZoneIdForFilteringWhenSpecified() throws Exception {
    ZoneId dublinZoneId = ZoneId.of("Europe/Dublin");
    ZoneId chicagoZoneId = ZoneId.of("America/Chicago");
    ZoneId filterZoneId = ZoneId.of("America/Argentina/Buenos_Aires");

    // 05/01/2018 14:00 - 07/01/2018 01:00 [Dublin]
    // 05/01/2018 10:00 - 06/01/2018 21:00 [Buenos_Aires]
    Path dublinServerPath = MockUtils.mockPath("dublinServer.gfs", false);
    SamplingMetadata dublinServerMetadata = mock(SamplingMetadata.class);
    when(dublinServerMetadata.getTimeZoneId()).thenReturn(dublinZoneId);
    when(dublinServerMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    when(dublinServerMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 05, 14, 00, 00, dublinZoneId));
    when(dublinServerMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 01, 07, 01, 00, 00, dublinZoneId)));

    // 05/01/2018 06:00 - 06/01/2018 23:00 [Chicago]
    // 05/01/2018 08:00 - 07/01/2018 01:00 [Buenos_Aires]
    Path chicagoLocatorPath = MockUtils.mockPath("chicagoLocator.gfs", false);
    SamplingMetadata chicagoLocatorMetadata = mock(SamplingMetadata.class);
    when(chicagoLocatorMetadata.getTimeZoneId()).thenReturn(chicagoZoneId);
    when(chicagoLocatorMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    when(chicagoLocatorMetadata.getStartTimeStamp()).thenReturn(MockUtils.mockTimeStamp(2018, 01, 05, 06, 00, 00, chicagoZoneId));
    when(chicagoLocatorMetadata.getFinishTimeStamp()).thenReturn((MockUtils.mockTimeStamp(2018, 01, 06, 23, 00, 00, chicagoZoneId)));

    ParsingResult<SamplingMetadata> dublinServerResult = new ParsingResult<>(dublinServerPath, dublinServerMetadata);
    ParsingResult<SamplingMetadata> chicagoLocatorResult = new ParsingResult<>(chicagoLocatorPath, chicagoLocatorMetadata);
    List<ParsingResult<SamplingMetadata>> mockedResults = Arrays.asList(dublinServerResult, chicagoLocatorResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    // Filter by deterministic point in time: 06/01/2018 12:05 AM [America/Buenos_Aires]
    Object resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 06, 12, 05, 00, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, filterZoneId);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(3);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("dublinServer.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("true");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("chicagoLocator.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("true");
    verify(filesService, times(1)).copyFile(dublinServerPath, mockedMatchingFolderPath);
    verify(filesService, times(1)).copyFile(chicagoLocatorPath, mockedMatchingFolderPath);
    reset(filesService);

    // Filter by day and hour: 05/01/2018 07:30 [America/Buenos_Aires]
    resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 05, 07, 30, null, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, filterZoneId);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(3);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("dublinServer.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("false");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("chicagoLocator.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("false");
    verify(filesService, times(1)).copyFile(dublinServerPath, mockedNonMatchingFolderPath);
    verify(filesService, times(1)).copyFile(chicagoLocatorPath, mockedNonMatchingFolderPath);
    reset(filesService);

    // Filter by day only: 07/01/2018 [America/Buenos_Aires]
    resultObject = statisticsCommands.filterStatisticsByDateTime(2018, 01, 07, null, null, null, mockedSourceFolder, mockedMatchingFolder, mockedNonMatchingFolder, filterZoneId);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(3);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(2);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Matches");
    assertThat(resultTableModel.getValue(1, 0)).isEqualTo("dublinServer.gfs");
    assertThat(resultTableModel.getValue(1, 1)).isEqualTo("false");
    assertThat(resultTableModel.getValue(2, 0)).isEqualTo("chicagoLocator.gfs");
    assertThat(resultTableModel.getValue(2, 1)).isEqualTo("true");
    verify(filesService, times(1)).copyFile(dublinServerPath, mockedNonMatchingFolderPath);
    verify(filesService, times(1)).copyFile(chicagoLocatorPath, mockedMatchingFolderPath);
    reset(filesService);
  }
}
