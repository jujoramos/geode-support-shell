package org.apache.geode.support.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.geode.support.SampleDataUtils;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;

public class StatisticsServiceIntegrationTest {
  private StatisticsService statisticsService;

  @Before
  public void setUp() {
    this.statisticsService = new DefaultStatisticsService();
  }

  @Test
  @Ignore
  public void manualTestForDebugging() {
    statisticsService.parseMetadata(null);
  }

  @Test
  public void parseMetadataShouldReturnParsingErrorsWhenFileDoestNotExist() {
    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(Paths.get("nonExistingFile.gfs"));

    assertThat(parsingResults.size()).isEqualTo(1);
    ParsingResult<StatisticFileMetadata> parsingResult = parsingResults.get(0);
    assertThat(parsingResult.isSuccess()).isFalse();
    System.out.println(parsingResult.getException());
    assertThat(parsingResult.getException()).isNotNull();
    assertThat(parsingResult.getException()).isInstanceOf(NoSuchFileException.class).hasMessage("nonExistingFile.gfs");
  }

  @Test
  public void parseMetadataShouldCorrectlyHandleCompressedAndRegularFilesFromDifferentMemberTypes() {
    List<ParsingResult<StatisticFileMetadata>> parsingResults = statisticsService.parseMetadata(SampleDataUtils.rootFolder.toPath());

    assertThat(parsingResults).isNotNull();
    assertThat(parsingResults.size()).isEqualTo(8);

    // unparseableFile.gfs
    String unparseableFilePath = SampleDataUtils.SampleType.UNPARSEABLE.getFilePath();
    ParsingResult<StatisticFileMetadata> unparseableResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(unparseableFilePath)).findAny().get();
    assertThat(unparseableResult.isSuccess()).isFalse();
    assertThat(unparseableResult.getException()).isNotNull();
    assertThat(unparseableResult.getFile().toAbsolutePath().toString()).isEqualTo(unparseableFilePath);
    Exception unparseableException = unparseableResult.getException();
    assertThat(unparseableException).isInstanceOf(RuntimeException.class).hasMessage("There was a problem while parsing file " + unparseableFilePath + ".");

    // SampleClient.gfs
    String sampleClientFilePath = SampleDataUtils.SampleType.CLIENT.getFilePath();
    ParsingResult<StatisticFileMetadata> clientStatisticsResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(sampleClientFilePath)).findAny().get();
    assertThat(clientStatisticsResult.isSuccess()).isTrue();
    assertThat(clientStatisticsResult.getData()).isNotNull();
    assertThat(clientStatisticsResult.getFile().toAbsolutePath().toString()).isEqualTo(sampleClientFilePath);
    StatisticFileMetadata clientArchiveMetadata = clientStatisticsResult.getData();
    SampleDataUtils.assertClientMetadata(clientArchiveMetadata);

    // cluster1-locator.gz
    String clusterOneLocatorFilePath = SampleDataUtils.SampleType.CLUSTER1_LOCATOR.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterOneLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneLocatorFilePath)).findAny().get();
    assertThat(clusterOneLocatorResult.isSuccess()).isTrue();
    assertThat(clusterOneLocatorResult.getData()).isNotNull();
    assertThat(clusterOneLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneLocatorFilePath);
    StatisticFileMetadata clusterOneLocatorMetadata = clusterOneLocatorResult.getData();
    SampleDataUtils.assertClusterOneLocatorMetadata(clusterOneLocatorMetadata);

    // cluster2-locator.gz
    String clusterTwoLocatorFilePath = SampleDataUtils.SampleType.CLUSTER2_LOCATOR.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterTwoLocatorResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoLocatorFilePath)).findAny().get();
    assertThat(clusterTwoLocatorResult.isSuccess()).isTrue();
    assertThat(clusterTwoLocatorResult.getData()).isNotNull();
    assertThat(clusterTwoLocatorResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoLocatorFilePath);
    StatisticFileMetadata clusterTwoLocatorMetadata = clusterTwoLocatorResult.getData();
    SampleDataUtils.assertClusterTwoLocatorMetadata(clusterTwoLocatorMetadata);

    // cluster1-server1.gfs
    String clusterOneServerOneFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER1.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterOneServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerOneFilePath)).findAny().get();
    assertThat(clusterOneServerOneResult.isSuccess()).isTrue();
    assertThat(clusterOneServerOneResult.getData()).isNotNull();
    assertThat(clusterOneServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerOneFilePath);
    StatisticFileMetadata clusterOneServerOneMetadata = clusterOneServerOneResult.getData();
    SampleDataUtils.assertClusterOneServerOneMetadata(clusterOneServerOneMetadata);

    // cluster1-server2.gfs
    String clusterOneServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER1_SERVER2.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterOneServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterOneServerTwoFilePath)).findAny().get();
    assertThat(clusterOneServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterOneServerTwoResult.getData()).isNotNull();
    assertThat(clusterOneServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterOneServerTwoFilePath);
    StatisticFileMetadata clusterOneServerTwoMetadata = clusterOneServerTwoResult.getData();
    SampleDataUtils.assertClusterOneServerTwoMetadata(clusterOneServerTwoMetadata);

    // cluster2-server1.gfs
    String clusterTwoServerOneFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER1.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterTwoServerOneResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerOneFilePath)).findAny().get();
    assertThat(clusterTwoServerOneResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerOneResult.getData()).isNotNull();
    assertThat(clusterTwoServerOneResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerOneFilePath);
    StatisticFileMetadata clusterTwoServerOneMetadata = clusterTwoServerOneResult.getData();
    SampleDataUtils.assertClusterTwoServerOneMetadata(clusterTwoServerOneMetadata);

    // cluster2-server2.gfs
    String clusterTwoServerTwoFilePath = SampleDataUtils.SampleType.CLUSTER2_SERVER2.getFilePath();
    ParsingResult<StatisticFileMetadata> clusterTwoServerTwoResult = parsingResults.stream().filter(result -> result.getFile().toAbsolutePath().toString().equals(clusterTwoServerTwoFilePath)).findAny().get();
    assertThat(clusterTwoServerTwoResult.isSuccess()).isTrue();
    assertThat(clusterTwoServerTwoResult.getData()).isNotNull();
    assertThat(clusterTwoServerTwoResult.getFile().toAbsolutePath().toString()).isEqualTo(clusterTwoServerTwoFilePath);
    StatisticFileMetadata clusterTwoServerTwoMetadata = clusterTwoServerTwoResult.getData();
    SampleDataUtils.assertClusterTwoServerTwoMetadata(clusterTwoServerTwoMetadata);
  }
}
