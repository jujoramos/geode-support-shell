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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;

public abstract class AbstractLogParser implements LogParser {
  private final FilesService filesService;

  public AbstractLogParser(FilesService filesService) {
    this.filesService = filesService;
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
    String firstLine = readFirstLine(path);
    String finishLine = readLastLine(path);

    return buildMetadataWithIntervalOnly(path, firstLine, finishLine);
  }

  @Override
  public LogMetadata parseLogFileMetadata(Path path) throws IOException {
    Objects.requireNonNull(path, "Path can not be null.");
    filesService.assertFileReadability(path);
    String firstLine = readFirstLine(path);
    String finishLine = readLastLine(path);

    return buildMetadata(path, firstLine, finishLine);
  }
}
