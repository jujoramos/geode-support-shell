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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.shell.table.Table;

import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.test.mockito.MockUtils;
import org.apache.geode.support.test.mockito.ResultCaptor;
import org.apache.geode.support.test.assertj.TableAssert;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnitParamsRunner.class)
@PrepareForTest({ Files.class, StartVisualStatisticsDisplayCommand.class })
public class StartVisualStatisticsDisplayCommandTest {
  private Path mockedVsdHome;
  private Path vsdExecutablePath;
  private Path mockedRegularPath;
  private Path mockedCompressedPath;
  private Path mockedRootDirectoryPath;
  private Path mockedRootDecompressedPath;

  private FilesService filesService;
  private StatisticsService statisticsService;
  private StartVisualStatisticsDisplayCommand startVisualStatisticsDisplayCommand;

  @Before
  public void setUp() {
    mockedVsdHome = MockUtils.mockPath("/apps/vsd", true);
    vsdExecutablePath = MockUtils.mockPath("/apps/vsd/bin/vsd", false);
    mockedRootDirectoryPath = MockUtils.mockPath("/statistics", true);
    mockedRootDecompressedPath = MockUtils.mockPath("/decompressed", true);
    mockedRegularPath = MockUtils.mockPath("/statistics/regular.gfs", false);
    mockedCompressedPath = MockUtils.mockPath("/statistics/compressed.gz", false);

    filesService = mock(FilesService.class);
    statisticsService = mock(StatisticsService.class);
    startVisualStatisticsDisplayCommand = spy(new StartVisualStatisticsDisplayCommand(filesService, statisticsService));

    PowerMockito.mockStatic(Files.class);
  }

  @Test
  public void resolveVsdExecutablePathShouldThrowExceptionWhenVdsHomeCanNotBeFound() {
    assertThatThrownBy(() -> startVisualStatisticsDisplayCommand.resolveVsdExecutablePath(null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Visual Statistics Display Tool (VSD) can not be found.");
  }

  @Test
  public void resolveVsdExecutablePathShouldThrowExceptionWhenVsdRootPathIsNotReadableOrNotExecutable() {
    doThrow(new IllegalArgumentException("Folder not readable.")).when(filesService).assertFolderReadability(any());
    assertThatThrownBy(() -> startVisualStatisticsDisplayCommand.resolveVsdExecutablePath(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Folder not readable.");

    doNothing().when(filesService).assertFolderReadability(any());
    doThrow(new IllegalArgumentException("File not executable.")).when(filesService).assertFileExecutability(any());
    assertThatThrownBy(() -> startVisualStatisticsDisplayCommand.resolveVsdExecutablePath("/tmp"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File not executable.");

    doNothing().when(filesService).assertFolderReadability(any());
    doNothing().when(filesService).assertFileExecutability(any());
    assertThatCode(() -> startVisualStatisticsDisplayCommand.resolveVsdExecutablePath("/tmp")).doesNotThrowAnyException();
  }

  @Test
  @Parameters( { "Windows", "Other" })
  public void resolveVsdExecutablePathShouldApplyTheCorrectPriorityOrder(String os) {
    Path executablePath;
    boolean isWindows = os.equals("Windows");
    when(startVisualStatisticsDisplayCommand.isWindows()).thenReturn(isWindows);

    Path mockDefaultVsdRootPath = MockUtils.mockPath("/home/system/vsd", true);
    startVisualStatisticsDisplayCommand.setDefaultVsdHome(mockDefaultVsdRootPath.toAbsolutePath().toString());
    executablePath = startVisualStatisticsDisplayCommand.resolveVsdExecutablePath(null);
    assertThat(executablePath).isNotNull();
    assertThat(executablePath.toString()).isEqualTo("/home/system/vsd/bin/vsd" + (isWindows ? ".bat" : ""));

    Path mockVsdRootPath = MockUtils.mockPath("/home/apps/vsd", true);
    executablePath = startVisualStatisticsDisplayCommand.resolveVsdExecutablePath(mockVsdRootPath.toAbsolutePath().toString());
    assertThat(executablePath).isNotNull();
    assertThat(executablePath.toString()).isEqualTo("/home/apps/vsd/bin/vsd" + (isWindows ? ".bat" : ""));
  }

  @Test
  public void decompressShouldCorrectlyBuildDecompressedName() throws IOException {
    Path mockedPath = MockUtils.mockPath("/home/statistics/locator1.gz", false);
    Path mockedDirectory = MockUtils.mockPath("/directory", true);

    Path decompressedPath = startVisualStatisticsDisplayCommand.decompress(mockedPath, mockedDirectory);
    assertThat(decompressedPath).isNotNull();
    assertThat(decompressedPath.toAbsolutePath().toString()).isEqualTo("/directory/locator1.gfs");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void buildCommandLineShouldWorkCorrectly() {
    Object objectResult = startVisualStatisticsDisplayCommand.buildCommandLine(mockedVsdHome, Collections.emptyList(), Collections.emptyList());
    assertThat(objectResult).isNotNull().isInstanceOf(List.class);
    List listResult = (List<String>) objectResult;
    assertThat(listResult.size()).isEqualTo(1);
    assertThat(listResult.get(0)).isEqualTo(mockedVsdHome.toString());

    List<Path> regularFiles = Arrays.asList(MockUtils.mockPath("/stats/locator1.gfs", false), MockUtils.mockPath("/stats/server1.gfs", false));
    List<Path> decompressedFiles = Arrays.asList(MockUtils.mockPath("/stats/decompressed/locator2.gfs", false), MockUtils.mockPath("/stats/decompressed/server1.gfs", false));
    objectResult = startVisualStatisticsDisplayCommand.buildCommandLine(mockedVsdHome, regularFiles, decompressedFiles);
    assertThat(objectResult).isNotNull().isInstanceOf(List.class);
    listResult = (List<String>) objectResult;
    assertThat(listResult.size()).isEqualTo(5);
    assertThat(listResult.get(0)).isEqualTo(mockedVsdHome.toString());
    assertThat(listResult.get(1)).isEqualTo(regularFiles.get(0).toString());
    assertThat(listResult.get(2)).isEqualTo(regularFiles.get(1).toString());
    assertThat(listResult.get(3)).isEqualTo(decompressedFiles.get(0).toString());
    assertThat(listResult.get(4)).isEqualTo(decompressedFiles.get(1).toString());
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenVsdExecutableCanNotBeResolved() {
    doThrow(new IllegalStateException("Mocked Exception when Resolving VSD Path.")).when(startVisualStatisticsDisplayCommand).resolveVsdExecutablePath(any());
    assertThatThrownBy(() -> startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), null, null))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Mocked Exception when Resolving VSD Path.");
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenSourcePathDoesNotExist() {
    doReturn(mockedVsdHome).when(startVisualStatisticsDisplayCommand).resolveVsdExecutablePath(any());
    doThrow(new IllegalArgumentException("Mocked Exception when Asserting Path Existence.")).when(filesService).assertFileExistence(any());
    assertThatThrownBy(() -> startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), null, null, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Mocked Exception when Asserting Path Existence.");
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenSourcePathCanNotBeIterated() throws Exception {
    when(Files.walk(any())).thenThrow(new IOException("Mocked IOException when walking through source path."));

    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List)resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("There was an error while iterating through the source folder " + mockedRootDirectoryPath.toString() + ".");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void startVisualStatisticsDisplayToolShouldIgnoreCompressedFilesWhenDecompressionFolderIsNotSet() throws Exception {
    when(Files.isRegularFile(any())).thenReturn(true);
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.of(mockedRegularPath, mockedCompressedPath));
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));
    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());

    // Decompression folder not set.
    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    verify(filesService, times(0)).createDirectories(any());
    verify(statisticsService, times(0)).decompress(any(), any());
    verify(startVisualStatisticsDisplayCommand, times(0)).decompress(any(), any());
    assertThat(commandLinetCaptor.getResult()).isEqualTo(Arrays.asList(vsdExecutablePath.toString() , mockedRegularPath.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void startVisualStatisticsDisplayToolShouldIgnoreCompressedFilesWhenDecompressionFolderIsSettButNoCompressedFilesAreFound() throws Exception {
    when(Files.isRegularFile(any())).thenReturn(true);
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.of(mockedRegularPath));
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));
    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());

    // Decompression folder set, no compressed files present within the source path.
    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), mockedRootDecompressedPath.toFile(), null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    verify(filesService, times(0)).createDirectories(any());
    verify(statisticsService, times(0)).decompress(any(), any());
    verify(startVisualStatisticsDisplayCommand, times(0)).decompress(any(), any());
    assertThat(commandLinetCaptor.getResult()).isEqualTo(Arrays.asList(vsdExecutablePath.toString() , mockedRegularPath.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void startVisualStatisticsDisplayToolShouldIgnoreCompressedFilesWhenDecompressionFolderCanNotBeCreated() throws Exception {
    when(Files.isRegularFile(any())).thenReturn(true);
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.of(mockedRegularPath, mockedCompressedPath));
    doThrow(new IOException("Mocked IOException while creating decompression folder.")).when(filesService).createDirectories(mockedRootDecompressedPath);
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));
    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());

    // Decompression folder set, compressed files present within the source path but decompression folder creation fails.
    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), mockedRootDecompressedPath.toFile(), null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    @SuppressWarnings("unchecked") List<String> resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    String errorMessage = resultList.get(0);
    assertThat(errorMessage).isEqualTo("Decompression folder " + mockedRootDecompressedPath.toString() + " couldn't be created. Compressed files will be ignored.");

    String resultMessage = resultList.get(1);
    assertThat(resultMessage).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    verify(filesService, times(1)).createDirectories(mockedRootDecompressedPath);
    verify(statisticsService, times(0)).decompress(any(), any());
    verify(startVisualStatisticsDisplayCommand, times(0)).decompress(any(), any());
    assertThat(commandLinetCaptor.getResult()).isEqualTo(Arrays.asList(vsdExecutablePath.toString() , mockedRegularPath.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void startVisualStatisticsDisplayToolShouldReturnErrorsTableIfDecompressionFailsForAtLeastOneFile() throws Exception {
    when(Files.isRegularFile(any())).thenReturn(true);
    Path serverMockedCompressedPath = MockUtils.mockPath("/statistics/server.gz", false);
    Path locatorMockedCompressedPath = MockUtils.mockPath("/statistics/locator.gz", false);
    Path locatorMockedDecompressedPath = MockUtils.mockPath("/decompressed/locator.gfs", false);
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.of(mockedRegularPath, serverMockedCompressedPath, locatorMockedCompressedPath));
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));
    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());
    doThrow(new IOException("Problem while decompressing file.")).when(startVisualStatisticsDisplayCommand).decompress(serverMockedCompressedPath, mockedRootDecompressedPath);

    // Decompression folder set, compressed files present and one decompression fails.
    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(mockedRootDirectoryPath.toFile(), mockedVsdHome.toFile(), mockedRootDecompressedPath.toFile(), null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(2);

    // Error Table
    assertThat(resultList.get(0)).isInstanceOf(Table.class);
    Table errorsTable = (Table) resultList.get(0);
    TableAssert.assertThat(errorsTable).rowCountIsEqualsTo(2).columnCountIsEqualsTo(2);
    TableAssert.assertThat(errorsTable).row(0).isEqualTo("File Name", "Error Description");
    TableAssert.assertThat(errorsTable).row(1).isEqualTo("/server.gz", "Problem while decompressing file.");

    // Result String
    assertThat(resultList.get(1)).isInstanceOf(String.class);
    String resultMessage = (String) resultList.get(1);
    assertThat(resultMessage).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    verify(filesService, times(1)).createDirectories(mockedRootDecompressedPath);
    verify(startVisualStatisticsDisplayCommand, times(2)).decompress(any(), any());
    assertThat(commandLinetCaptor.getResult()).isEqualTo(Arrays.asList(vsdExecutablePath.toString() , mockedRegularPath.toString(), locatorMockedDecompressedPath.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  @Parameters({ "", "Australia/Sydney", "Asia/Shanghai" })
  public void startVisualStatisticsDisplayToolShouldSetTimeZoneEnvironmentVariable(String timeZoneId) throws Exception {
    ZoneId zoneId = StringUtils.isBlank(timeZoneId) ? null : ZoneId.of(timeZoneId);
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.empty());
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));
    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());

    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(null, mockedVsdHome.toFile(), null, zoneId);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");

    List<String> expectedCommandLine = Collections.singletonList(vsdExecutablePath.toString());
    Map<String, String> expectedEnvironment = new HashMap<>();
    if (zoneId != null) expectedEnvironment.put("TZ", zoneId.toString());

    assertThat(commandLinetCaptor.getResult()).isEqualTo(expectedCommandLine);
    verify(startVisualStatisticsDisplayCommand, times(1)).launchProcess(expectedCommandLine, expectedEnvironment);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void startVisualStatisticsDisplayToolShouldReturnWhetherVsdWasLaunchedOrNot() throws Exception {
    when(Files.walk(mockedRootDirectoryPath)).thenReturn(Stream.empty());
    ResultCaptor<List<String>> commandLinetCaptor = new ResultCaptor<>();
    doAnswer(commandLinetCaptor).when(startVisualStatisticsDisplayCommand).buildCommandLine(any(Path.class), any(List.class), any(List.class));

    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(true, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());
    Object resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(null, mockedVsdHome.toFile(), null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    List resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");

    doReturn(new StartVisualStatisticsDisplayCommand.ProcessWrapper(false, null, null, null)).when(startVisualStatisticsDisplayCommand).launchProcess(any(), any());
    resultObject = startVisualStatisticsDisplayCommand.startVisualStatisticsDisplayTool(null, mockedVsdHome.toFile(), null, null);
    assertThat(resultObject).isNotNull();
    assertThat(resultObject).isInstanceOf(List.class);
    resultList = (List) resultObject;
    assertThat(resultList.size()).isEqualTo(1);
    assertThat(resultList.get(0)).isEqualTo("There was an error while starting the VSD process, please check logs for details.");
  }
}
