package org.apache.geode.support.service.logs.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.component.ULogger;
import org.apache.log4j.component.spi.NOPULogger;
import org.apache.log4j.receivers.varia.LogFilePatternReceiver;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.internal.logging.DateFormatter;

/**
 * Class {@link LogFilePatternReceiver} is not designed for inheritance. This class extends it,
 * anyway, overriding some methods and constructors to be able to store all lines within an
 * internal structure, and to parse individual lines instead of the entire log file.
 * The class is not thread-safe.
 *
 * @see org.apache.log4j.receivers.varia.LogFilePatternReceiver
 */
class CustomLogFilePatternReceiver extends LogFilePatternReceiver {
  private static final Logger logger = LoggerFactory.getLogger(CustomLogFilePatternReceiver.class);

  /* Geode/GemFire Patterns */
  private final static String GEODE_LOG_MESSAGE_FORMAT = "[LEVEL TIMESTAMP THREAD] MESSAGE";
  private final static String GEODE_CUSTOM_LOG_LEVELS = "fine=TRACE,finest=TRACE,config=DEBUG,warning=WARN,severe=ERROR";

  /* Instance Fields */
  private boolean stopOnFirstMatch;
  private final Pattern regexpFilter;
  private List<String> unparsedLines;
  private List<LoggingEvent> loggingEvents;

  List<String> getUnparsedLines() {
    return unparsedLines;
  }

  @Override
  protected ULogger getLogger() {
    return NOPULogger.NOP_LOGGER;
  }

  CustomLogFilePatternReceiver(String filterExpression) {
    super();

    setFileURL("file:");
    setUseCurrentThread(true);
    setLogFormat(GEODE_LOG_MESSAGE_FORMAT);
    setTimestampFormat(DateFormatter.FORMAT_STRING);
    setCustomLevelDefinitions(GEODE_CUSTOM_LOG_LEVELS);

    if (StringUtils.isBlank(filterExpression)) {
      setFilterExpression(null);
      this.regexpFilter = Pattern.compile(".*");
    } else {
      setFilterExpression(filterExpression);
      this.regexpFilter = Pattern.compile("(.*)" + filterExpression + "(.*)", Pattern.CASE_INSENSITIVE);
    }

    this.stopRequested = false;
    this.stopOnFirstMatch = false;
    this.loggingEvents = new ArrayList<>();
    this.unparsedLines = new ArrayList<>();
  }

  @Override
  public void doPost(LoggingEvent event) {
    this.loggingEvents.add(event);
    this.stopRequested = stopOnFirstMatch;
  }

  @Override
  protected void processAdditionalLines(List<String> additionalLines) {
    if (regexpFilter != null) {
      for (String unparsedLine : additionalLines) {
        if (regexpFilter.matcher(unparsedLine).matches())
          this.unparsedLines.add(unparsedLine);
      }
    }
  }

  Optional<LoggingEvent> parseLine(String line) {
    Objects.requireNonNull(line, "Line can't be null.");
    initialize();
    createPattern();
    this.loggingEvents.clear();
    BufferedReader bufferedReader = new BufferedReader(new StringReader(line));

    try {
      process(bufferedReader);
    } catch (IOException ioException) {
      // Probably shut down, shouldn't happen as we're using the current thread.
      logger.error("IOException while processing the line.", ioException);
    }

    return loggingEvents.isEmpty() ? Optional.empty() : Optional.of(loggingEvents.get(0));
  }

  List<LoggingEvent> parseFile(Path path, boolean stopOnFirstMatch) throws IOException {
    Objects.requireNonNull(path, "Path can't be null.");
    if (stopOnFirstMatch && getFilterExpression() == null) throw new IllegalArgumentException("Can't stop on first match if filter expression is null.");

    this.loggingEvents.clear();
    this.stopOnFirstMatch = stopOnFirstMatch;
    setFileURL("file:" + path.toAbsolutePath());
    reader = new InputStreamReader(new URL(getFileURL()).openStream(), StandardCharsets.UTF_8);
    activateOptions();

    return loggingEvents;
  }
}
