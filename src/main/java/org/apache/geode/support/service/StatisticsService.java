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

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;

public interface StatisticsService {

  /**
   * Parses the metadata for the source statistics files, or all statistics files contained within the source path if it's a folder.
   *
   * @param path A statistics file, or a directory containing statistics files to scan.
   * @return List of ParsingResult instances, containing the parsed metadata and/or the error occurred while trying to read the file.
   */
  List<ParsingResult<StatisticFileMetadata>> parseMetadata(Path path);

  /**
   * Decompress the source statistics file and write the contents as a regular statistics file to the output path.
   *
   * @param sourcePath Path of the compressed source statistics file.
   * @param targetPath Path of the resulting file where original statistics file contents should be written.
   * @throws IOException If an exception occurs while decompressing or witting the results.
   */
  void decompress(Path sourcePath, Path targetPath) throws IOException;
}
