package org.apache.geode.support.service.logs.internal;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.spi.LoggingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;

/**
 * Parses the log file using an extension of the log4j-extras library.
 *
 * Current and old versions of GemFire/Geode log the metadata information during the startup.
 * Configuration properties are logged right after the 'Startup Configuration:' string.
 * Startup parameters and version information is logged around the 'Command Line Parameters:' string.
 * Complex Pattern to get metadata and properties: "(MSG ~= 'Command Line Parameters') || (MSG ~= 'Startup Configuration')".
 */
@Component
class Log4jParser extends AbstractLogParser {
  private final static String SYSTEM_PROPERTIES = "System Properties:";
  private final static String LOG4J2_CONFIGURATION = "Log4J 2 Configuration:";
  private final static Pattern OPERATING_SYSTEM_PATTERN = Pattern.compile("Running on: .*,(.*)", Pattern.CASE_INSENSITIVE);
  private final static Pattern FULL_PRODUCT_VERSION_PATTERN = Pattern.compile("Java version:(.*) build|Product-Version:(.*)", Pattern.CASE_INSENSITIVE);

  private static Supplier<? extends RuntimeException> unparseableFormatExceptionSupplier() {
    return () -> new IllegalArgumentException("Log format not recognized.");
  }

  @Autowired
  public Log4jParser(FilesService filesService) {
    super(filesService);
  }

  @Override
  LogMetadata buildMetadataWithIntervalOnly(Path filePath, String startTimeLine, String finishTimeLine) {
    CustomLogFilePatternReceiver patternReceiver = new CustomLogFilePatternReceiver("");
    LoggingEvent parsedStartEvent = patternReceiver.parseLine(startTimeLine).orElseThrow(unparseableFormatExceptionSupplier());
    LoggingEvent parsedFinishEvent = patternReceiver.parseLine(finishTimeLine).orElseThrow(unparseableFormatExceptionSupplier());

    return LogMetadata.of(filePath.toString(), parsedStartEvent.getTimeStamp(), parsedFinishEvent.getTimeStamp());
  }

  List<LoggingEvent> parseFile(Path filePath) throws IOException {
    CustomLogFilePatternReceiver metadataPatternReceiver = new CustomLogFilePatternReceiver("'Command Line Parameters:'");
    return metadataPatternReceiver.parseFile(filePath, true);
  }

  String parseProductVersion(String loggingEventMessage) {
    Matcher versionMatcher = FULL_PRODUCT_VERSION_PATTERN.matcher(loggingEventMessage);
    String productVersion = null;
    if (versionMatcher.find()) {
      productVersion = versionMatcher.group(1);
      if (productVersion == null) productVersion = versionMatcher.group(2);
    }

    return (productVersion != null) ? productVersion.trim() : null;
  }

  String parseOperatingSystem(String loggingEventMessage) {
    Matcher operatingSystemMatcher = OPERATING_SYSTEM_PATTERN.matcher(loggingEventMessage);
    return operatingSystemMatcher.find() ? operatingSystemMatcher.group(1).trim() : null;
  }

  Properties parseSystemProperties(String loggingEventMessage) throws IOException {
    Properties systemProperties = new Properties();
    int startIndex = loggingEventMessage.indexOf(SYSTEM_PROPERTIES);
    int finishIndex = loggingEventMessage.indexOf(LOG4J2_CONFIGURATION);

    if (startIndex != -1) {
      String systemPropertiesString = loggingEventMessage.substring(startIndex + SYSTEM_PROPERTIES.length(), (finishIndex != -1) ? finishIndex: loggingEventMessage.length());
      systemProperties.load(new StringReader(systemPropertiesString));
    }

    return systemProperties;
  }

  @Override
  LogMetadata buildMetadata(Path filePath, String startTimeLine, String finishTimeLine) throws IOException {
    // Get Interval covered first.
    LogMetadata logMetadata = buildMetadataWithIntervalOnly(filePath, startTimeLine, finishTimeLine);

    // If we've reached this point, then the file format is valid so we can try to parse the metadata.
    List<LoggingEvent> loggingEvents = parseFile(filePath);

    // Metadata not found, return what we can.
    if (loggingEvents.isEmpty()) return logMetadata;

    // Metadata found, parse what we need.
    String loggingEventMessage = loggingEvents.get(0).getMessage().toString();
    String productVersion = parseProductVersion(loggingEventMessage);
    String operatingSystem = parseOperatingSystem(loggingEventMessage);
    Properties systemProperties = parseSystemProperties(loggingEventMessage);

    return LogMetadata.of(filePath.toString(), logMetadata.getStartTimeStamp(), logMetadata.getFinishTimeStamp(), productVersion, operatingSystem, systemProperties);
  }
}
