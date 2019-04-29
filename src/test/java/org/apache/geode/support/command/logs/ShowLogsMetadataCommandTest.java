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
import java.util.Properties;
import java.util.Random;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.shell.table.Table;

import org.apache.geode.support.command.ExportableCommand;
import org.apache.geode.support.command.AbstractExportableCommandTest;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.service.TableExportService;
import org.apache.geode.support.test.assertj.TableAssert;
import org.apache.geode.support.test.mockito.MockUtils;
import org.apache.geode.support.utils.FormatUtils;

@RunWith(JUnitParamsRunner.class)
public class ShowLogsMetadataCommandTest
    extends AbstractExportableCommandTest {
  private File mockedFolderFile;
  private LogsService logsService;
  private FilesService filesService;
  private ShowLogsMetadataCommand logsCommand;

  @Override
  protected ExportableCommand getCommand() {
    return logsCommand;
  }

  @Before
  public void setUp() {
    mockedFolderFile = mock(File.class);
    Path mockedFolderPath = mock(Path.class);
    when(mockedFolderFile.toPath()).thenReturn(mockedFolderPath);
    when(mockedFolderPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedFolderPath.toAbsolutePath().toString()).thenReturn("/temp/mocked");

    logsService = mock(LogsService.class);
    filesService = mock(FilesService.class);
    exportService = mock(TableExportService.class);
    logsCommand = new ShowLogsMetadataCommand(filesService, exportService, logsService);

    super.setUp();
  }

  @Test
  @Parameters({ "true", "false" })
  public void showLogsMetadataShouldThrowExceptionWhenFileIsNotReadable(boolean intervalOnly) {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Mocked IllegalArgumentException.$");
  }

  @Test
  @Parameters({ "true", "false" })
  public void showLogsMetadataShouldPropagateExceptionsThrownByTheServiceLayer(boolean intervalOnly) {
    doThrow(new RuntimeException()).when(logsService).parseMetadata(any());
    doThrow(new RuntimeException()).when(logsService).parseInterval(any());
    assertThatThrownBy(() -> logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, null, null))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  @Parameters({ "true", "false" })
  public void showLogsMetadataShouldReturnStringWhenNoFilesAreFound(boolean intervalOnly) {
    when(logsService.parseMetadata(any())).thenReturn(Collections.emptyList());
    when(logsService.parseInterval(any())).thenReturn(Collections.emptyList());

    Object resultObject = logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("No log files found.");
  }

  @Test
  @Parameters({ "true", "false" })
  public void showLogsMetadataShouldReturnOnlyErrorTableWhenParsingFailsForAllFiles(boolean intervalOnly) {
    Path mockedUnparseablePath = MockUtils.mockPath("mockedUnparseableFile.log", false);
    ParsingResult<LogMetadata> errorResult = new ParsingResult<>(mockedUnparseablePath, new Exception("Mocked Exception"));
    List<ParsingResult<LogMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);
    when(logsService.parseInterval(any())).thenReturn(mockedResults);

    Object resultObject = logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    Table errorsResultTable = resultList.get(0);
    TableAssert.assertThat(errorsResultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsResultTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsResultTable).row(1).isEqualTo("mockedUnparseableFile.log", "Mocked Exception");
  }

  @Test
  public void showLogsMetadataShouldReturnErrorTableAndIgnoreExportFileWhenParsingFailsForAllFilesAndExportFileIsSet() throws IOException {
    Path mockedPath = MockUtils.mockPath("mockedUnparseableFile.log", false);
    ParsingResult<LogMetadata> errorResult = new ParsingResult<>(mockedPath, new Exception("Mocked Exception"));
    List<ParsingResult<LogMetadata>> mockedResults = Collections.singletonList(errorResult);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);

    Object resultObject = logsCommand.showLogsMetadata(mockedFolderFile, false, null, mockedExportFile);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    assertThat(((List)resultObject).size()).isEqualTo(1);
    verify(exportService, times(0)).export(any(), any(), any());
  }

  @Test
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai, false" })
  public void showLogsMetadataShouldReturnOnlyMetadataTableWhenParsingSucceedsForAllFiles(String timeZoneId, boolean intervalOnly) {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    LogMetadata mockedMetadata = mock(LogMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("9.4.0");
    ParsingResult<LogMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.log", false), mockedMetadata);
    List<ParsingResult<LogMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);
    when(logsService.parseInterval(any())).thenReturn(mockedResults);

    Object fullMetadataResultObject = logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, zoneId, null);
    assertThat(fullMetadataResultObject).isNotNull();
    assertThat(fullMetadataResultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Table> fullMetadataResultList = (List)fullMetadataResultObject;
    assertThat(fullMetadataResultList.size()).isEqualTo(1);
    Table fullMetadataResultTable = fullMetadataResultList.get(0);
    TableAssert.assertThat(fullMetadataResultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(6);
    TableAssert.assertThat(fullMetadataResultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);
  }

  @Test
  @Parameters({ "true", "false" })
  public void showLogsMetadataShouldReturnMetadataTableAndExportResultMessageWhenParsingSucceedsForAllFilesAndExportFileIsSet(boolean exportSucceeds) throws IOException {
    String zoneIdDesc = FormatUtils.formatTimeZoneId(null);
    LogMetadata mockedMetadata = mock(LogMetadata.class);
    when(mockedMetadata.getTimeZoneId()).thenReturn(ZoneId.systemDefault());
    when(mockedMetadata.getProductVersion()).thenReturn("9.4.0");
    ParsingResult<LogMetadata> correctResult = new ParsingResult<>(MockUtils.mockPath("temporal.log", false), mockedMetadata);
    List<ParsingResult<LogMetadata>> mockedResults = Collections.singletonList(correctResult);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);
    setExportServiceAnswer(exportSucceeds);

    Object fullMetadataResultObject = logsCommand.showLogsMetadata(mockedFolderFile, false, null, mockedExportFile);
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
  @Parameters({ ",true", ",false", "Australia/Sydney,true", "Australia/Sydney,false", "America/Argentina/Buenos_Aires,true", "America/Argentina/Buenos_Aires,false", "Asia/Shanghai,true", "Asia/Shanghai, false" })
  public void showLogsMetadataShouldReturnErrorAndMetadataTablesInOrder(String timeZoneId, boolean intervalOnly) {
    Path regularFile1 = MockUtils.mockPath("/temp/mocked/regularFile1", false);
    Path regularFile2 = MockUtils.mockPath("/temp/mocked/regularFile2", false);
    Path unparseableFile1 = MockUtils.mockPath("/temp/mocked/unparseableFile1", false);
    Path unparseableFile2 = MockUtils.mockPath("/temp/mocked/unparseableFile2", false);

    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    List<String> timeZoneIds = new ArrayList<>(ZoneId.getAvailableZoneIds());
    ZoneId defaultTimeZone = ZoneId.systemDefault();
    ZoneId nonDefaultTimeZone = ZoneId.of(timeZoneIds.get(new Random().nextInt(timeZoneIds.size())));

    LogMetadata metadata1 = LogMetadata.of("/regularFile1", defaultTimeZone, 1L, 1L, "productVersion1", "operatingSystem1", new Properties());
    LogMetadata metadata2 = LogMetadata.of("/regularFile2", nonDefaultTimeZone, 2L, 2L, "productVersion2", "operatingSystem2", new Properties());
    List<LogMetadata> mockedMetadata = Arrays.asList(metadata1, metadata2);
    ParsingResult<LogMetadata> correctResult1 = new ParsingResult<>(regularFile1, mockedMetadata.get(0));
    ParsingResult<LogMetadata> correctResult2 = new ParsingResult<>(regularFile2, mockedMetadata.get(1));

    List<Exception> mockedExceptions = Arrays.asList(new Exception("Mocked Exception1"), new RuntimeException("Mocked RuntimeException2"));
    ParsingResult<LogMetadata> errorResult1 = new ParsingResult<>(unparseableFile1, mockedExceptions.get(0));
    ParsingResult<LogMetadata> errorResult2 = new ParsingResult<>(unparseableFile2, mockedExceptions.get(1));

    List<ParsingResult<LogMetadata>> mockedResults = Arrays.asList(correctResult1, correctResult2, errorResult1, errorResult2);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);
    when(logsService.parseInterval(any())).thenReturn(mockedResults);

    Object resultObject = logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, zoneId, null);
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
      LogMetadata expectedRowData = mockedMetadata.get(row - 1);
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
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(3).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    for (int row = 1; row < errorsRowCount; row++) {
      Exception expectedException = mockedExceptions.get(row - 1);
      TableAssert.assertThat(errorsTable).row(row).isEqualTo("/unparseableFile" + row, expectedException.getMessage());
    }
  }

  @Test
  @Parameters({ ",true,true", ",true,false", ",false,true", ",false,false", "Asia/Shanghai,true,true", "Asia/Shanghai,true,false", "Asia/Shanghai,false,true", "Asia/Shanghai,false,false" })
  public void showLogsMetadataShouldReturnBothTablesAndExportResultMessageInOrderWhenExportFileIsSet(String timeZoneId, boolean intervalOnly, boolean exportSucceeds) throws IOException {
    Path regularFile1 = MockUtils.mockPath("/temp/mocked/regularFile1", false);
    Path unparseableFile1 = MockUtils.mockPath("/temp/mocked/unparseableFile1", false);

    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    ZoneId defaultTimeZone = ZoneId.systemDefault();

    LogMetadata metadata1 = LogMetadata.of("/regularFile1", defaultTimeZone, 1L, 1L, "productVersion1", "operatingSystem1", new Properties());
    List<LogMetadata> mockedMetadata = Collections.singletonList(metadata1);
    ParsingResult<LogMetadata> correctResult1 = new ParsingResult<>(regularFile1, mockedMetadata.get(0));

    List<Exception> mockedExceptions = Arrays.asList(new Exception("Mocked Exception1"), new RuntimeException("Mocked RuntimeException2"));
    ParsingResult<LogMetadata> errorResult1 = new ParsingResult<>(unparseableFile1, mockedExceptions.get(0));

    List<ParsingResult<LogMetadata>> mockedResults = Arrays.asList(correctResult1, errorResult1);
    setExportServiceAnswer(exportSucceeds);
    when(logsService.parseMetadata(any())).thenReturn(mockedResults);
    when(logsService.parseInterval(any())).thenReturn(mockedResults);

    Object resultObject = logsCommand.showLogsMetadata(mockedFolderFile, intervalOnly, zoneId, mockedExportFile);
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<Object> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(3);

    // Results Table should come first.
    Table resultTable = (Table) resultList.get(0);
    int rowCount = resultTable.getModel().getRowCount();
    TableAssert.assertThat(resultTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(6);
    TableAssert.assertThat(resultTable).row(0).isEqualTo("File Name", "Product Version", "Operating System", "Time Zone", "Start Time" + zoneIdDesc, "Finish Time" + zoneIdDesc);

    // Assert Row Data
    for (int row = 1; row < rowCount; row++) {
      LogMetadata expectedRowData = mockedMetadata.get(row - 1);
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
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    for (int row = 1; row < errorsRowCount; row++) {
      Exception expectedException = mockedExceptions.get(row - 1);
      TableAssert.assertThat(errorsTable).row(row).isEqualTo("/unparseableFile" + row, expectedException.getMessage());
    }

    // Export Message should be the last.
    assertExportServiceResultMessageAndInvocation(resultList, exportSucceeds);
  }
}
