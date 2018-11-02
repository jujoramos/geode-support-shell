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
package org.apache.geode.support.service.logs.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.test.mockito.MockUtils;

public abstract class AbstractLogParserTest {
  FilesService filesService;
  AbstractLogParser logParser;

  @Before
  public void setUp() {
    filesService = mock(FilesService.class);
  }

  @Test
  public void parseLogFileIntervalShouldThrowExceptionWhenPathIsNull() {
    assertThatThrownBy(() -> logParser.parseLogFileInterval(null)).isInstanceOf(NullPointerException.class).hasMessage("Path can not be null.");
  }

  @Test
  public void parseLogFileIntervalShouldPropagateExceptionsThrownByFilesService() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> logParser.parseLogFileInterval(mock(Path.class))).isInstanceOf(IllegalArgumentException.class).hasMessage("Mocked IllegalArgumentException.");
  }

  @Test
  public void parseLogFileIntervalShouldPropagateExceptionsThrownByReadFirstLine() throws IOException {
    doThrow(new IOException("Mocked IOException")).when(logParser).readFirstLine(any());
    assertThatThrownBy(() -> logParser.parseLogFileInterval(mock(Path.class))).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseLogFileIntervalShouldPropagateExceptionsThrownByReadLastLine() throws IOException {
    doReturn("").when(logParser).readFirstLine(any());
    doThrow(new IOException("Mocked IOException")).when(logParser).readLastLine(any());
    assertThatThrownBy(() -> logParser.parseLogFileInterval(mock(Path.class))).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseLogFileIntervalShouldPropagateExceptionsThrownByBuildMetadataWithIntervalOnly() throws IOException {
    doReturn("").when(logParser).readFirstLine(any());
    doReturn("").when(logParser).readLastLine(any());
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException")).when(logParser).buildMetadataWithIntervalOnly(any(), any(), any());
    assertThatThrownBy(() -> logParser.parseLogFileInterval(mock(Path.class))).isInstanceOf(IllegalArgumentException.class).hasMessage("Mocked IllegalArgumentException");
  }

  @Test
  public void parseLogFileMetadataShouldThrowExceptionWhenPathIsNull() {
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(null)).isInstanceOf(NullPointerException.class).hasMessage("Path can not be null.");
  }

  @Test
  public void parseLogFileMetadataShouldPropagateExceptionsThrownByFilesService() {
    doThrow(new IllegalArgumentException("Mocked IllegalArgumentException.")).when(filesService).assertFileReadability(any());
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(mock(Path.class))).isInstanceOf(IllegalArgumentException.class).hasMessage("Mocked IllegalArgumentException.");
  }

  @Test
  public void parseLogFileMetadataShouldPropagateExceptionsThrownByReadFirstLine() throws IOException {
    doThrow(new IOException("Mocked IOException")).when(logParser).readFirstLine(any());
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(mock(Path.class))).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseLogFileMetadataShouldPropagateExceptionsThrownByReadLastLine() throws IOException {
    doReturn("").when(logParser).readFirstLine(any());
    doThrow(new IOException("Mocked IOException")).when(logParser).readLastLine(any());
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(mock(Path.class))).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseLogFileMetadataShouldPropagateExceptionsThrownByBuildMetadata() throws IOException {
    doReturn("").when(logParser).readFirstLine(any());
    doReturn("").when(logParser).readLastLine(any());
    doThrow(new IOException("Mocked IOException")).when(logParser).readLastLine(any());
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(mock(Path.class))).isInstanceOf(IOException.class).hasMessage("Mocked IOException");
  }

  @Test
  public void parseLogFileIntervalShouldReturnTheLogMetadataWithTheIntervalCoveredOnly() throws IOException {
    Path mockedPath = MockUtils.mockPath("/temp/logFile.log", false);
    doReturn("[info 2018/04/17 15:19:48.658 IST server1 <main> tid=0x1] Startup Configuration:").when(logParser).readFirstLine(any());
    doReturn("[info 2018/04/17 15:20:45.610 IST server1 <pool-3-thread-1> tid=0x4e] Marking DistributionManager 192.168.1.7(server1:32310)<v1>:1025 as closed.").when(logParser).readLastLine(any());

    LogMetadata logMetadata = logParser.parseLogFileInterval(mockedPath);
    assertThat(logMetadata).isNotNull();
    assertThat(logMetadata.getFileName()).isEqualTo("/temp/logFile.log");
    assertThat(logMetadata.getStartTimeStamp()).isEqualTo(1523974788658L);
    assertThat(logMetadata.getFinishTimeStamp()).isEqualTo(1523974845610L);
    assertThat(logMetadata.getSystemProperties()).isNull();
    assertThat(logMetadata.getProductVersion()).isNull();
    assertThat(logMetadata.getOperatingSystem()).isNull();
  }
}
