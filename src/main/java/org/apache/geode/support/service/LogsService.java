package org.apache.geode.support.service;

import java.nio.file.Path;
import java.util.List;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;

public interface LogsService {

  /**
   * Parses only the time covered by the log file, or all log files contained within the source path if it's a folder.
   *
   * @param path A log file, or a directory containing log files to scan.
   * @return List of ParsingResult instances, containing the parsed interval and/or the error occurred while trying to parse each file.
   */
  List<ParsingResult<LogMetadata>> parseInterval(Path path);

  /**
   * Parses the metadata for the log file, or all log files contained within the source path if it's a folder.
   *
   * @param path A log file, or a directory containing log files to scan.
   * @return List of ParsingResult instances, containing the parsed metadata and/or the error occurred while trying to read the file.
   */
  List<ParsingResult<LogMetadata>> parseMetadata(Path path);
}
