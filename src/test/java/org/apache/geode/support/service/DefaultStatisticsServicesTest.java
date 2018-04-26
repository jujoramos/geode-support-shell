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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.apache.geode.internal.statistics.ArchiveInfo;
import org.apache.geode.internal.statistics.StatArchiveFile;
import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;
import org.apache.geode.support.test.MockUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Files.class, DefaultStatisticsService.class })
public class DefaultStatisticsServicesTest {
  private Path mockedRegularPath;
  private Path mockedCompressedPath;
  private Path mockedDirectoryPath;
  private Stream<Path> mockedDirectoryStream;

  @Spy
  private DefaultStatisticsService statisticsService = new DefaultStatisticsService();

  @Before
  public void setUp() {
    mockedDirectoryPath = MockUtils.mockPath("/mockedDirectory", true);
    mockedRegularPath = MockUtils.mockPath("/mockedDirectory/mockedFile.gfs", false);
    mockedCompressedPath = MockUtils.mockPath("/mockedDirectory/mockedFile.gz", false);

    mockedDirectoryStream = Stream.of(mockedRegularPath, mockedCompressedPath);
    PowerMockito.mockStatic(Files.class);
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenFilesCanNotBeTraversed() throws Exception {
    when(Files.walk(mockedDirectoryPath)).thenThrow(new IOException("Mocked IOException"));
    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<StatisticFileMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(IOException.class).hasMessageMatching("^Mocked IOException$");
  }

  @Test
  public void parseMetadataShouldParseBothRegularAndCompressedFiles() throws Exception {
    when(Files.isRegularFile(mockedRegularPath)).thenReturn(true);
    when(Files.isRegularFile(mockedCompressedPath)).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
    doReturn(mock(StatisticFileMetadata.class)).when(statisticsService).parseFileMetadata(any());

    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    verify(statisticsService, times(2)).parseFileMetadata(any());

    parsingResults.stream().forEach(
        parsingResult -> {
          assertThat(parsingResult.isSuccess()).isTrue();
          assertThat(parsingResult.getData()).isNotNull();
        }
    );
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenCreateStatArchiveFileFails() throws Exception {
    when(Files.isRegularFile(mockedRegularPath)).thenReturn(true);
    when(Files.isRegularFile(mockedCompressedPath)).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
    doThrow(new IOException("Mocked Exception.")).when(statisticsService).createStatArchiveFile(any(), any());

    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<StatisticFileMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isFalse();
    assertThat(parsingResult1.getException()).isNotNull();
    assertThat(parsingResult1.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("^There was a problem while parsing file (.*).$");

    ParsingResult<StatisticFileMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isFalse();
    assertThat(parsingResult2.getException()).isNotNull();
    assertThat(parsingResult2.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("^There was a problem while parsing file (.*).$");
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenParseStatisticFileMetadataFails() throws Exception {
    when(Files.isRegularFile(mockedRegularPath)).thenReturn(true);
    when(Files.isRegularFile(mockedCompressedPath)).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
    doReturn(mock(StatArchiveFile.class)).when(statisticsService).createStatArchiveFile(any(), any());
    doThrow(new IOException("Mocked Exception.")).when(statisticsService).parseStatisticFileMetadata(any(), any());

    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    ParsingResult<StatisticFileMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isFalse();
    assertThat(parsingResult1.getException()).isNotNull();
    assertThat(parsingResult1.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("^There was a problem while parsing file (.*).$");

    ParsingResult<StatisticFileMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isFalse();
    assertThat(parsingResult2.getException()).isNotNull();
    assertThat(parsingResult2.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("^There was a problem while parsing file (.*).$");
  }

  @Test
  public void parseMetadataShouldReturnBothFailedAndSucceededResults() throws Exception {
    when(Files.isRegularFile(mockedRegularPath)).thenReturn(true);
    when(Files.isRegularFile(mockedCompressedPath)).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
    doReturn(mock(StatisticFileMetadata.class)).when(statisticsService).parseFileMetadata(mockedRegularPath);
    doThrow(new RuntimeException("Mocked Exception.")).when(statisticsService).parseFileMetadata(mockedCompressedPath);

    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    verify(statisticsService, times(2)).parseFileMetadata(any());

    ParsingResult<StatisticFileMetadata> succeededResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gfs")).findAny().get();
    assertThat(succeededResult.isSuccess()).isTrue();
    assertThat(succeededResult.getData()).isNotNull();

    ParsingResult<StatisticFileMetadata> failedResult = parsingResults.stream().filter(result -> result.getFile().toFile().getName().endsWith(".gz")).findAny().get();
    assertThat(failedResult.isSuccess()).isFalse();
    assertThat(failedResult.getException()).isNotNull();
    assertThat(failedResult.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("Mocked Exception.");
  }

  @Test
  public void parseMetadataShouldReturnCorrectlyForMultipleFiles() throws Exception {
    /* Common Mocked Data */
    String[] timeZoneIds = TimeZone.getAvailableIDs();
    TimeZone defaultTimeZone = TimeZone.getDefault();
    TimeZone nonDefaultTimeZone = TimeZone.getTimeZone(timeZoneIds[new Random().nextInt(timeZoneIds.length)]);

    Calendar currentTime = Calendar.getInstance();
    Calendar startOfWeek = Calendar.getInstance();
    startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
    Calendar endOfWeek = Calendar.getInstance();
    endOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
    long[] unorderedTimeStamps = new long[] { currentTime.getTimeInMillis(), endOfWeek.getTimeInMillis(), startOfWeek.getTimeInMillis() };

    StatArchiveReader.StatValue mockValue = mock(StatArchiveReader.StatValue.class);
    when(mockValue.getRawAbsoluteTimeStamps()).thenReturn(unorderedTimeStamps);
    StatArchiveReader.ResourceInst mockResourceInstance = mock(StatArchiveReader.ResourceInst.class);
    when(mockResourceInstance.isLoaded()).thenReturn(true);
    when(mockResourceInstance.getStatValue(any())).thenReturn(mockValue);
    when(statisticsService.isSearchedResourceInstance(any())).thenReturn(p -> true);

    /* Mock for Regular File */
    StatArchiveFile regularStatArchiveFile = mock(StatArchiveFile.class, "Regular");

    ArchiveInfo regularArchiveInfo = mock(ArchiveInfo.class);
    when(regularArchiveInfo.getArchiveFileName()).thenReturn("mockedFile.gfs");
    when(regularArchiveInfo.getArchiveFormatVersion()).thenReturn(1);
    when(regularArchiveInfo.isCompressed()).thenReturn(false);
    when(regularArchiveInfo.getTimeZone()).thenReturn(defaultTimeZone);
    when(regularArchiveInfo.getProductVersion()).thenReturn("Geode-1.0");
    when(regularArchiveInfo.getOs()).thenReturn("Linux 2.6.32-696.el6.x86_64");

    doReturn(regularStatArchiveFile).when(statisticsService).createStatArchiveFile(eq(mockedRegularPath), anyList());
    doReturn(true).when(regularStatArchiveFile).update(anyBoolean());
    when(regularStatArchiveFile.getArchiveInfo()).thenReturn(regularArchiveInfo);
    when(regularStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { mockResourceInstance });

    /* Mock for Compressed File */
    StatArchiveFile compressedStatArchiveFile = mock(StatArchiveFile.class, "Compressed");

    ArchiveInfo compressedArchiveInfo = mock(ArchiveInfo.class);
    when(compressedArchiveInfo.getArchiveFileName()).thenReturn("mockedFile.gz");
    when(compressedArchiveInfo.getArchiveFormatVersion()).thenReturn(2);
    when(compressedArchiveInfo.isCompressed()).thenReturn(true);
    when(compressedArchiveInfo.getTimeZone()).thenReturn(nonDefaultTimeZone);
    when(compressedArchiveInfo.getProductVersion()).thenReturn("Geode-2.0");
    when(compressedArchiveInfo.getOs()).thenReturn("Windows 2000SR3");

    doReturn(compressedStatArchiveFile).when(statisticsService).createStatArchiveFile(eq(mockedCompressedPath), anyList());
    doReturn(true).when(compressedStatArchiveFile).update(anyBoolean());
    when(compressedStatArchiveFile.getArchiveInfo()).thenReturn(compressedArchiveInfo);
    when(compressedStatArchiveFile.getResourceInstancesTable()).thenReturn(new StatArchiveReader.ResourceInst[] { mockResourceInstance });

    /* Mock for Non Matching File */
    Path mockedIgnoredPath = mock(Path.class);
    File mockedIgnoredFile = mock(File.class);
    when(mockedIgnoredFile.getName()).thenReturn("mockedIgnored.txt");
    when(mockedIgnoredPath.toFile()).thenReturn(mockedIgnoredFile);
    when(mockedIgnoredPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedIgnoredPath.toAbsolutePath().toString()).thenReturn("/mockedDirectory/mockedIgnored.txt");

    /* Mock for Unparseable File */
    Path mockedUnparseablePath = mock(Path.class);
    File mockedUnparseableFile = mock(File.class);
    when(mockedUnparseableFile.getName()).thenReturn("mockedUnparseable.gfs");
    when(mockedUnparseablePath.toFile()).thenReturn(mockedUnparseableFile);
    when(mockedUnparseablePath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedUnparseablePath.toAbsolutePath().toString()).thenReturn("/mockedDirectory/mockedUnparseable.gfs");

    StatArchiveFile unparseableStatArchiveFile = mock(StatArchiveFile.class);
    doReturn(unparseableStatArchiveFile).when(statisticsService).createStatArchiveFile(eq(mockedUnparseablePath), anyList());
    doThrow(new IOException("Mocked IOException.")).when(unparseableStatArchiveFile).update(anyBoolean());

    when(Files.isRegularFile(mockedIgnoredPath)).thenReturn(true);
    when(Files.isRegularFile(mockedRegularPath)).thenReturn(true);
    when(Files.isRegularFile(mockedCompressedPath)).thenReturn(true);
    when(Files.isRegularFile(mockedUnparseablePath)).thenReturn(true);
    Stream<Path> mockedStream = Stream.of(mockedIgnoredPath, mockedRegularPath, mockedCompressedPath, mockedUnparseablePath);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedStream);

    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(mockedDirectoryPath);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(3);

    ParsingResult<StatisticFileMetadata> regularStatFileResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().endsWith("mockedFile.gfs")).findAny().get();
    assertThat(regularStatFileResult.isSuccess()).isTrue();
    assertThat(regularStatFileResult.getData()).isNotNull();
    assertThat(regularStatFileResult.getFile()).isEqualTo(mockedRegularPath);
    StatisticFileMetadata regularArchiveMetadata = regularStatFileResult.getData();
    assertThat(regularArchiveMetadata.getFileName()).isEqualTo(mockedRegularPath.getFileName().toString());
    assertThat(regularArchiveMetadata.getVersion()).isEqualTo(1);
    assertThat(regularArchiveMetadata.getTimeZoneId()).isEqualTo(defaultTimeZone.toZoneId());
    assertThat(regularArchiveMetadata.isCompressed()).isFalse();
    assertThat(regularArchiveMetadata.getStartTimeStamp()).isEqualTo(startOfWeek.getTimeInMillis());
    assertThat(regularArchiveMetadata.getFinishTimeStamp()).isEqualTo(endOfWeek.getTimeInMillis());
    assertThat(regularArchiveMetadata.getProductVersion()).isEqualTo("Geode-1.0");
    assertThat(regularArchiveMetadata.getOperatingSystem()).isEqualTo("Linux 2.6.32-696.el6.x86_64");

    ParsingResult<StatisticFileMetadata> compressedStatFileResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().endsWith("mockedFile.gz")).findAny().get();
    assertThat(compressedStatFileResult.isSuccess()).isTrue();
    assertThat(compressedStatFileResult.getData()).isNotNull();
    assertThat(compressedStatFileResult.getFile()).isEqualTo(mockedCompressedPath);
    StatisticFileMetadata compressedArchiveMetadata = compressedStatFileResult.getData();
    assertThat(compressedArchiveMetadata.getFileName()).isEqualTo(mockedCompressedPath.getFileName().toString());
    assertThat(compressedArchiveMetadata.getVersion()).isEqualTo(2);
    assertThat(compressedArchiveMetadata.getTimeZoneId()).isEqualTo(nonDefaultTimeZone.toZoneId());
    assertThat(compressedArchiveMetadata.isCompressed()).isTrue();
    assertThat(compressedArchiveMetadata.getStartTimeStamp()).isEqualTo(startOfWeek.getTimeInMillis());
    assertThat(compressedArchiveMetadata.getFinishTimeStamp()).isEqualTo(endOfWeek.getTimeInMillis());
    assertThat(compressedArchiveMetadata.getProductVersion()).isEqualTo("Geode-2.0");
    assertThat(compressedArchiveMetadata.getOperatingSystem()).isEqualTo("Windows 2000SR3");

    ParsingResult<StatisticFileMetadata> failedResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().endsWith("mockedUnparseable.gfs")).findAny().get();
    assertThat(failedResult.isSuccess()).isFalse();
    assertThat(failedResult.getException()).isNotNull();
    assertThat(failedResult.getException()).isInstanceOf(RuntimeException.class).hasMessageMatching("^There was a problem while parsing file (.*).$");
  }
}
