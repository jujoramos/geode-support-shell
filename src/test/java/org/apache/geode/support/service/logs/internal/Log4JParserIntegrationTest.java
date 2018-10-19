package org.apache.geode.support.service.logs.internal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.test.LogsSampleDataUtils;

public class Log4JParserIntegrationTest {
  private LogParser logParser;

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
    LogsSampleDataUtils.assertMember8XMetadata(logParser.parseLogFileMetadata(LogsSampleDataUtils.member8XLogPath), false);
    LogsSampleDataUtils.assertMember9XMetadata(logParser.parseLogFileMetadata(LogsSampleDataUtils.member9XLogPath), false);
  }
}
