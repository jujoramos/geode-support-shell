package org.apache.geode.support.service.logs.internal;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.geode.support.domain.logs.LogMetadata;

public interface LogParser {

  /**
   * Parses only the time frame covered by the log file.
   *
   * @param path Log file to parse.
   * @return The log file metadata, with only the start and finish time populated.
   */
  LogMetadata parseLogFileInterval(Path path) throws IOException;

  /**
   * Parses the full log file metadata.
   *
   * @param path Log file to parse.
   * @return The log file metadata.
   */
  LogMetadata parseLogFileMetadata(Path path) throws IOException;
}
