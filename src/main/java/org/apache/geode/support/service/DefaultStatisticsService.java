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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.apache.geode.internal.statistics.ArchiveInfo;
import org.apache.geode.internal.statistics.StatArchiveFile;
import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.internal.statistics.StatValue;
import org.apache.geode.internal.statistics.ValueFilter;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.Category;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.domain.statistics.filters.AbstractValueFilter;
import org.apache.geode.support.domain.statistics.filters.SimpleValueFilter;

/**
 *
 */
@Service
class DefaultStatisticsService implements StatisticsService {
  private static final int BUFFER_SIZE = 1024 * 1024;
  private static final Logger logger = LoggerFactory.getLogger(DefaultStatisticsService.class);
  /* This Statistic must be present in all files, that's why we use it as the default */
  protected final AbstractValueFilter defaultValueFilter = new SimpleValueFilter("VMStats", null, "cpus", null);

  /**
   *
   * @return
   */
  protected Predicate<Path> isStatisticsFile() {
    return path -> Files.isRegularFile(path) && defaultValueFilter.archiveMatches(path.toFile());
  }

  /**
   *
   * @param filterUsed
   * @return
   */
  protected Predicate<StatArchiveReader.ResourceInst> isSearchedResourceInstance(AbstractValueFilter filterUsed) {
    return resourceInst -> resourceInst != null && resourceInst.isLoaded() && resourceInst.getType().getName().equals(filterUsed.getTypeId());
  }

  /**
   * Instantiates and initializes the internal {@link StatArchiveFile} to parse a statistics file.
   *
   * @param path Path representing the file to read.
   * @param filters Filters to apply when parsing the file.
   * @return The StatArchiveFile, ready for use.
   * @throws IOException If an exception occurs while trying to create the InputStream on the original file.
   */
  protected StatArchiveFile initializeStatArchiveFile(Path path, List<ValueFilter> filters) throws IOException {
    StatArchiveFile statArchiveFile = new StatArchiveFile(path.toFile(), filters.toArray(new ValueFilter[filters.size()]));
    statArchiveFile.update(false);

    return statArchiveFile;
  }

  /**
   * Parses the sampling metadata from a given {@link StatArchiveFile}.
   * Makes defensive checks for nullity and validity of the received results.
   *
   * @param statFile The {@link StatArchiveFile} to parse the metadata from, must be already initialized.
   * @return The SamplingMetadata parsed from the given StatArchiveFile.
   */
  protected SamplingMetadata parseSamplingMetadata(StatArchiveFile statFile) {
    Objects.requireNonNull(statFile, "StatArchiveFile can not be null.");

    ArchiveInfo info = statFile.getArchiveInfo();
    StatArchiveReader.ResourceInst[] resourceInstancesTable = statFile.getResourceInstancesTable();
    if (ArrayUtils.isEmpty(resourceInstancesTable)) throw new IllegalStateException("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");

    // Find the ResourceInstance corresponding to the filter used when reading the file.
    StatArchiveReader.ResourceInst cpuResourceInstance = Arrays.stream(resourceInstancesTable)
        .filter(isSearchedResourceInstance(defaultValueFilter))
        .findAny().get();

    StatValue cpuStatValues = cpuResourceInstance.getStatValue(defaultValueFilter.getStatisticId());
    if (cpuStatValues == null) throw new IllegalStateException(String.format("Invalid sampling file, StatValue for %s should not be null.", defaultValueFilter.getStatisticId()));

    long[] timeStamps = cpuStatValues.getRawAbsoluteTimeStamps();
    if (ArrayUtils.isEmpty(timeStamps)) throw new IllegalStateException(String.format("Invalid sampling file, array of TimeStamps for %s should not be null.", defaultValueFilter.getStatisticId()));

    // Make sure values are ordered.
    Arrays.sort(timeStamps);
    return new SamplingMetadata(info.getArchiveFileName(), info.getArchiveFormatVersion(),
        info.isCompressed(), info.getTimeZone().toZoneId(), timeStamps[0],
        timeStamps[timeStamps.length - 1], info.getProductVersion(), info.getOs());
  }

  /**
   * Parses the actual statistical data from a given {@link StatArchiveFile}.
   * Makes defensive checks for nullity and validity of the received results.
   *
   * @param statFile The {@link StatArchiveFile} to parse the statistical data from, must be already initialized.
   * @return The statistical data parsed from the StatFile, based on the filters used.
   */
  protected Map<String, Category> parseSamplingStatisticalData(StatArchiveFile statFile) {
    Objects.requireNonNull(statFile, "StatArchiveFile can not be null.");

    Map<String, Category> categoryMap = new HashMap<>();
    StatArchiveReader.ResourceInst[] resourceInstancesTable = statFile.getResourceInstancesTable();
    if (ArrayUtils.isEmpty(resourceInstancesTable)) throw new IllegalStateException("Invalid sampling file, ResourceInstancesTable should not be null nor empty.");

    // Iterate through the parsed resources and populate internal structures.
    Arrays.stream(resourceInstancesTable).filter(Objects::nonNull)
        .forEach(resourceInst -> {
          StatValue[] statValues = resourceInst.getStatValues();
          StatArchiveReader.ResourceType resourceType = resourceInst.getType();

          // StatArchiveFile loads metadata for all ResourceType found, use only those that were actually loaded (at least one filter returned true).
          if (resourceInst.isLoaded()) {
            Category category = new Category(resourceType.getName(), resourceType.getDescription());

            if (ArrayUtils.isNotEmpty(statValues)) {
              Arrays.stream(statValues).filter(Objects::nonNull)
                  .forEach(statValue -> {
                    Statistic statistic = new Statistic(statValue);
                    category.addStatistic(statistic);
                  });
            }

            categoryMap.put(category.getName(), category);
          }
        });

    return categoryMap;
  }

  /**
   * Parses a given sampling file using the specified filters, populating both its metadata and its statistical data.
   *
   * @param path Path of the sampling file to parse.
   * @param filters Filters to apply when reading the file, which determine whether certain categories and statistics will be parsed or not.
   * @return The Statistic Sampling containing the metadata and statistical data, if any.
   * @throws IOException When an exception occurs while parsing the file.
   */
  protected Sampling parseIndividualSampling(Path path, final List<ValueFilter> filters) throws Exception {
    Sampling samplingResult;
    StatArchiveFile statArchiveFile = null;
    List<ValueFilter> clonedFilters = new ArrayList<>(filters);

    // Always include the metadata when parsing a file.
    if (!clonedFilters.contains(defaultValueFilter)) clonedFilters.add(defaultValueFilter);

    try {
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File %s...", path.toString()));
      statArchiveFile = initializeStatArchiveFile(path, clonedFilters);
      SamplingMetadata fileMetadata = parseSamplingMetadata(statArchiveFile);
      Map<String, Category> categoriesMap = clonedFilters.size() == 1 ?  Collections.emptyMap() : parseSamplingStatisticalData(statArchiveFile);

      // Remove the stat added by the default filter.
      if (clonedFilters.size() != filters.size()) {
        Category vmStatsCategory = categoriesMap.get(defaultValueFilter.getTypeId());

        if (vmStatsCategory != null) {
          vmStatsCategory.removeStatistic(defaultValueFilter.getStatisticId());

          // Remove the Category entirely if it was included only for the default filter.
          if (vmStatsCategory.getStatistics().isEmpty()) categoriesMap.remove(defaultValueFilter.getTypeId());
        }
      }

      samplingResult = new Sampling(fileMetadata, categoriesMap);
      if (logger.isDebugEnabled()) logger.debug(String.format("Parsing File  %s... Done!.", path.toString()));
    } catch (Exception exception) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toAbsolutePath().toString());
      logger.error(errorMessage, exception);
      throw exception;
    } finally {
      if (statArchiveFile != null) {
        try {
          statArchiveFile.close();
        } catch (IOException ioException) {
          logger.warn(String.format("File %s wasn't correctly closed.", path.toAbsolutePath().toString()), ioException);
        }
      }
    }

    return samplingResult;
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
          logger.warn(String.format("Sampling %s wasn't correctly closed.", sourcePath.toAbsolutePath().toString()), ioException);
        }
      }

      if (uncompressedOutputStream != null) {
        try {
          uncompressedOutputStream.close();
        } catch (IOException ioException) {
          logger.warn(String.format("Sampling %s wasn't correctly closed.", targetPath.toAbsolutePath().toString()), ioException);
        }
      }
    }
  }

  @Override
  public List<ParsingResult<SamplingMetadata>> parseMetadata(Path path) {
    List<ParsingResult<SamplingMetadata>> parsingResults = new ArrayList<>();

    try {
      Files.walk(path)
          .filter(isStatisticsFile())
          .forEach(currentPath -> {
            ParsingResult<SamplingMetadata> parsingResult;

            try {
              Sampling sampling = parseIndividualSampling(currentPath, new ArrayList<>());
              parsingResult = new ParsingResult<>(currentPath, sampling.getMetadata());
            } catch (Exception exception) {
              parsingResult = new ParsingResult<>(currentPath, exception);
            }

            parsingResults.add(parsingResult);
          });
    } catch (IOException ioException) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toAbsolutePath().toString());
      logger.error(errorMessage, ioException);
      parsingResults.add(new ParsingResult<>(path, ioException));
    }

    return parsingResults;
  }

  @Override
  public List<ParsingResult<Sampling>> parseSampling(Path path, List<ValueFilter> filters) {
    List<ParsingResult<Sampling>> parsingResults = new ArrayList<>();

    try {
      Files.walk(path)
          .filter(isStatisticsFile())
          .forEach(currentPath -> {
            ParsingResult<Sampling> parsingResult;

            try {
              parsingResult = new ParsingResult<>(currentPath, parseIndividualSampling(currentPath, filters));
            } catch (Exception exception) {
              parsingResult = new ParsingResult<>(currentPath, exception);
            }

            parsingResults.add(parsingResult);
          });
    } catch (IOException ioException) {
      String errorMessage = String.format("There was a problem while parsing file %s.", path.toAbsolutePath().toString());
      logger.error(errorMessage, ioException);
      parsingResults.add(new ParsingResult<>(path, ioException));
    }

    return parsingResults;
  }
}
