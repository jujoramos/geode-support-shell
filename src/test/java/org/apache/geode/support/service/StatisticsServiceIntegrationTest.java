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
import org.apache.geode.support.test.SampleDataUtils;

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
    String clusterOneServerOneFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    assertThatThrownBy(() -> statisticsService.decompress(Paths.get(clusterOneServerOneFilePath), mockedFile.toPath())).isInstanceOf(IOException.class);
  }

  @Test
  public void decompressShouldDeleteTargetFileIfDecompressionFails() throws Exception {
    File uncompressedFile = temporaryFolder.newFile("cluster1-locator.gfs");
    String clusterOneLocatorFilePath = SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getFilePath();

    assertThatThrownBy(() -> statisticsService.decompress(Paths.get(clusterOneLocatorFilePath), uncompressedFile.toPath())).isInstanceOf(IOException.class).hasMessage("Not in GZIP format");
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).count()).isEqualTo(0);
  }

  @Test
  public void decompressShouldExecuteCorrectly() throws Exception {
    File uncompressedFile = temporaryFolder.newFile("cluster1-locator.gfs");
    String clusterOneLocatorFilePath = SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();

    assertThatCode(() -> statisticsService.decompress(Paths.get(clusterOneLocatorFilePath), uncompressedFile.toPath())).doesNotThrowAnyException();
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).count()).isEqualTo(1);
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).filter(path -> Files.isRegularFile(path) && path.getFileName().toString().equals("cluster1-locator.gfs")).count()).isEqualTo(1);

    // TODO: find a better way to asses the decompression result.
    SamplingMetadata decompressedMetadata = statisticsService.parseMetadata(uncompressedFile.toPath()).stream().findFirst().get().getData();
    SamplingMetadata uncompressedMetadata = statisticsService.parseMetadata(Paths.get(SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath())).stream().findFirst().get().getData();
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
    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(SampleDataUtils.rootFolder.toPath());
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(9);

    // unparseableFile.gfs
    String unparseableFilePath = SampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<SamplingMetadata> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().get();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // unparseableFile.gfs
    String unparseableCompressedFilePath = SampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<SamplingMetadata> unparseableCompressedResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableCompressedFilePath)).findAny().get();
    assertThat(unparseableCompressedResult.isSuccess()).isFalse();
    assertThat(unparseableCompressedResult.getException()).isNotNull();
    assertThat(unparseableCompressedResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableCompressedFilePath);
    Exception unparseableCompressedException = unparseableCompressedResult.getException();
    assertThat(unparseableCompressedException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // SampleClient.gfs
    String sampleClientFilePath = SampleDataUtils.SampleType.CLIENT.getFilePath();
    ParsingResult<SamplingMetadata> clientStatisticsResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(sampleClientFilePath)).findAny().get();
    assertThat(clientStatisticsResult.isSuccess()).isTrue();
    assertThat(clientStatisticsResult.getData()).isNotNull();
    assertThat(clientStatisticsResult.getFile().toAbsolutePath().toString()).isEqualTo(sampleClientFilePath);
    SamplingMetadata clientArchiveMetadata = clientStatisticsResult.getData();
    SampleDataUtils.assertClientMetadata(clientArchiveMetadata);

    // cluster1-locator.gz
    String clusterOneLocatorFilePath = SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneLocatorFilePath)).findAny().get();
    assertThat(clusterOneLocatorResult.isSuccess()).isTrue();
    assertThat(clusterOneLocatorResult.getData()).isNotNull();
    assertThat(clusterOneLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneLocatorFilePath);
    SamplingMetadata clusterOneLocatorMetadata = clusterOneLocatorResult.getData();
    SampleDataUtils.assertClusterOneLocatorMetadata(clusterOneLocatorMetadata);

    // cluster2-locator.gz
    String clusterTwoLocatorFilePath = SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoLocatorFilePath)).findAny().get();
    assertThat(clusterTwoLocatorResult.isSuccess()).isTrue();
    assertThat(clusterTwoLocatorResult.getData()).isNotNull();
    assertThat(clusterTwoLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoLocatorFilePath);
    SamplingMetadata clusterTwoLocatorMetadata = clusterTwoLocatorResult.getData();
    SampleDataUtils.assertClusterTwoLocatorMetadata(clusterTwoLocatorMetadata);

    // cluster1-server1.gfs
    String clusterOneServerOneFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerOneFilePath)).findAny().get();
    assertThat(clusterOneServerOneResult.isSuccess()).isTrue();
    assertThat(clusterOneServerOneResult.getData()).isNotNull();
    assertThat(clusterOneServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerOneFilePath);
    SamplingMetadata clusterOneServerOneMetadata = clusterOneServerOneResult.getData();
    SampleDataUtils.assertClusterOneServerOneMetadata(clusterOneServerOneMetadata);

    // cluster1-server2.gfs
    String clusterOneServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER2.getFilePath();
    ParsingResult<SamplingMetadata> clusterOneServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerTwoFilePath)).findAny().get();
    assertThat(clusterOneServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterOneServerTwoResult.getData()).isNotNull();
    assertThat(clusterOneServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerTwoFilePath);
    SamplingMetadata clusterOneServerTwoMetadata = clusterOneServerTwoResult.getData();
    SampleDataUtils.assertClusterOneServerTwoMetadata(clusterOneServerTwoMetadata);

    // cluster2-server1.gfs
    String clusterTwoServerOneFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER1.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerOneFilePath)).findAny().get();
    assertThat(clusterTwoServerOneResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerOneResult.getData()).isNotNull();
    assertThat(clusterTwoServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerOneFilePath);
    SamplingMetadata clusterTwoServerOneMetadata = clusterTwoServerOneResult.getData();
    SampleDataUtils.assertClusterTwoServerOneMetadata(clusterTwoServerOneMetadata);

    // cluster2-server2.gfs
    String clusterTwoServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER2.getFilePath();
    ParsingResult<SamplingMetadata> clusterTwoServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerTwoFilePath)).findAny().get();
    assertThat(clusterTwoServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerTwoResult.getData()).isNotNull();
    assertThat(clusterTwoServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerTwoFilePath);
    SamplingMetadata clusterTwoServerTwoMetadata = clusterTwoServerTwoResult.getData();
    SampleDataUtils.assertClusterTwoServerTwoMetadata(clusterTwoServerTwoMetadata);
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
    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(SampleDataUtils.rootFolder.toPath(), SampleDataUtils.filters);
    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(9);

    // unparseableFile.gfs
    String unparseableFilePath = SampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<Sampling> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().get();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // unparseableFile.gfs
    String unparseableCompressedFilePath = SampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<Sampling> unparseableCompressedResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableCompressedFilePath)).findAny().get();
    assertThat(unparseableCompressedResult.isSuccess()).isFalse();
    assertThat(unparseableCompressedResult.getException()).isNotNull();
    assertThat(unparseableCompressedResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableCompressedFilePath);
    Exception unparseableCompressedException = unparseableCompressedResult.getException();
    assertThat(unparseableCompressedException).isInstanceOf(IOException.class).hasMessage("Unexpected token byte value: 67");

    // SampleClient.gfs
    String sampleClientFilePath = SampleDataUtils.SampleType.CLIENT.getFilePath();
    ParsingResult<Sampling> clientStatisticsResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(sampleClientFilePath)).findAny().get();
    assertThat(clientStatisticsResult.isSuccess()).isTrue();
    assertThat(clientStatisticsResult.getData()).isNotNull();
    assertThat(clientStatisticsResult.getFile().toAbsolutePath().toString()).isEqualTo(sampleClientFilePath);
    SampleDataUtils.assertClientSampling(clientStatisticsResult.getData());

    // cluster1-locator.gz
    String clusterOneLocatorFilePath = SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();
    ParsingResult<Sampling> clusterOneLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneLocatorFilePath)).findAny().get();
    assertThat(clusterOneLocatorResult.isSuccess()).isTrue();
    assertThat(clusterOneLocatorResult.getData()).isNotNull();
    assertThat(clusterOneLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneLocatorFilePath);
    SampleDataUtils.assertClusterOneLocatorMetadata(clusterOneLocatorResult.getData().getMetadata());
    SampleDataUtils.assertLocatorSampling(clusterOneLocatorResult.getData());

    // cluster2-locator.gz
    String clusterTwoLocatorFilePath = SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getFilePath();
    ParsingResult<Sampling> clusterTwoLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoLocatorFilePath)).findAny().get();
    assertThat(clusterTwoLocatorResult.isSuccess()).isTrue();
    assertThat(clusterTwoLocatorResult.getData()).isNotNull();
    assertThat(clusterTwoLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoLocatorFilePath);
    SampleDataUtils.assertClusterTwoLocatorMetadata(clusterTwoLocatorResult.getData().getMetadata());
    SampleDataUtils.assertLocatorSampling(clusterTwoLocatorResult.getData());

    // cluster1-server1.gfs
    String clusterOneServerOneFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    ParsingResult<Sampling> clusterOneServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerOneFilePath)).findAny().get();
    assertThat(clusterOneServerOneResult.isSuccess()).isTrue();
    assertThat(clusterOneServerOneResult.getData()).isNotNull();
    assertThat(clusterOneServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerOneFilePath);
    SampleDataUtils.assertClusterOneServerOneMetadata(clusterOneServerOneResult.getData().getMetadata());
    SampleDataUtils.assertServerSampling(clusterOneServerOneResult.getData());

    // cluster1-server2.gfs
    String clusterOneServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER2.getFilePath();
    ParsingResult<Sampling> clusterOneServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerTwoFilePath)).findAny().get();
    assertThat(clusterOneServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterOneServerTwoResult.getData()).isNotNull();
    assertThat(clusterOneServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerTwoFilePath);
    SampleDataUtils.assertClusterOneServerTwoMetadata(clusterOneServerTwoResult.getData().getMetadata());
    SampleDataUtils.assertServerSampling(clusterOneServerTwoResult.getData());

    // cluster2-server1.gfs
    String clusterTwoServerOneFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER1.getFilePath();
    ParsingResult<Sampling> clusterTwoServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerOneFilePath)).findAny().get();
    assertThat(clusterTwoServerOneResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerOneResult.getData()).isNotNull();
    assertThat(clusterTwoServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerOneFilePath);
    SampleDataUtils.assertClusterTwoServerOneMetadata(clusterTwoServerOneResult.getData().getMetadata());
    SampleDataUtils.assertServerSampling(clusterTwoServerOneResult.getData());

    // cluster2-server2.gfs
    String clusterTwoServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER2.getFilePath();
    ParsingResult<Sampling> clusterTwoServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerTwoFilePath)).findAny().get();
    assertThat(clusterTwoServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerTwoResult.getData()).isNotNull();
    assertThat(clusterTwoServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerTwoFilePath);
    SampleDataUtils.assertClusterTwoServerTwoMetadata(clusterTwoServerTwoResult.getData().getMetadata());
    SampleDataUtils.assertServerSampling(clusterTwoServerTwoResult.getData());
  }
}
