package org.apache.geode.support.service.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.logs.internal.LogParser;
import org.apache.geode.support.test.mockito.MockUtils;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(JUnitParamsRunner.class)
@PrepareForTest({ Files.class, DefaultLogsService.class })
public class DefaultLogsServiceTest {
  private Path mockedFileOnePath;
  private Path mockedFileTwoPath;
  private Path mockedDirectoryPath;
  private LogParser mockedLogParser;
  private DefaultLogsService logsService;

  @Before
  public void setUp() throws IOException {
    mockedLogParser = mock(LogParser.class);
    logsService = spy(new DefaultLogsService(mockedLogParser));

    mockedDirectoryPath = MockUtils.mockPath("/mockedDirectory", true);
    mockedFileOnePath = MockUtils.mockPath("/mockedDirectory/mockedFile1.log", false);
    mockedFileTwoPath = MockUtils.mockPath("/mockedDirectory/mockedFile2.log", false);

    Stream<Path> mockedDirectoryStream = Stream.of(mockedFileOnePath, mockedFileTwoPath);
    PowerMockito.mockStatic(Files.class);

    when(Files.isRegularFile(any())).thenReturn(true);
    when(Files.walk(mockedDirectoryPath)).thenReturn(mockedDirectoryStream);
  }

  @Test
  public void isLogFileTest() {
    Path mockedPath = MockUtils.mockPath("mockFile.txt", false);

    when(Files.isRegularFile(any())).thenReturn(false);
    assertThat(logsService.isLogFile().test(mockedPath)).isFalse();
    assertThat(logsService.isLogFile().test(mockedFileOnePath)).isFalse();
    assertThat(logsService.isLogFile().test(mockedFileTwoPath)).isFalse();

    when(Files.isRegularFile(any())).thenReturn(true);
    assertThat(logsService.isLogFile().test(mockedPath)).isFalse();
    assertThat(logsService.isLogFile().test(mockedFileOnePath)).isTrue();
    assertThat(logsService.isLogFile().test(mockedFileTwoPath)).isTrue();
  }

  @Test
  public void parseSelectivelyShouldPropagateExceptionsThrownByLogParser() throws IOException {
    doThrow(new IOException("Mocked IOException Interval Only")).when(mockedLogParser).parseLogFileInterval(any());
    doThrow(new IOException("Mocked IOException Full Metadata")).when(mockedLogParser).parseLogFileMetadata(any());

    assertThatThrownBy(() -> logsService.parseSelectively(mockedFileOnePath, true)).isInstanceOf(IOException.class).hasMessage("Mocked IOException Interval Only");
    assertThatThrownBy(() -> logsService.parseSelectively(mockedFileTwoPath, false)).isInstanceOf(IOException.class).hasMessage("Mocked IOException Full Metadata");
  }

  @Test
  public void parseSelectivelyShouldExecuteTheCorrectMethod() throws Exception {
    when(mockedLogParser.parseLogFileInterval(mockedFileOnePath)).thenReturn(mock(LogMetadata.class));
    assertThat(logsService.parseSelectively(mockedFileOnePath, true)).isNotNull();
    verify(mockedLogParser, times(1)).parseLogFileInterval(mockedFileOnePath);
    verify(mockedLogParser, times(0)).parseLogFileMetadata(mockedFileOnePath);

    when(mockedLogParser.parseLogFileMetadata(mockedFileTwoPath)).thenReturn(mock(LogMetadata.class));
    assertThat(logsService.parseSelectively(mockedFileTwoPath, false)).isNotNull();
    verify(mockedLogParser, times(0)).parseLogFileInterval(mockedFileTwoPath);
    verify(mockedLogParser, times(1)).parseLogFileMetadata(mockedFileTwoPath);
  }

  @Test
  @Parameters( { "true", "false" })
  public void parseAllShouldReturnParsingErrorWhenSourcePathCanNotBeTraversed(boolean intervalOnly) throws Exception {
    when(Files.walk(mockedDirectoryPath)).thenThrow(new IOException("Mocked IOException"));
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseAll(mockedDirectoryPath, intervalOnly);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<LogMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getFile()).isNotNull();
    assertThat(parsingResult.getFile()).isEqualTo(mockedDirectoryPath);
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  @Parameters( { "true", "false" })
  public void parseAllShouldReturnOnlyParsingErrorsWhenParseSelectivelyFailsForAllFiles(boolean intervalOnly) throws Exception {
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(logsService).parseSelectively(any(), anyBoolean());
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseAll(mockedDirectoryPath, intervalOnly);

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<LogMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isFalse();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedFileOnePath);
    assertThat(parsingResult1.getException()).isNotNull();
    assertThat(parsingResult1.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");

    ParsingResult<LogMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isFalse();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedFileTwoPath);
    assertThat(parsingResult2.getException()).isNotNull();
    assertThat(parsingResult2.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }

  @Test
  @Parameters( { "true", "false" })
  public void parseAllShouldReturnOnlyParsingSuccessesWhenParseSelectivelySucceedsForAllFiles(boolean intervalOnly) throws Exception {
    LogMetadata mockedMetadata = mock(LogMetadata.class);
    doReturn(mockedMetadata).when(logsService).parseSelectively(any(), anyBoolean());

    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseAll(mockedDirectoryPath, intervalOnly);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);

    ParsingResult<LogMetadata> parsingResult1 = parsingResults.get(0);
    assertThat(parsingResult1.isSuccess()).isTrue();
    assertThat(parsingResult1.getFile()).isNotNull();
    assertThat(parsingResult1.getFile()).isEqualTo(mockedFileOnePath);
    assertThat(parsingResult1.getData()).isNotNull();

    ParsingResult<LogMetadata> parsingResult2 = parsingResults.get(1);
    assertThat(parsingResult2.isSuccess()).isTrue();
    assertThat(parsingResult2.getFile()).isNotNull();
    assertThat(parsingResult2.getFile()).isEqualTo(mockedFileTwoPath);
    assertThat(parsingResult2.getData()).isNotNull();
  }

  @Test
  @Parameters( { "true", "false" })
  public void parseAllShouldReturnBothParsingErrorsAndParsingSuccessesWhenParseSelectivelySucceedsForSomeFilesAndFailsForOthers(boolean intervalOnly) throws Exception {
    LogMetadata mockedMetadata = mock(LogMetadata.class);
    doReturn(mockedMetadata).when(logsService).parseSelectively(mockedFileOnePath, intervalOnly);
    doThrow(new IOException("Mocked Exception While Parsing File.")).when(logsService).parseSelectively(mockedFileTwoPath, intervalOnly);

    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseAll(mockedDirectoryPath, intervalOnly);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(2);
    verify(logsService, times(2)).parseSelectively(any(), anyBoolean());

    ParsingResult<LogMetadata> succeededResult = parsingResults.get(0);
    assertThat(succeededResult).isNotNull();
    assertThat(succeededResult.isSuccess()).isTrue();
    assertThat(succeededResult.getData()).isNotNull();
    assertThat(succeededResult.getFile()).isEqualTo(mockedFileOnePath);

    ParsingResult<LogMetadata> failedResult = parsingResults.get(1);
    assertThat(failedResult).isNotNull();
    assertThat(failedResult.isSuccess()).isFalse();
    assertThat(failedResult.getException()).isNotNull();
    assertThat(failedResult.getFile()).isEqualTo(mockedFileTwoPath);
    assertThat(failedResult.getException()).isInstanceOf(IOException.class).hasMessage("Mocked Exception While Parsing File.");
  }

  @Test
  public void parseIntervalShouldProperlyDelegateToTheInternalMethod() {
    doReturn(Collections.emptyList()).when(logsService).parseAll(mockedDirectoryPath, true);
    assertThat(logsService.parseInterval(mockedDirectoryPath)).isNotNull().isEmpty();
    verify(logsService, times(0)).parseAll(mockedDirectoryPath, false);
    verify(logsService, times(1)).parseAll(mockedDirectoryPath, true);
  }

  @Test
  public void parseMetadataShouldProperlyDelegateToTheInternalMethod() {
    doReturn(Collections.emptyList()).when(logsService).parseAll(mockedDirectoryPath, false);
    assertThat(logsService.parseMetadata(mockedDirectoryPath)).isNotNull().isEmpty();
    verify(logsService, times(0)).parseAll(mockedDirectoryPath, true);
    verify(logsService, times(1)).parseAll(mockedDirectoryPath, false);
  }
}
