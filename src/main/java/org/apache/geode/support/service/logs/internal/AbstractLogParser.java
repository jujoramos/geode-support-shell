package org.apache.geode.support.service.logs.internal;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;

public abstract class AbstractLogParser implements LogParser {
  private final Logger logger;
  private final FilesService filesService;

  public AbstractLogParser(FilesService filesService) {
    this.filesService = filesService;
    this.logger = LoggerFactory.getLogger(getClass());
  }

  abstract LogMetadata buildMetadata(Path path, String startTime, String finishTime) throws IOException;

  abstract LogMetadata buildMetadataWithIntervalOnly(Path fileName, String startTime, String finishTime);

  String readFirstLine(Path path) throws IOException {
    String firstLine;

    // Read only first non-empty line of the file.
    try (Scanner fileReader = new Scanner(path)) {
      do {
        firstLine = fileReader.nextLine();
      } while (StringUtils.isBlank(firstLine));
    }

    return firstLine;
  }

  String readLastLine(Path path) throws IOException {
    String finishLine;

    // Read only last non-empty line of the file.
    try (ReversedLinesFileReader reversedLinesFileReader = new ReversedLinesFileReader(path.toFile(), Charset.defaultCharset())) {
      do {
        finishLine = reversedLinesFileReader.readLine();
      } while (StringUtils.isBlank(finishLine));
    }

    return finishLine;
  }

  @Override
  public LogMetadata parseLogFileInterval(Path path) throws IOException {
    Objects.requireNonNull(path, "Path can not be null.");
    filesService.assertFileReadability(path);

    if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
    String firstLine = readFirstLine(path);
    String finishLine = readLastLine(path);
    LogMetadata logMetadata = buildMetadataWithIntervalOnly(path, firstLine, finishLine);
    if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File  %s... Done!.", path.toString()));

    return logMetadata;
  }

  @Override
  public LogMetadata parseLogFileMetadata(Path path) throws IOException {
    Objects.requireNonNull(path, "Path can not be null.");
    filesService.assertFileReadability(path);

    if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
    String firstLine = readFirstLine(path);
    String finishLine = readLastLine(path);
    LogMetadata logMetadata = buildMetadata(path, firstLine, finishLine);
    if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File  %s... Done!.", path.toString()));

    return logMetadata;
  }
}
