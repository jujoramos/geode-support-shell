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
package org.apache.geode.support.service.logs;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.test.LogsSampleDataUtils;
import org.apache.geode.support.test.junit.TimeZoneRule;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(properties = {
    ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT_ENABLED + "=false",
    InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
})
public class LogsServiceIntegrationTest {

  @Autowired
  private LogsService logsService;

  @Rule
  public TimeZoneRule timeZoneRule = new TimeZoneRule(ZoneId.of("Europe/Dublin"));

  @Test
  public void parseIntervalShouldReturnParsingErrorsWhenFileDoestNotExist() {
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseInterval(Paths.get("nonExistingFile.log"));

    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<LogMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(NoSuchFileException.class).hasMessage("nonExistingFile.log");
  }

  @Test
  public void parseIntervalShouldReturnBothParsingErrorsAndParsingSuccessesWhenParsingSucceedsForSomeFilesAndFailsForOthers() {
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseInterval(LogsSampleDataUtils.rootFolder.toPath());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(4);

    // unknownFormat.log
    String unparseableFilePath = LogsSampleDataUtils.unknownLogPath.toString();
    ParsingResult<LogMetadata> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().orElse(null);
    assertThat(unparseableResult).isNotNull();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");

    // member_8X.log
    String member8XFilePath = LogsSampleDataUtils.member8XLogPath.toString();
    ParsingResult<LogMetadata> member8XResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(member8XFilePath)).findAny().orElse(null);
    assertThat(member8XResult).isNotNull();
    assertThat(member8XResult.isSuccess()).isTrue();
    assertThat(member8XResult.getData()).isNotNull();
    assertThat(member8XResult.getFile().toAbsolutePath().toString()).isEqualTo(member8XFilePath);
    LogMetadata member8XMetadata = member8XResult.getData();
    LogsSampleDataUtils.assertMember8XMetadata(member8XMetadata, true);

    // member_9X.log
    String member9XFilePath = LogsSampleDataUtils.member9XLogPath.toString();
    ParsingResult<LogMetadata> member9XResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(member9XFilePath)).findAny().orElse(null);
    assertThat(member9XResult).isNotNull();
    assertThat(member9XResult.isSuccess()).isTrue();
    assertThat(member9XResult.getData()).isNotNull();
    assertThat(member9XResult.getFile().toAbsolutePath().toString()).isEqualTo(member9XFilePath);
    LogMetadata member9XMetadata = member9XResult.getData();
    LogsSampleDataUtils.assertMember9XMetadata(member9XMetadata, true);

    // noHeader.log
    String noHeaderFilePath = LogsSampleDataUtils.noHeaderLogPath.toString();
    ParsingResult<LogMetadata> noHeaderResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(noHeaderFilePath)).findAny().orElse(null);
    assertThat(noHeaderResult).isNotNull();
    assertThat(noHeaderResult.isSuccess()).isTrue();
    assertThat(noHeaderResult.getData()).isNotNull();
    assertThat(noHeaderResult.getFile().toAbsolutePath().toString()).isEqualTo(noHeaderFilePath);
    LogMetadata noHeaderMetadata = noHeaderResult.getData();
    LogsSampleDataUtils.assertNoHeaderMetadata(noHeaderMetadata);
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenFileDoestNotExist() {
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseMetadata(Paths.get("nonExistingFile.log"));

    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<LogMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(NoSuchFileException.class).hasMessage("nonExistingFile.log");
  }

  @Test
  public void parseMetadataShouldReturnBothParsingErrorsAndParsingSuccessesWhenParsingSucceedsForSomeFilesAndFailsForOthers() {
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseMetadata(LogsSampleDataUtils.rootFolder.toPath());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(4);

    // unknownFormat.log
    String unparseableFilePath = LogsSampleDataUtils.unknownLogPath.toString();
    ParsingResult<LogMetadata> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().orElse(null);
    assertThat(unparseableResult).isNotNull();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");

    // member_8X.log
    String member8XFilePath = LogsSampleDataUtils.member8XLogPath.toString();
    ParsingResult<LogMetadata> member8XResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(member8XFilePath)).findAny().orElse(null);
    assertThat(member8XResult).isNotNull();
    assertThat(member8XResult.isSuccess()).isTrue();
    assertThat(member8XResult.getData()).isNotNull();
    assertThat(member8XResult.getFile().toAbsolutePath().toString()).isEqualTo(member8XFilePath);
    LogMetadata member8XMetadata = member8XResult.getData();
    LogsSampleDataUtils.assertMember8XMetadata(member8XMetadata, false);

    // member_9X.log
    String member9XFilePath = LogsSampleDataUtils.member9XLogPath.toString();
    ParsingResult<LogMetadata> member9XResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(member9XFilePath)).findAny().orElse(null);
    assertThat(member9XResult).isNotNull();
    assertThat(member9XResult.isSuccess()).isTrue();
    assertThat(member9XResult.getData()).isNotNull();
    assertThat(member9XResult.getFile().toAbsolutePath().toString()).isEqualTo(member9XFilePath);
    LogMetadata member9XMetadata = member9XResult.getData();
    LogsSampleDataUtils.assertMember9XMetadata(member9XMetadata, false);

    // noHeader.log
    String noHeaderFilePath = LogsSampleDataUtils.noHeaderLogPath.toString();
    ParsingResult<LogMetadata> noHeaderResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(noHeaderFilePath)).findAny().orElse(null);
    assertThat(noHeaderResult).isNotNull();
    assertThat(noHeaderResult.isSuccess()).isTrue();
    assertThat(noHeaderResult.getData()).isNotNull();
    assertThat(noHeaderResult.getFile().toAbsolutePath().toString()).isEqualTo(noHeaderFilePath);
    LogMetadata noHeaderMetadata = noHeaderResult.getData();
    LogsSampleDataUtils.assertNoHeaderMetadata(noHeaderMetadata);
  }
}
