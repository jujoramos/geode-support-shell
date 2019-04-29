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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.ZoneId;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.test.LogsSampleDataUtils;
import org.apache.geode.support.test.junit.TimeZoneRule;

public class Log4JParserIntegrationTest {
  private LogParser logParser;

  @Rule
  public TimeZoneRule timeZoneRule = new TimeZoneRule(ZoneId.of("Europe/Dublin"));

  @Before
  public void setUp() {
    logParser = new Log4jParser(new FilesService());
  }

  @Test
  public void parseLogFileIntervalShouldThrowExceptionWhenPathIsNull() {
    assertThatThrownBy(() -> logParser.parseLogFileInterval(null)).isInstanceOf(NullPointerException.class).hasMessage("Path can not be null.");
  }

  @Test
  public void parseLogFileIntervalShouldThrowExceptionForUnknownFormats() {
    assertThatThrownBy(() -> logParser.parseLogFileInterval(LogsSampleDataUtils.unknownLogPath)).isInstanceOf(IllegalArgumentException.class).hasMessageMatching("Log format not recognized.");
  }

  @Test
  public void parseLogFileIntervalShouldWorkCorrectly() throws IOException {
    LogsSampleDataUtils.assertNoHeaderMetadata(logParser.parseLogFileInterval(LogsSampleDataUtils.noHeaderLogPath));
    LogsSampleDataUtils.assertMember8XMetadata(logParser.parseLogFileInterval(LogsSampleDataUtils.member8XLogPath), true);
    LogsSampleDataUtils.assertMember9XMetadata(logParser.parseLogFileInterval(LogsSampleDataUtils.member9XLogPath), true);
  }

  @Test
  public void parseLogFileMetadataShouldThrowExceptionWhenPathIsNull() {
    assertThatThrownBy(() -> logParser.parseLogFileMetadata(null)).isInstanceOf(NullPointerException.class).hasMessage("Path can not be null.");
  }

  @Test
  public void parseLogFileMetadataShouldThrowExceptionForUnknownFormats() {
    assertThatThrownBy(() -> logParser.parseLogFileInterval(LogsSampleDataUtils.unknownLogPath)).isInstanceOf(IllegalArgumentException.class).hasMessageMatching("Log format not recognized.");
  }

  @Test
  public void parseLogFileMetadataShouldWorkCorrectly() throws IOException {
    LogsSampleDataUtils.assertNoHeaderMetadata(logParser.parseLogFileInterval(LogsSampleDataUtils.noHeaderLogPath));
    LogsSampleDataUtils.assertMember8XMetadata(logParser.parseLogFileMetadata(LogsSampleDataUtils.member8XLogPath), false);
    LogsSampleDataUtils.assertMember9XMetadata(logParser.parseLogFileMetadata(LogsSampleDataUtils.member9XLogPath), false);
  }
}
