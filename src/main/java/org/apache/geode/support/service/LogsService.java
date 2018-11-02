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
