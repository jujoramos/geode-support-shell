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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
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

import org.apache.geode.support.command.AbstractExportableCommandTest;
import org.apache.geode.support.command.ExportableCommand;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.service.TableExportService;
import org.apache.geode.support.test.assertj.TableAssert;
import org.apache.geode.support.test.mockito.MockUtils;
import org.apache.geode.support.utils.FormatUtils;

@RunWith(JUnitParamsRunner.class)
public class ShowStatisticsMetadataCommandTest extends AbstractExportableCommandTest {
  private File mockedFolderFile;
  private FilesService filesService;
  private StatisticsService statisticsService;
  private ShowStatisticsMetadataCommand statisticsCommands;

  @Override
  protected ExportableCommand getCommand() {
    return statisticsCommands;
  }

  @Before
  public void setUp() {
    mockedFolderFile = mock(File.class);
    Path mockedFolderPath = mock(Path.class);
    when(mockedFolderFile.toPath()).thenReturn(mockedFolderPath);
    when(mockedFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedFolderPath.toAbsolutePath().toString()).thenReturn("/temp/mocked");

    filesService = mock(FilesService.class);
    exportService = mock(TableExportService.class);
    statisticsService = mock(StatisticsService.class);
    statisticsCommands = new ShowStatisticsMetadataCommand(filesService, exportService, statisticsService);

    super.setUp();
  }

  @Test
  public void showStatisticsMetadataShouldThrowExceptionWhenFileIsNotReadable() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> statisticsCommands.showStatisticsMetadata(mockedFolderFile, null,  null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Mocked IllegalArgumentException.$");
  }

  @Test
  public void showStatisticsMetadataShouldPropagateExceptionsThrownByTheServiceLayer() {
    doThrow(new RuntimeException()).when(statisticsService).parseMetadata(any());
    assertThatThrownBy(() -> statisticsCommands.showStatisticsMetadata(mockedFolderFile, null, null))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  public void showStatisticsMetadataShouldReturnStringWhenNoFilesAreFound() {
    when(statisticsService.parseMetadata(any())).thenReturn(Collections.emptyList());

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No statistics files found.");
  }

  @Test
  public void showStatisticsMetadataShouldReturnOnlyErrorTableWhenParsingFailsForAllFiles() {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    ParsingResult<SamplingMetadata> errorResult = new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception"));
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table errorsResultTable = resultList.get(0);
    TableAssert.assertThat(errorsResultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsResultTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsResultTable).row(1).isEqualTo("mockedUnparseableFile.gfs", "Mocked Exception");
  }

  @Test
  public void showStatisticsMetadataShouldReturnErrorTableAndIgnoreExportFileWhenParsingFailsForAllFilesAndExportFileIsSet() throws IOException {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.gfs", false);
    ParsingResult<SamplingMetadata> errorResult = new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception"));
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null, mockedExportFile);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    assertThat(((List)resultObject).size()).isEqualTo(1);
    verify(exportService, times(0)).export(any(), any(), any());
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai" })
  public void showStatisticsMetadataShouldReturnOnlyMetadataTableWhenParsingSucceedsForAllFiles(String timeZoneId) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    SamplingMetadata mockedMetadata = mock(SamplingMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    ParsingResult<SamplingMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.gfs", false), mockedMetadata);
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, zoneId, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table resultTable = resultList.get(0);
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);
  }

  @Test
  @Parameters({ "true", "false" })
  public void showStatisticsMetadataShouldReturnMetadataTableAndExportResultMessageWhenParsingSucceedsForAllFilesAndExportFileIsSet(boolean exportSucceeds) throws IOException {
    String zoneIdDesc = FormatUtils.formatTimeZoneId(null);
    SamplingMetadata mockedMetadata = mock(SamplingMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("GemFire 9.4.0 #build 0");
    ParsingResult<SamplingMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.gfs", false), mockedMetadata);
    List<ParsingResult<SamplingMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);
    setExportServiceAnswer(exportSucceeds);

    Object fullMetadataResultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, null, mockedExportFile);
    assertThat(fullMetadataResultObject).isNotNull();
    assertThat(fullMetadataResultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> fullMetadataResultList = (List) fullMetadataResultObject;
    assertThat(fullMetadataResultList.size()).isEqualTo(2);
    Table fullMetadataResultTable = (Table) fullMetadataResultList.get(0);
    TableAssert.assertThat(fullMetadataResultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(6);
    TableAssert.assertThat(fullMetadataResultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);
    assertExportServiceResultMessageAndInvocation(fullMetadataResultList, exportSucceeds);
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

    SamplingMetadata metadata1 = new SamplingMetadata("/regularFile1", 1, true, defaultTimeZone, 1L, 1L, "productVersion1", "operatingSystem1");
    SamplingMetadata metadata2 = new SamplingMetadata("/regularFile2", 2, true, nonDefaultTimeZone, 2L, 2L, "productVersion2", "operatingSystem2");
    List<SamplingMetadata> mockedMetadata = Arrays.asList(metadata1, metadata2);
    ParsingResult<SamplingMetadata> correctResult1 = new ParsingResult<>(regularFile1, mockedMetadata.get(0));
    ParsingResult<SamplingMetadata> correctResult2 = new ParsingResult<>(regularFile2, mockedMetadata.get(1));

    List<Exception> mockedExceptions = Arrays.asList(new Exception("Mocked Exception1"), new RuntimeException("Mocked RuntimeException2"), new IllegalArgumentException("Mocked IllegalArgumentException3"));
    ParsingResult<SamplingMetadata> errorResult1 = new ParsingResult<>(unparseableFile1, mockedExceptions.get(0));
    ParsingResult<SamplingMetadata> errorResult2 = new ParsingResult<>(unparseableFile2, mockedExceptions.get(1));
    ParsingResult<SamplingMetadata> errorResult3 = new ParsingResult<>(unparseableFile3, mockedExceptions.get(2));

    List<ParsingResult<SamplingMetadata>> mockedResults = Arrays.asList(correctResult1, correctResult2, errorResult1, errorResult2, errorResult3);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, zoneId, null);
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    // Results Table should come first.
    Table resultTable = resultList.get(0);
    int rowCount = resultTable.getModel().getRowCount();
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);

    // Assert Row Data
    for (int row = 1; row < rowCount; row++) {
      SamplingMetadata expectedRowData = mockedMetadata.get(row - 1);
      ZoneId statTimeZone = expectedRowData.getTimeZoneId();
      ZoneId timeZoneUsed = zoneId != null ? zoneId : expectedRowData.getTimeZoneId();
      Instant startInstant = Instant.ofEpochMilli(expectedRowData.getStartTimeStamp());
      ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, timeZoneUsed);
      Instant finishInstant = Instant.ofEpochMilli(expectedRowData.getFinishTimeStamp());
      ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, timeZoneUsed);
      TableAssert.assertThat(resultTable).row(row).isEqualTo(expectedRowData.getFileName(), expectedRowData.getProductVersion(), expectedRowData.getOperatingSystem(), statTimeZone.toString(), startTime.format(FormatUtils.getDateTimeFormatter()), finishTime.format(FormatUtils.getDateTimeFormatter()));
    }

    // Errors Table should come last.
    Table errorsTable = resultList.get(1);
    int errorsRowCount = errorsTable.getModel().getRowCount();
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(4).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    for (int row = 1; row < errorsRowCount; row++) {
      Exception expectedException = mockedExceptions.get(row - 1);
      TableAssert.assertThat(errorsTable).row(row).isEqualTo("/unparseableFile" + row, expectedException.getMessage());
    }
  }

  @Test
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai,false" })
  public void showStatisticsMetadataShouldReturnBothTablesAndExportResultMessageInOrderWhenExportFileIsSet(String timeZoneId, boolean exportSucceeds) throws IOException {
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

    SamplingMetadata metadata1 = new SamplingMetadata("/regularFile1", 1, true, defaultTimeZone, 1L, 1L, "productVersion1", "operatingSystem1");
    SamplingMetadata metadata2 = new SamplingMetadata("/regularFile2", 2, true, nonDefaultTimeZone, 2L, 2L, "productVersion2", "operatingSystem2");
    List<SamplingMetadata> mockedMetadata = Arrays.asList(metadata1, metadata2);
    ParsingResult<SamplingMetadata> correctResult1 = new ParsingResult<>(regularFile1, mockedMetadata.get(0));
    ParsingResult<SamplingMetadata> correctResult2 = new ParsingResult<>(regularFile2, mockedMetadata.get(1));

    List<Exception> mockedExceptions = Arrays.asList(new Exception("Mocked Exception1"), new RuntimeException("Mocked RuntimeException2"), new IllegalArgumentException("Mocked IllegalArgumentException3"));
    ParsingResult<SamplingMetadata> errorResult1 = new ParsingResult<>(unparseableFile1, mockedExceptions.get(0));
    ParsingResult<SamplingMetadata> errorResult2 = new ParsingResult<>(unparseableFile2, mockedExceptions.get(1));
    ParsingResult<SamplingMetadata> errorResult3 = new ParsingResult<>(unparseableFile3, mockedExceptions.get(2));

    List<ParsingResult<SamplingMetadata>> mockedResults = Arrays.asList(correctResult1, correctResult2, errorResult1, errorResult2, errorResult3);
    when(statisticsService.parseMetadata(any())).thenReturn(mockedResults);
    setExportServiceAnswer(exportSucceeds);

    Object resultObject = statisticsCommands.showStatisticsMetadata(mockedFolderFile, zoneId, mockedExportFile);
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(3);

    // Results Table should come first.
    Table resultTable = (Table) resultList.get(0);
    int rowCount = resultTable.getModel().getRowCount();
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);

    // Assert Row Data
    for (int row = 1; row < rowCount; row++) {
      SamplingMetadata expectedRowData = mockedMetadata.get(row - 1);
      ZoneId statTimeZone = expectedRowData.getTimeZoneId();
      ZoneId timeZoneUsed = zoneId != null ? zoneId : expectedRowData.getTimeZoneId();
      Instant startInstant = Instant.ofEpochMilli(expectedRowData.getStartTimeStamp());
      ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, timeZoneUsed);
      Instant finishInstant = Instant.ofEpochMilli(expectedRowData.getFinishTimeStamp());
      ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, timeZoneUsed);
      TableAssert.assertThat(resultTable).row(row).isEqualTo(expectedRowData.getFileName(), expectedRowData.getProductVersion(), expectedRowData.getOperatingSystem(), statTimeZone.toString(), startTime.format(FormatUtils.getDateTimeFormatter()), finishTime.format(FormatUtils.getDateTimeFormatter()));
    }

    // Errors Table should come afterwards.
    Table errorsTable = (Table) resultList.get(1);
    int errorsRowCount = errorsTable.getModel().getRowCount();
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(4).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    for (int row = 1; row < errorsRowCount; row++) {
      Exception expectedException = mockedExceptions.get(row - 1);
      TableAssert.assertThat(errorsTable).row(row).isEqualTo("/unparseableFile" + row, expectedException.getMessage());
    }

    // Export Message should be the last.
    assertExportServiceResultMessageAndInvocation(resultList, exportSucceeds);
  }
}
