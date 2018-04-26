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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.test.MockUtils;
import org.apache.geode.support.utils.FormatUtils;

@RunWith(JUnitParamsRunner.class)
public class ShowStatisticsMetadataCommandTest {
  private File mockedFolderFile;
  private FilesService filesService;
  private StatisticsService statisticsService;
  private ShowStatisticsMetadataCommand statisticsCommands;

  @Before
  public void setUp() {
    mockedFolderFile = mock(File.class);
    Path mockedFolderPath = mock(Path.class);
    when(mockedFolderFile.toPath()).thenReturn(mockedFolderPath);
    when(mockedFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedFolderPath.toAbsolutePath().toString()).thenReturn("/temp/mocked");

    filesService = mock(FilesService.class);
    statisticsService = mock(StatisticsService.class);
    statisticsCommands = new ShowStatisticsMetadataCommand(filesService, statisticsService);
  }

  @Test
  public void showStatisticsMetadataShouldThrowExceptionWhenFileIsNotReadable() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> statisticsCommands.showStatisticsMetadata(mockedFolderFile, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Mocked IllegalArgumentException.$");
  }

  @Test
  public void showStatisticsMetadataShouldPropagateExceptionsThrownByTheServiceLayer() {
    doThrow(new RuntimeException()).when(statisticsService).parseMetadata(any());
    assertThatThrownBy(() -> statisticsCommands.showStatisticsMetadata(mockedFolderFile, null))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void showStatisticsMetadataShouldReturnStringWhenNoFilesAreFound() {
    when(statisticsService.parseMetadata(any())).thenReturn(Collections.emptyList());

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null);

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  public void showStatisticsMetadataShouldReturnOnlyErrorTableIfParsingFailsForAllFiles() {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    ParsingResult<StatisticFileMetadata> errorResult = new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception"));
    List<ParsingResult<StatisticFileMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null);

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
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnOnlyMetadataTableIfParsingSucceedsForAllFiles(String timeZoneId) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    StatisticFileMetadata mockedMetadata = mock(StatisticFileMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    ParsingResult<StatisticFileMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.gfs", false), mockedMetadata);
    List<ParsingResult<StatisticFileMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, zoneId);

    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    assertThat(resultTable).isNotNull();
    TableModel resultTableModel = resultTable.getModel();
    assertThat(resultTableModel.getRowCount()).isEqualTo(2);
    assertThat(resultTableModel.getColumnCount()).isEqualTo(6);
    assertThat(resultTableModel.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultTableModel.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultTableModel.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultTableModel.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultTableModel.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultTableModel.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnErrorAndMetadataTablesInOrder(String timeZoneId) {
    Path regularFile1 = MockUtils.mockPath("/temp/mocked/regularFile1", false);
    Path regularFile2 = MockUtils.mockPath("/temp/mocked/regularFile2", false);
    Path unparseableFile1 = MockUtils.mockPath("/temp/mocked/unparseableFile1", false);
    Path unparseableFile2 = MockUtils.mockPath("/temp/mocked/unparseableFile2", false);
    Path unparseableFile3 = MockUtils.mockPath("/temp/mocked/unparseableFile3", false);

    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    List<String> timeZoneIds = new ArrayList<>(ZoneId.getAvailableZoneIds());
    ZoneId defaultTimeZone = ZoneId.systemDefault();
    ZoneId nonDefaultTimeZone = ZoneId.of(timeZoneIds.get(new Random().nextInt(timeZoneIds.size())));

    StatisticFileMetadata metadata1 = new StatisticFileMetadata("/regularFile1", 1, true, defaultTimeZone, 1L, 1L, "productVersion1", "operatingSystem1");
    StatisticFileMetadata metadata2 = new StatisticFileMetadata("/regularFile2", 2, true, nonDefaultTimeZone, 2L, 2L, "productVersion2", "operatingSystem2");
    List<StatisticFileMetadata> mockedMetadata = Arrays.asList(metadata1, metadata2);
    ParsingResult<StatisticFileMetadata> correctResult1 = new ParsingResult<>(regularFile1, mockedMetadata.get(0));
    ParsingResult<StatisticFileMetadata> correctResult2 = new ParsingResult<>(regularFile2, mockedMetadata.get(1));

    List<Exception> mockedExceptions = Arrays.asList(new Exception("Mocked Exception1"), new RuntimeException("Mocked RuntimeException2"), new IllegalArgumentException("Mocked IllegalArgumentException3"));
    ParsingResult<StatisticFileMetadata> errorResult1 = new ParsingResult<>(unparseableFile1, mockedExceptions.get(0));
    ParsingResult<StatisticFileMetadata> errorResult2 = new ParsingResult<>(unparseableFile2, mockedExceptions.get(1));
    ParsingResult<StatisticFileMetadata> errorResult3 = new ParsingResult<>(unparseableFile3, mockedExceptions.get(2));

    List<ParsingResult<StatisticFileMetadata>> mockedResults = Arrays.asList(correctResult1, correctResult2, errorResult1, errorResult2, errorResult3);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, zoneId);
    assertThat(resultObject).isInstanceOf(List.class);
    List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    // Correct Results should come first.
    TableModel resultsTable = resultList.get(0).getModel();
    int rowCount = resultsTable.getRowCount();
    int columnCount = resultsTable.getColumnCount();
    assertThat(rowCount).isEqualTo(3);
    assertThat(columnCount).isEqualTo(6);

    // Assert Titles
    assertThat(resultsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(resultsTable.getValue(0, 1)).isEqualTo("Product Version");
    assertThat(resultsTable.getValue(0, 2)).isEqualTo("Operating System");
    assertThat(resultsTable.getValue(0, 3)).isEqualTo("Time Zone");
    assertThat(resultsTable.getValue(0, 4)).isEqualTo("Start Time" + zoneIdDesc);
    assertThat(resultsTable.getValue(0, 5)).isEqualTo("Finish Time" + zoneIdDesc);

    // Assert Row Data
    for (int row = 1; row < rowCount; row++) {
      StatisticFileMetadata expectedRowData = mockedMetadata.get(row - 1);
      ZoneId statTimeZone = expectedRowData.getTimeZoneId();
      ZoneId timeZoneUsed = zoneId != null ? zoneId : expectedRowData.getTimeZoneId();

      assertThat(resultsTable.getValue(row, 0)).isEqualTo(expectedRowData.getFileName());
      assertThat(resultsTable.getValue(row, 1)).isEqualTo(expectedRowData.getProductVersion());
      assertThat(resultsTable.getValue(row, 2)).isEqualTo(expectedRowData.getOperatingSystem());
      assertThat(resultsTable.getValue(row, 3)).isEqualTo(statTimeZone.toString());

      Instant startInstant = Instant.ofEpochMilli(expectedRowData.getStartTimeStamp());
      ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, timeZoneUsed);
      assertThat(resultsTable.getValue(row, 4)).isEqualTo(startTime.format(FormatUtils.getDateTimeFormatter()));

      Instant finishInstant = Instant.ofEpochMilli(expectedRowData.getFinishTimeStamp());
      ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, timeZoneUsed);
      assertThat(resultsTable.getValue(row, 5)).isEqualTo(finishTime.format(FormatUtils.getDateTimeFormatter()));
    }

    // Error Results should come last.
    TableModel errorsTable = resultList.get(1).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(4);
    assertThat(errorsColumnCount).isEqualTo(2);

    // Assert Titles
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");

    for (int row = 1; row < errorsRowCount; row++) {
      Exception expectedException = mockedExceptions.get(row - 1);
      assertThat(errorsTable.getValue(row, 0)).isEqualTo("/unparseableFile" + row);
      assertThat(errorsTable.getValue(row, 1)).isEqualTo(expectedException.getMessage());
    }
  }
}
