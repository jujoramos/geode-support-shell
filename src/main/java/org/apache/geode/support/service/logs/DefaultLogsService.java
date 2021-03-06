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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.service.logs.internal.LogParser;

/**
 * @deprecated since 1.2.0, use {@link MultiThreadedLogsService} instead.
 */
@Deprecated
class DefaultLogsService implements LogsService {
  private LogParser logParser;
  private static final Logger logger = LoggerFactory.getLogger(DefaultLogsService.class);

  Predicate<Path> isLogFile() {
    return path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".log");
  }

  DefaultLogsService(LogParser logParser) {
    this.logParser = logParser;
  }

  LogMetadata parseSelectively(Path path, boolean intervalOnly) throws Exception {
    LogMetadata logFileMetadata;

    try {
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
      long starTime = System.nanoTime();
      logFileMetadata = (intervalOnly) ? logParser.parseLogFileInterval(path) : logParser.parseLogFileMetadata(path);
      long finishTime =  System.nanoTime();
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File  %s... Done!. Time elapsed: %d seconds.", path.toString(), TimeUnit.SECONDS.convert(finishTime - starTime, TimeUnit.NANOSECONDS)));
    } catch (Exception exception) {
      logger.error(String.format("Parsing File %s... Error: %s", path.toString(), exception.getMessage()));
      throw exception;
    }

    return logFileMetadata;
  }

  List<ParsingResult<LogMetadata>> parseAll(Path path, boolean intervalOnly) {
    List<ParsingResult<LogMetadata>> parsingResults = new ArrayList<>();

    try {
      Files.walk(path)
          .filter(isLogFile())
          .forEach(currentPath -> {
            ParsingResult<LogMetadata> parsingResult;

            try {
              LogMetadata logFileMetadata = parseSelectively(currentPath, intervalOnly);
              parsingResult = new ParsingResult<>(currentPath, logFileMetadata);
            } catch (Exception exception) {
              parsingResult = new ParsingResult<>(currentPath, exception);
            }

            parsingResults.add(parsingResult);
          });
    } catch (IOException ioException) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toString());
      logger.error(errorMessage, ioException);
      parsingResults.add(new ParsingResult<>(path, ioException));
    }

    return parsingResults;
  }

  @Override
  public List<ParsingResult<LogMetadata>> parseInterval(Path path) {
    return parseAll(path, true);
  }

  @Override
  public List<ParsingResult<LogMetadata>> parseMetadata(Path path) {
    return parseAll(path, false);
  }
}
