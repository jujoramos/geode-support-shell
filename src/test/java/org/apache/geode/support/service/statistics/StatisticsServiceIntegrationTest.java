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
package org.apache.geode.support.service.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.test.StatisticsSampleDataUtils;

public class StatisticsServiceIntegrationTest {
  private StatisticsService statisticsService;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    this.statisticsService = new DefaultStatisticsService();
  }

  @Test
  public void decompressShouldThrowExceptionWhenFileIsNotCompressed() throws Exception {
    File mockedFile = temporaryFolder.newFile();
    String clusterOneServerOneFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    assertThatThrownBy(() -> statisticsService.decompress(Paths.get(clusterOneServerOneFilePath), mockedFile.toPath())).isInstanceOf(IOException.class);
  }

  @Test
  public void decompressShouldDeleteTargetFileIfDecompressionFails() throws Exception {
    File uncompressedFile = temporaryFolder.newFile("cluster1-locator.gfs");
    String clusterOneLocatorFilePath = StatisticsSampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getFilePath();

    assertThatThrownBy(() -> statisticsService.decompress(Paths.get(clusterOneLocatorFilePath), uncompressedFile.toPath())).isInstanceOf(IOException.class).hasMessage("Not in GZIP format");
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).count()).isEqualTo(0);
  }

  @Test
  public void decompressShouldExecuteCorrectly() throws Exception {
    File uncompressedFile = temporaryFolder.newFile("cluster1-locator.gfs");
    String clusterOneLocatorFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();

    assertThatCode(() -> statisticsService.decompress(Paths.get(clusterOneLocatorFilePath), uncompressedFile.toPath())).doesNotThrowAnyException();
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).count()).isEqualTo(1);
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).filter(path -> Files.isRegularFile(path) && path.getFileName().toString().equals("cluster1-locator.gfs")).count()).isEqualTo(1);

    // TODO: find a better way to asses the decompression result.
    SamplingMetadata decompressedMetadata = statisticsService.parseMetadata(uncompressedFile.toPath()).stream().findFirst().get().getData();
    SamplingMetadata uncompressedMetadata = statisticsService.parseMetadata(Paths.get(StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath())).stream().findFirst().get().getData();
    assertThat(decompressedMetadata.getVersion()).isEqualTo(uncompressedMetadata.getVersion());
    assertThat(decompressedMetadata.getTimeZoneId()).isEqualTo(uncompressedMetadata.getTimeZoneId());
    assertThat(decompressedMetadata.getStartTimeStamp()).isEqualTo(uncompressedMetadata.getStartTimeStamp());
    assertThat(decompressedMetadata.getProductVersion()).isEqualTo(uncompressedMetadata.getProductVersion());
    assertThat(decompressedMetadata.getFinishTimeStamp()).isEqualTo(uncompressedMetadata.getFinishTimeStamp());
    assertThat(decompressedMetadata.getOperatingSystem()).isEqualTo(uncompressedMetadata.getOperatingSystem());
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenFileDoestNotExist() {
    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(Paths.get("nonExistingFile.gfs"));

    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<SamplingMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(NoSuchFileException.class).hasMessage("nonExistingFile.gfs");
  }

  @Test
  public void parseMetadataShouldReturnBothParsingErrorsAndParsingSuccessesWhenParsingSucceedsForSomeFilesAndFailsForOthers() {
    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(
        StatisticsSampleDataUtils.rootFolder.toPath());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(9);

    // unparseableFile.gfs
    String unparseableFilePath = StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<SamplingMetadata> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().orElse(null);
    assertThat(unparseableResult).isNotNull();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // unparseableFile.gfs
    String unparseableCompressedFilePath = StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<SamplingMetadata> unparseableCompressedResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableCompressedFilePath)).findAny().orElse(null);
    assertThat(unparseableCompressedResult).isNotNull();
    assertThat(unparseableCompressedResult.isSuccess()).isFalse();
    assertThat(unparseableCompressedResult.getException()).isNotNull();
    assertThat(unparseableCompressedResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableCompressedFilePath);
    Exception unparseableCompressedException = unparseableCompressedResult.getException();
    assertThat(unparseableCompressedException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // SampleClient.gfs
    String sampleClientFilePath = StatisticsSampleDataUtils.SampleType.CLIENT.getFilePath();
    ParsingResult<SamplingMetadata> clientStatisticsResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(sampleClientFilePath)).findAny().orElse(null);
    assertThat(clientStatisticsResult).isNotNull();
    assertThat(clientStatisticsResult.isSuccess()).isTrue();
    assertThat(clientStatisticsResult.getData()).isNotNull();
    assertThat(clientStatisticsResult.getFile().toAbsolutePath().toString()).isEqualTo(sampleClientFilePath);
    SamplingMetadata clientArchiveMetadata = clientStatisticsResult.getData();
    StatisticsSampleDataUtils.assertClientMetadata(clientArchiveMetadata);

    // cluster1-locator.gz
    String clusterOneLocatorFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneLocatorFilePath)).findAny().orElse(null);
    assertThat(clusterOneLocatorResult).isNotNull();
    assertThat(clusterOneLocatorResult.isSuccess()).isTrue();
    assertThat(clusterOneLocatorResult.getData()).isNotNull();
    assertThat(clusterOneLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneLocatorFilePath);
    SamplingMetadata clusterOneLocatorMetadata = clusterOneLocatorResult.getData();
    StatisticsSampleDataUtils.assertClusterOneLocatorMetadata(clusterOneLocatorMetadata);

    // cluster2-locator.gz
    String clusterTwoLocatorFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_LOCATOR.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoLocatorFilePath)).findAny().orElse(null);
    assertThat(clusterTwoLocatorResult).isNotNull();
    assertThat(clusterTwoLocatorResult.isSuccess()).isTrue();
    assertThat(clusterTwoLocatorResult.getData()).isNotNull();
    assertThat(clusterTwoLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoLocatorFilePath);
    SamplingMetadata clusterTwoLocatorMetadata = clusterTwoLocatorResult.getData();
    StatisticsSampleDataUtils.assertClusterTwoLocatorMetadata(clusterTwoLocatorMetadata);

    // cluster1-server1.gfs
    String clusterOneServerOneFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerOneFilePath)).findAny().orElse(null);
    assertThat(clusterOneServerOneResult).isNotNull();
    assertThat(clusterOneServerOneResult.isSuccess()).isTrue();
    assertThat(clusterOneServerOneResult.getData()).isNotNull();
    assertThat(clusterOneServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerOneFilePath);
    SamplingMetadata clusterOneServerOneMetadata = clusterOneServerOneResult.getData();
    StatisticsSampleDataUtils.assertClusterOneServerOneMetadata(clusterOneServerOneMetadata);

    // cluster1-server2.gfs
    String clusterOneServerTwoFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER2.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerTwoFilePath)).findAny().orElse(null);
    assertThat(clusterOneServerTwoResult).isNotNull();
    assertThat(clusterOneServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterOneServerTwoResult.getData()).isNotNull();
    assertThat(clusterOneServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerTwoFilePath);
    SamplingMetadata clusterOneServerTwoMetadata = clusterOneServerTwoResult.getData();
    StatisticsSampleDataUtils.assertClusterOneServerTwoMetadata(clusterOneServerTwoMetadata);

    // cluster2-server1.gfs
    String clusterTwoServerOneFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER1.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerOneFilePath)).findAny().orElse(null);
    assertThat(clusterTwoServerOneResult).isNotNull();
    assertThat(clusterTwoServerOneResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerOneResult.getData()).isNotNull();
    assertThat(clusterTwoServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerOneFilePath);
    SamplingMetadata clusterTwoServerOneMetadata = clusterTwoServerOneResult.getData();
    StatisticsSampleDataUtils.assertClusterTwoServerOneMetadata(clusterTwoServerOneMetadata);

    // cluster2-server2.gfs
    String clusterTwoServerTwoFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER2.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerTwoFilePath)).findAny().orElse(null);
    assertThat(clusterTwoServerTwoResult).isNotNull();
    assertThat(clusterTwoServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerTwoResult.getData()).isNotNull();
    assertThat(clusterTwoServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerTwoFilePath);
    SamplingMetadata clusterTwoServerTwoMetadata = clusterTwoServerTwoResult.getData();
    StatisticsSampleDataUtils.assertClusterTwoServerTwoMetadata(clusterTwoServerTwoMetadata);
  }

  @Test
  public void parseSamplingShouldReturnParsingErrorsWhenFileDoestNotExist() {
    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(Paths.get("nonExistingFile.gfs"), new ArrayList<>());

    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<Sampling> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(NoSuchFileException.class).hasMessage("nonExistingFile.gfs");
  }

  @Test
  public void parseSamplingShouldReturnBothParsingErrorsAndParsingSuccessesWhenParsingSucceedsForSomeFilesAndFailsForOthers() {
    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(
        StatisticsSampleDataUtils.rootFolder.toPath(), StatisticsSampleDataUtils.filters);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(9);

    // unparseableFile.gfs
    String unparseableFilePath = StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<Sampling> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().orElse(null);
    assertThat(unparseableResult).isNotNull();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // unparseableFile.gfs
    String unparseableCompressedFilePath = StatisticsSampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<Sampling> unparseableCompressedResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableCompressedFilePath)).findAny().orElse(null);
    assertThat(unparseableCompressedResult).isNotNull();
    assertThat(unparseableCompressedResult.isSuccess()).isFalse();
    assertThat(unparseableCompressedResult.getException()).isNotNull();
    assertThat(unparseableCompressedResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableCompressedFilePath);
    Exception unparseableCompressedException = unparseableCompressedResult.getException();
    assertThat(unparseableCompressedException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // SampleClient.gfs
    String sampleClientFilePath = StatisticsSampleDataUtils.SampleType.CLIENT.getFilePath();
    ParsingResult<Sampling> clientStatisticsResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(sampleClientFilePath)).findAny().orElse(null);
    assertThat(clientStatisticsResult).isNotNull();
    assertThat(clientStatisticsResult.isSuccess()).isTrue();
    assertThat(clientStatisticsResult.getData()).isNotNull();
    assertThat(clientStatisticsResult.getFile().toAbsolutePath().toString()).isEqualTo(sampleClientFilePath);
    StatisticsSampleDataUtils.assertClientSampling(clientStatisticsResult.getData());

    // cluster1-locator.gz
    String clusterOneLocatorFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();
    ParsingResult<Sampling> clusterOneLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneLocatorFilePath)).findAny().orElse(null);
    assertThat(clusterOneLocatorResult).isNotNull();
    assertThat(clusterOneLocatorResult.isSuccess()).isTrue();
    assertThat(clusterOneLocatorResult.getData()).isNotNull();
    assertThat(clusterOneLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneLocatorFilePath);
    StatisticsSampleDataUtils.assertClusterOneLocatorMetadata(clusterOneLocatorResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertLocatorSampling(clusterOneLocatorResult.getData(), 1);

    // cluster2-locator.gz
    String clusterTwoLocatorFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_LOCATOR.getFilePath();
    ParsingResult<Sampling> clusterTwoLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoLocatorFilePath)).findAny().orElse(null);
    assertThat(clusterTwoLocatorResult).isNotNull();
    assertThat(clusterTwoLocatorResult.isSuccess()).isTrue();
    assertThat(clusterTwoLocatorResult.getData()).isNotNull();
    assertThat(clusterTwoLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoLocatorFilePath);
    StatisticsSampleDataUtils.assertClusterTwoLocatorMetadata(clusterTwoLocatorResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertLocatorSampling(clusterTwoLocatorResult.getData(), 2);

    // cluster1-server1.gfs
    String clusterOneServerOneFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    ParsingResult<Sampling> clusterOneServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerOneFilePath)).findAny().orElse(null);
    assertThat(clusterOneServerOneResult).isNotNull();
    assertThat(clusterOneServerOneResult.isSuccess()).isTrue();
    assertThat(clusterOneServerOneResult.getData()).isNotNull();
    assertThat(clusterOneServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerOneFilePath);
    StatisticsSampleDataUtils.assertClusterOneServerOneMetadata(clusterOneServerOneResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertServerSampling(clusterOneServerOneResult.getData(), 1, 1);

    // cluster1-server2.gfs
    String clusterOneServerTwoFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER1_SERVER2.getFilePath();
    ParsingResult<Sampling> clusterOneServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerTwoFilePath)).findAny().orElse(null);
    assertThat(clusterOneServerTwoResult).isNotNull();
    assertThat(clusterOneServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterOneServerTwoResult.getData()).isNotNull();
    assertThat(clusterOneServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerTwoFilePath);
    StatisticsSampleDataUtils.assertClusterOneServerTwoMetadata(clusterOneServerTwoResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertServerSampling(clusterOneServerTwoResult.getData(), 2, 1);

    // cluster2-server1.gfs
    String clusterTwoServerOneFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER1.getFilePath();
    ParsingResult<Sampling> clusterTwoServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerOneFilePath)).findAny().orElse(null);
    assertThat(clusterTwoServerOneResult).isNotNull();
    assertThat(clusterTwoServerOneResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerOneResult.getData()).isNotNull();
    assertThat(clusterTwoServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerOneFilePath);
    StatisticsSampleDataUtils.assertClusterTwoServerOneMetadata(clusterTwoServerOneResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertServerSampling(clusterTwoServerOneResult.getData(), 1, 2);

    // cluster2-server2.gfs
    String clusterTwoServerTwoFilePath = StatisticsSampleDataUtils.SampleType.CLUSTER2_SERVER2.getFilePath();
    ParsingResult<Sampling> clusterTwoServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerTwoFilePath)).findAny().orElse(null);
    assertThat(clusterTwoServerTwoResult).isNotNull();
    assertThat(clusterTwoServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerTwoResult.getData()).isNotNull();
    assertThat(clusterTwoServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerTwoFilePath);
    StatisticsSampleDataUtils.assertClusterTwoServerTwoMetadata(clusterTwoServerTwoResult.getData().getMetadata());
    StatisticsSampleDataUtils.assertServerSampling(clusterTwoServerTwoResult.getData(), 2, 2);
  }
}
