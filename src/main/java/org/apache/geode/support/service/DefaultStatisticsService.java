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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.apache.geode.internal.statistics.ArchiveInfo;
import org.apache.geode.internal.statistics.StatArchiveFile;
import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.internal.statistics.ValueFilter;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.SimpleValueFilter;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;

/**
 *
 */
@Service
class DefaultStatisticsService implements StatisticsService {
  private static final int BUFFER_SIZE = 1024 * 1024;
  private static final Logger logger = LoggerFactory.getLogger(DefaultStatisticsService.class);
  /* This particular Statistic must be present in all statistics files, that's why we use it as the default */
  protected final SimpleValueFilter defaultValueFilter = new SimpleValueFilter("VMStats", "", "cpus", null);

  /**
   *
   * @param <T>
   * @return
   */
  protected <T> Collector<T, ?, T> singletonCollector() {
    return Collectors.collectingAndThen(
        Collectors.toList(),
        list -> {
          if (list.size() != 1) {
            throw new IllegalStateException("More than one element received.");
          }

          return list.get(0);
        }
    );
  }

  /**
   *
   * @param filterUsed
   * @return
   */
  protected Predicate<StatArchiveReader.ResourceInst> isSearchedResourceInstance(SimpleValueFilter filterUsed) {
    return resourceInst ->
        resourceInst != null &&
            resourceInst.isLoaded() &&
            resourceInst.getType().getName().equals(filterUsed.getTypeId());
  }

  /**
   * Instantiates the internal StatArchiveFile to read a statistics file.
   * @param path File to read.
   * @param filters Filters to apply when reading the file.
   * @return The StatArchiveFile, ready for use.
   * @throws IOException If an exception occurs while trying to create the InputStream on the original file.
   */
  protected StatArchiveFile createStatArchiveFile(Path path, List<ValueFilter> filters) throws IOException {
    return new StatArchiveFile(path.toFile(), filters.toArray(new ValueFilter[0]));
  }

  /**
   * Executes the actual parsing of the file metadata.
   * @param statFile Internal StatArchiveFile to parse.
   * @param filterUsed Default filter used when creating the file.
   * @return The parsed Metadata for the given file.
   * @throws IOException When an exception occurs while reading the actual file.
   */
  protected StatisticFileMetadata parseStatisticFileMetadata(StatArchiveFile statFile, SimpleValueFilter filterUsed) throws IOException {
    statFile.update(false);
    ArchiveInfo info = statFile.getArchiveInfo();
    StatArchiveReader.ResourceInst[] resourceInstances = statFile.getResourceInstancesTable();

    // Find the single ResourceInstance corresponding to the filter used when reading the file.
    StatArchiveReader.ResourceInst cpuResourceInstance = Arrays.stream(resourceInstances)
        .filter(isSearchedResourceInstance(filterUsed))
        .collect(singletonCollector());

    StatArchiveReader.StatValue cpuValues = cpuResourceInstance.getStatValue(filterUsed.getStatisticId());
    long[] timeStamps = cpuValues.getRawAbsoluteTimeStamps();

    // Make sure values are ordered.
    Arrays.sort(timeStamps);
    return new StatisticFileMetadata(info.getArchiveFileName(), info.getArchiveFormatVersion(),
        info.isCompressed(), info.getTimeZone().toZoneId(), timeStamps[0],
        timeStamps[timeStamps.length - 1], info.getProductVersion(), info.getOs());
  }

  /**
   * Parses only the statistic file metadata. It uses the default filter ('VMStats.cpus') to
   * save memory and speeds up the loading process.
   * @param path File to parse the metadata from.
   * @return The Metadata for the given file.
   */
  protected StatisticFileMetadata parseFileMetadata(Path path) {
    StatArchiveFile statArchiveFile = null;
    StatisticFileMetadata statisticFileMetadata;

    try {
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
      statArchiveFile = createStatArchiveFile(path, Collections.singletonList(defaultValueFilter));
      statisticFileMetadata = parseStatisticFileMetadata(statArchiveFile, defaultValueFilter);
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s... Done!.", path.toString()));
    } catch (Exception exception) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toAbsolutePath().toString());
      logger.error(errorMessage, exception);
      throw new RuntimeException(errorMessage, exception);
    } finally {
      if (statArchiveFile != null) {
        try {
          statArchiveFile.close();
        } catch (IOException ioException) {
          logger.warn(String.format("File %s wasn't correctly closed.", path.toAbsolutePath().toString()), ioException);
        }
      }
    }

    return statisticFileMetadata;
  }

  @Override
  public List<ParsingResult<StatisticFileMetadata>> parseMetadata(Path path) {
    List<ParsingResult<StatisticFileMetadata>> parsingResults = new ArrayList<>();

    try {
      Files.walk(path)
          .filter(currentPath -> Files.isRegularFile(currentPath) && defaultValueFilter.archiveMatches(currentPath.toFile()))
          .forEach(currentPath -> {
            ParsingResult<StatisticFileMetadata> parsingResult;

            try {
              parsingResult = new ParsingResult<>(currentPath, parseFileMetadata(currentPath));
            } catch (Exception exception) {
              parsingResult = new ParsingResult<>(currentPath, exception);
            }

            parsingResults.add(parsingResult);
          });
    } catch (IOException ioException) {
      String errorMessage = String.format("There was a problem while parsing the files.");
      logger.error(errorMessage, ioException);
      parsingResults.add(new ParsingResult<>(path, ioException));
    }

    return parsingResults;
  }

  @Override
  public void decompress(Path sourcePath, Path targetPath) throws IOException {
    logger.debug(String.format("Decompressing file %s...", sourcePath.toString()));

    byte[] buffer = new byte[BUFFER_SIZE];
    GZIPInputStream compressedInputStream = null;
    FileOutputStream uncompressedOutputStream = null;

    try {
      uncompressedOutputStream = new FileOutputStream(targetPath.toString());
      compressedInputStream = new GZIPInputStream(new FileInputStream(sourcePath.toFile()));

      int readBytes;
      while ((readBytes = compressedInputStream.read(buffer)) > 0) {
        uncompressedOutputStream.write(buffer, 0, readBytes);
      }

      logger.debug(String.format("Decompressing file %s... Done!.", sourcePath.toString()));
    } catch (IOException ioException) {
      logger.error(String.format("Decompressing file %s... Error!.", sourcePath.toString()), ioException);

      // Delete the file if it was created.
      Files.delete(targetPath);
      throw ioException;
    } finally {

      if (compressedInputStream != null) {
        try {
          compressedInputStream.close();
        } catch (IOException ioException) {
          logger.warn(String.format("File %s wasn't correctly closed.", sourcePath.toAbsolutePath().toString()), ioException);
        }
      }

      if (uncompressedOutputStream != null) {
        try {
          uncompressedOutputStream.close();
        } catch (IOException ioException) {
          logger.warn(String.format("File %s wasn't correctly closed.", targetPath.toAbsolutePath().toString()), ioException);
        }
      }
    }
  }
}
