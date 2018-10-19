package org.apache.geode.support.service.logs.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.support.test.LogsSampleDataUtils;

public class CustomLogFilePatternReceiverIntegrationTest {
  private CustomLogFilePatternReceiver logFilePatternReceiver;

  @Before
  public void setUp() {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver(""));
  }

  @Test
  public void parseLineShouldThrowExceptionWhenLineIsNull() {
    assertThatThrownBy(() -> logFilePatternReceiver.parseLine(null)).isInstanceOf(NullPointerException.class).hasMessage("Line can't be null.");
  }

  @Test
  public void parseLineShouldReturnNullWhenLineCanNotBeParsed() {
    String invalidLine = "[Level 2017-12-27T11:45:01.432 main tid=0x1] Nothing";
    Optional<LoggingEvent> finestLoggingEvent = logFilePatternReceiver.parseLine(invalidLine);
    assertThat(finestLoggingEvent).isNotNull();
    assertThat(finestLoggingEvent.isPresent()).isFalse();
  }

  @Test
  public void parseLineShouldCorrectlyParseStringIntoLoggingEvent() {
    String finestLogLine = "[finest 2017/12/27 11:45:01.432 ART main tid=0x1] (msgTID=1 msgSN=12) name=GemFire:service=System,type=Distributed, attribute=MemberObjectName";
    assertThat(logFilePatternReceiver.parseLine(finestLogLine).isPresent()).isTrue();
    LoggingEvent finestLoggingEvent = logFilePatternReceiver.parseLine(finestLogLine).get();
    assertThat(finestLoggingEvent).isNotNull();
    assertThat(finestLoggingEvent.getLevel()).isEqualTo(Level.TRACE);
    assertThat(finestLoggingEvent.getTimeStamp()).isEqualTo(1514385901432L);
    assertThat(finestLoggingEvent.getThreadName()).isEqualTo("main tid=0x1");
    assertThat(finestLoggingEvent.getMessage()).isEqualTo("(msgTID=1 msgSN=12) name=GemFire:service=System,type=Distributed, attribute=MemberObjectName");

    String fineLogLine = "[fine 2018/08/28 23:06:29.209 GMT  <Management Task> tid=0x46] putAll GemFire:type=Member,member=server3(608415)<v0>-34521 -> ObjectName = GemFire:type=Member,member=server3(608415)<v0>-34521";
    assertThat(logFilePatternReceiver.parseLine(fineLogLine).isPresent()).isTrue();
    LoggingEvent fineLoggingEvent = logFilePatternReceiver.parseLine(fineLogLine).get();
    assertThat(fineLoggingEvent).isNotNull();
    assertThat(fineLoggingEvent.getLevel()).isEqualTo(Level.TRACE);
    assertThat(fineLoggingEvent.getTimeStamp()).isEqualTo(1535497589209L);
    assertThat(fineLoggingEvent.getThreadName()).isEqualTo("<Management Task> tid=0x46");
    assertThat(fineLoggingEvent.getMessage()).isEqualTo("putAll GemFire:type=Member,member=server3(608415)<v0>-34521 -> ObjectName = GemFire:type=Member,member=server3(608415)<v0>-34521");

    String configLogLine = "[config 2017/11/09 11:29:14.985 PST main tid=0x1] (msgTID=1 msgSN=2) Running in headless mode";
    assertThat(logFilePatternReceiver.parseLine(configLogLine).isPresent()).isTrue();
    LoggingEvent configLoggingEvent = logFilePatternReceiver.parseLine(configLogLine).get();
    assertThat(configLoggingEvent).isNotNull();
    assertThat(configLoggingEvent.getLevel()).isEqualTo(Level.DEBUG);
    assertThat(configLoggingEvent.getTimeStamp()).isEqualTo(1510255754985L);
    assertThat(configLoggingEvent.getThreadName()).isEqualTo("main tid=0x1");
    assertThat(configLoggingEvent.getMessage()).isEqualTo("(msgTID=1 msgSN=2) Running in headless mode");

    String infoLogLine = "[info 2018/05/30 14:24:05.459 IST server1 <main> tid=0x1] Startup Configuration:";
    assertThat(logFilePatternReceiver.parseLine(infoLogLine).isPresent()).isTrue();
    LoggingEvent infoLoggingEvent = logFilePatternReceiver.parseLine(infoLogLine).get();
    assertThat(infoLoggingEvent).isNotNull();
    assertThat(infoLoggingEvent.getLevel()).isEqualTo(Level.INFO);
    assertThat(infoLoggingEvent.getTimeStamp()).isEqualTo(1527686645459L);
    assertThat(infoLoggingEvent.getThreadName()).isEqualTo("server1 <main> tid=0x1");
    assertThat(infoLoggingEvent.getMessage()).isEqualTo("Startup Configuration:");

    String warningLogLine = "[warning 2017/07/21 08:57:27.957 UTC server1 <localhost-startStop-1> tid=0x15] No bind-address or hostname-for-sender is specified, Using local host";
    assertThat(logFilePatternReceiver.parseLine(warningLogLine).isPresent()).isTrue();
    LoggingEvent warningLoggingEvent = logFilePatternReceiver.parseLine(warningLogLine).get();
    assertThat(warningLoggingEvent).isNotNull();
    assertThat(warningLoggingEvent.getLevel()).isEqualTo(Level.WARN);
    assertThat(warningLoggingEvent.getTimeStamp()).isEqualTo(1500627447957L);
    assertThat(warningLoggingEvent.getThreadName()).isEqualTo("server1 <localhost-startStop-1> tid=0x15");
    assertThat(warningLoggingEvent.getMessage()).isEqualTo("No bind-address or hostname-for-sender is specified, Using local host");

    String severeLogLine = "[severe 2017/11/13 16:02:54.261 PST JMX client heartbeat 2 tid=0x2c] (msgTID=44 msgSN=13) No longer connected to 127.0.0.1[1099].";
    assertThat(logFilePatternReceiver.parseLine(severeLogLine).isPresent()).isTrue();
    LoggingEvent severeLoggingEvent = logFilePatternReceiver.parseLine(severeLogLine).get();
    assertThat(severeLoggingEvent).isNotNull();
    assertThat(severeLoggingEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(severeLoggingEvent.getTimeStamp()).isEqualTo(1510617774261L);
    assertThat(severeLoggingEvent.getThreadName()).isEqualTo("JMX client heartbeat 2 tid=0x2c");
    assertThat(severeLoggingEvent.getMessage()).isEqualTo("(msgTID=44 msgSN=13) No longer connected to 127.0.0.1[1099].");

    String errorLogLine = "[error 2018/08/28 23:02:38.915 GMT  <main> tid=0x1] Exception occurred in CacheListener";
    assertThat(logFilePatternReceiver.parseLine(errorLogLine).isPresent()).isTrue();
    LoggingEvent errorLoggingEvent = logFilePatternReceiver.parseLine(errorLogLine).get();
    assertThat(errorLoggingEvent).isNotNull();
    assertThat(errorLoggingEvent.getLevel()).isEqualTo(Level.ERROR);
    assertThat(errorLoggingEvent.getTimeStamp()).isEqualTo(1535497358915L);
    assertThat(errorLoggingEvent.getThreadName()).isEqualTo("<main> tid=0x1");
    assertThat(errorLoggingEvent.getMessage()).isEqualTo("Exception occurred in CacheListener");
  }

  @Test
  public void parseFileShouldThrowExceptionWhenPathIsNull() {
    assertThatThrownBy(() -> logFilePatternReceiver.parseFile(null, true)).isInstanceOf(NullPointerException.class).hasMessage("Path can't be null.");
  }

  @Test
  public void parseFileWithStopOnFirstMatchEnabledShouldThrowExceptionWhenNoFilterIsConfigured() {
    logFilePatternReceiver = new CustomLogFilePatternReceiver("");
    assertThatThrownBy(() -> logFilePatternReceiver.parseFile(mock(Path.class), true)).isInstanceOf(IllegalArgumentException.class).hasMessage("Can't stop on first match if filter expression is null.");

    logFilePatternReceiver = new CustomLogFilePatternReceiver(null);
    assertThatThrownBy(() -> logFilePatternReceiver.parseFile(mock(Path.class), true)).isInstanceOf(IllegalArgumentException.class).hasMessage("Can't stop on first match if filter expression is null.");
  }

  @Test
  public void parseFileShouldIgnoreUnknownFormats() throws IOException {
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.unknownLogPath, false);

    assertThat(loggingEvents.isEmpty()).isTrue();
    verify(logFilePatternReceiver, times(0)).doPost(any());
  }

  @Test
  public void parseFileShouldReturnOnlyTheFirstLoggingEventMatchingTheFilterWhenStopOnFirstMatchIsTrue_v8X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("'Command Line Parameters:'"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member8XLogPath, true);

    assertThat(loggingEvents.size()).isEqualTo(1);
    assertThat(loggingEvents.get(0).getMessage()).isInstanceOf(String.class);
    assertThat((String) loggingEvents.get(0).getMessage()).contains("Command Line Parameters:");
    verify(logFilePatternReceiver, times(1)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnOnlyTheFirstLoggingEventsMatchingTheComplexFilterWhenStopOnFirstMatchIsTrue_v8X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("(MSG ~= 'Startup Configuration') || (MSG ~= 'Command Line Parameters')"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member8XLogPath, true);

    assertThat(loggingEvents.size()).isEqualTo(1);
    LoggingEvent loggingEvent = loggingEvents.get(0);
    assertThat(loggingEvent.getMessage()).isNotNull();
    String message = (String) loggingEvent.getMessage();
    assertThat(message.contains("Command Line Parameters")).isTrue();
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsWhenNoFiltersAreConfigured_v8X() throws Exception {
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member8XLogPath, false);

    assertThat(loggingEvents.size()).isEqualTo(199);
    verify(logFilePatternReceiver, times(199)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsMatchingTheFilterWhenStopOnFirstMatchIsFalse_v8X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("'Initializing'"));
    List<LoggingEvent> startingLoggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member8XLogPath, false);

    assertThat(startingLoggingEvents.size()).isEqualTo(10);
    startingLoggingEvents.forEach(event -> assertThat(event.getMessage()).isInstanceOf(String.class));
    assertThat((String) startingLoggingEvents.get(0).getMessage()).isEqualTo("Initializing region _monitoringRegion_10.69.252.33<v0>34911");
    assertThat((String) startingLoggingEvents.get(1).getMessage()).isEqualTo("Initializing region _ConfigurationRegion");
    assertThat((String) startingLoggingEvents.get(2).getMessage()).isEqualTo("Initializing Spring FrameworkServlet 'gemfire'");
    assertThat((String) startingLoggingEvents.get(3).getMessage()).isEqualTo("Initializing ExecutorService  'asyncTaskExecutor'");
    assertThat((String) startingLoggingEvents.get(4).getMessage()).isEqualTo("Initializing Spring root WebApplicationContext");
    assertThat((String) startingLoggingEvents.get(5).getMessage()).isEqualTo("Initializing Spring FrameworkServlet 'mvc-dispatcher'");
    assertThat((String) startingLoggingEvents.get(6).getMessage()).isEqualTo("Initializing region _monitoringRegion_10.69.252.33<v1>45911");
    assertThat((String) startingLoggingEvents.get(7).getMessage()).isEqualTo("Initializing region _notificationRegion_10.69.252.33<v1>45911");
    assertThat((String) startingLoggingEvents.get(8).getMessage()).isEqualTo("Initializing region _monitoringRegion_10.69.252.33<v4>21403");
    assertThat((String) startingLoggingEvents.get(9).getMessage()).isEqualTo("Initializing region _notificationRegion_10.69.252.33<v4>21403");
    verify(logFilePatternReceiver, times(10)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsMatchingTheComplexFilterWhenStopOnFirstMatchIsFalse_v8X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("(MSG ~= 'Startup Configuration') || (MSG ~= 'Command Line Parameters')"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member8XLogPath, false);

    assertThat(loggingEvents.size()).isEqualTo(4);
    for (LoggingEvent loggingEvent : loggingEvents) {
      List<String> patterns = Arrays.asList("Startup Configuration", "Command Line Parameters");
      assertThat(loggingEvent.getMessage()).isNotNull();
      String message = (String) loggingEvent.getMessage();
      assertThat(patterns.parallelStream().anyMatch(message::contains)).isTrue();
    }
  }

  @Test
  public void parseFileShouldReturnOnlyTheFirstLoggingEventMatchingTheFilterWhenStopOnFirstMatchIsTrue_v9X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("'JGroups channel'"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member9XLogPath, true);

    assertThat(loggingEvents.size()).isEqualTo(1);
    assertThat(loggingEvents.get(0).getMessage()).isInstanceOf(String.class);
    assertThat((String) loggingEvents.get(0).getMessage()).isEqualTo("JGroups channel created (took 66ms)");
    verify(logFilePatternReceiver, times(1)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnOnlyTheFirstLoggingEventsMatchingTheComplexFilterWhenStopOnFirstMatchIsTrue_v9X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("(MSG ~= 'Command Line Parameters') || (MSG ~= 'Startup Configuration')"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member9XLogPath, true);

    assertThat(loggingEvents.size()).isEqualTo(1);
    LoggingEvent loggingEvent = loggingEvents.get(0);
    assertThat(loggingEvent.getMessage()).isNotNull();
    String message = (String) loggingEvent.getMessage();
    assertThat(message.contains("Startup Configuration")).isTrue();
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsWhenNoFiltersAreConfigured_v9X() throws Exception {
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member9XLogPath, false);

    assertThat(loggingEvents.size()).isEqualTo(49);
    verify(logFilePatternReceiver, times(49)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsMatchingTheFilterWhenStopOnFirstMatchIsFalse_v9X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("'Starting'"));
    List<LoggingEvent> startingLoggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member9XLogPath, false);

    assertThat(startingLoggingEvents.size()).isEqualTo(2);
    assertThat(startingLoggingEvents.get(0).getMessage()).isInstanceOf(String.class);
    assertThat((String) startingLoggingEvents.get(0).getMessage()).isEqualTo("Starting membership services");
    assertThat(startingLoggingEvents.get(1).getMessage()).isInstanceOf(String.class);
    assertThat((String) startingLoggingEvents.get(1).getMessage()).isEqualTo("Starting DistributionManager 192.168.1.7(server1:32310)<v1>:1025.  (took 520 ms)");
    verify(logFilePatternReceiver, times(2)).doPost(any());
    assertThat(logFilePatternReceiver.getUnparsedLines().size()).isEqualTo(0);
  }

  @Test
  public void parseFileShouldReturnAllLoggingEventsMatchingTheComplexFilterWhenStopOnFirstMatchIsFalse_v9X() throws Exception {
    logFilePatternReceiver = spy(new CustomLogFilePatternReceiver("(MSG ~= 'Startup Configuration') || (MSG ~= 'Command Line Parameters')"));
    List<LoggingEvent> loggingEvents = logFilePatternReceiver.parseFile(LogsSampleDataUtils.member9XLogPath, false);

    assertThat(loggingEvents.size()).isEqualTo(3);
    for (LoggingEvent loggingEvent : loggingEvents) {
      List<String> patterns = Arrays.asList("Command Line Parameters", "Startup Configuration");
      assertThat(loggingEvent.getMessage()).isNotNull();
      String message = (String) loggingEvent.getMessage();
      assertThat(patterns.parallelStream().anyMatch(message::contains)).isTrue();
    }
  }
}
