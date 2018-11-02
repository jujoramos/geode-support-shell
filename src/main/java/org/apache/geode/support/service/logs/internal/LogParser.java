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
