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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.service.logs.internal.Log4jParser;
import org.apache.geode.support.service.logs.internal.LogParser;

/**
 *
 */
@Service
class MultiThreadedLogsService implements LogsService {
  private final FilesService filesService;
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final Logger logger = LoggerFactory.getLogger(MultiThreadedLogsService.class);

  Predicate<Path> isLogFile() {
    return path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".log");
  }

  ParserTask createParserTask(Path path, boolean intervalOnly) {
    LogParser logParser = new Log4jParser(filesService);
    return new ParserTask(path, logParser, intervalOnly);
  }

  @Autowired
  public MultiThreadedLogsService(FilesService filesService) {
    this.filesService = filesService;
  }

  List<ParsingResult<LogMetadata>> parseAll(Path path, boolean intervalOnly) {
    List<Future<ParsingResult<LogMetadata>>> parserTasks = new ArrayList<>();
    List<ParsingResult<LogMetadata>> parsingResults = new ArrayList<>();

    try {
      Files.walk(path)
          .filter(isLogFile())
          .forEach(currentPath -> {
            parserTasks.add(executorService.submit(createParserTask(currentPath, intervalOnly)));
          });
    } catch (IOException ioException) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toString());
      logger.error(errorMessage, ioException);
      parsingResults.add(new ParsingResult<>(path, ioException));
    }

    try {
      for (Future<ParsingResult<LogMetadata>> future : parserTasks) parsingResults.add(future.get());
    } catch (Exception exception) {
      // Shouldn't happen.
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toString());
      logger.error(errorMessage, exception);
      parsingResults.add(new ParsingResult<>(path, exception));
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

  static class ParserTask implements Callable<ParsingResult<LogMetadata>> {
    private final Path path;
    private final LogParser logParser;
    private final boolean intervalOnly;

    ParserTask(Path path, LogParser logParser, boolean intervalOnly) {
      this.path = path;
      this.logParser = logParser;
      this.intervalOnly = intervalOnly;
    }

    @Override
    public ParsingResult<LogMetadata> call() {
      ParsingResult<LogMetadata> parsingResult;

      try {
        if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
        long starTime = System.nanoTime();
        LogMetadata logFileMetadata = (intervalOnly) ? logParser.parseLogFileInterval(path) : logParser.parseLogFileMetadata(path);
        long finishTime =  System.nanoTime();
        if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File  %s... Done!. Time elapsed: %d seconds.", path.toString(), TimeUnit.SECONDS.convert(finishTime - starTime, TimeUnit.NANOSECONDS)));
        parsingResult = new ParsingResult<>(path, logFileMetadata);
      } catch (Exception exception) {
        logger.error(String.format("Parsing File %s... Error: %s", path.toString(), exception.getMessage()));
        parsingResult = new ParsingResult<>(path, exception);
      }

      return parsingResult;
    }
  }
}
