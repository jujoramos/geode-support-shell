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
package org.apache.geode.support.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.geode.internal.statistics.ValueFilter;
import org.apache.geode.support.domain.statistics.Category;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.domain.statistics.filters.SimpleValueFilter;
import org.apache.geode.support.utils.FormatUtils;

/**
 * Class used in integration tests to store and assert the hardcoded contents of the statistic files within the samples directory.
 *
 * TODO: This should be removed once the tool has the ability to generate statistics files on its own, without using already created files for testing.
 */
public final class SampleDataUtils {
  public static final List<ValueFilter> filters;
  private static final SamplingMetadata clientMetadata;
  private static final SamplingMetadata cluster1locatorMetadata;
  private static final SamplingMetadata cluster1Server1Metadata;
  private static final SamplingMetadata cluster1Server2Metadata;
  private static final SamplingMetadata cluster2locatorMetadata;
  private static final SamplingMetadata cluster2Server1Metadata;
  private static final SamplingMetadata cluster2Server2Metadata;
  public static final File rootFolder = new File(SampleDataUtils.class.getResource("/samples").getFile());
  public static final File corruptedFolder = rootFolder.toPath().resolve("corrupted").toFile();
  public static final File uncorruptedFolder = rootFolder.toPath().resolve("uncorrupted").toFile();

  /**
   *
   */
  public enum SampleType {
    CLIENT,
    UNPARSEABLE,
    UNPARSEABLE_COMPRESSED,
    CLUSTER1_LOCATOR,
    CLUSTER1_SERVER1,
    CLUSTER1_SERVER2,
    CLUSTER2_LOCATOR,
    CLUSTER2_SERVER1,
    CLUSTER2_SERVER2;

    public String getFilePath() {
      switch(this) {
        case CLIENT: return clientMetadata.getFileName();
        case CLUSTER1_LOCATOR: return cluster1locatorMetadata.getFileName();
        case CLUSTER1_SERVER1: return cluster1Server1Metadata.getFileName();
        case CLUSTER1_SERVER2: return cluster1Server2Metadata.getFileName();
        case CLUSTER2_LOCATOR: return cluster2locatorMetadata.getFileName();
        case CLUSTER2_SERVER1: return cluster2Server1Metadata.getFileName();
        case CLUSTER2_SERVER2: return cluster2Server2Metadata.getFileName();
        case UNPARSEABLE: return corruptedFolder.getAbsolutePath() + File.separator + "unparseableFile.gfs";
        case UNPARSEABLE_COMPRESSED: return corruptedFolder.getAbsolutePath() + File.separator + "unparseableFile.gz";
        default: throw new IllegalArgumentException("Execution shouldn't reach this point.");
      }
    }

    public String getRelativeFilePath(Path basePath) {
      switch(this) {
        case CLIENT: return FormatUtils.relativizePath(basePath, new File(clientMetadata.getFileName()).toPath());
        case CLUSTER1_LOCATOR: return FormatUtils.relativizePath(basePath, new File(cluster1locatorMetadata.getFileName()).toPath());
        case CLUSTER1_SERVER1: return FormatUtils.relativizePath(basePath, new File(cluster1Server1Metadata.getFileName()).toPath());
        case CLUSTER1_SERVER2: return FormatUtils.relativizePath(basePath, new File(cluster1Server2Metadata.getFileName()).toPath());
        case CLUSTER2_LOCATOR: return FormatUtils.relativizePath(basePath, new File(cluster2locatorMetadata.getFileName()).toPath());
        case CLUSTER2_SERVER1: return FormatUtils.relativizePath(basePath, new File(cluster2Server1Metadata.getFileName()).toPath());
        case CLUSTER2_SERVER2: return FormatUtils.relativizePath(basePath, new File(cluster2Server2Metadata.getFileName()).toPath());
        case UNPARSEABLE: return FormatUtils.relativizePath(basePath, new File(corruptedFolder.getAbsolutePath() + File.separator + "unparseableFile.gfs").toPath());
        case UNPARSEABLE_COMPRESSED: return FormatUtils.relativizePath(basePath, new File(corruptedFolder.getAbsolutePath() + File.separator + "unparseableFile.gz").toPath());
        default: throw new IllegalArgumentException("Execution shouldn't reach this point.");
      }
    }
  }

  static {
    String basePath = uncorruptedFolder.getAbsolutePath() + File.separator;
    clientMetadata = new SamplingMetadata(basePath + "sampleClient.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727611058L, 1521731228603L, "GemFire 9.1.0 #root 26 as of 2017-07-11 18:00:39 +0000", "Mac OS X 10.13.3");
    cluster1locatorMetadata = new SamplingMetadata(basePath + "cluster1-locator.gz", 4, true, ZoneId.of("Europe/Dublin"), 1521727569159L, 1521731825118L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster1Server1Metadata = new SamplingMetadata(basePath + "cluster1-server1.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727582894L, 1521731826120L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster1Server2Metadata = new SamplingMetadata(basePath + "cluster1-server2.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727582886L, 1521731825048L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster2locatorMetadata = new SamplingMetadata(basePath + "cluster2-locator.gz", 4, true, ZoneId.of("America/Chicago"), 1521727584309L, 1521731824225L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");
    cluster2Server1Metadata = new SamplingMetadata(basePath + "cluster2-server1.gfs", 4, false, ZoneId.of("America/Chicago"), 1521727593055L, 1521731823828L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");
    cluster2Server2Metadata = new SamplingMetadata(basePath + "cluster2-server2.gfs", 4, false, ZoneId.of("America/Chicago"), 1521727593056L, 1521731824281L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");

    filters = new ArrayList<>();
    // Shared by all members.
    filters.add(new SimpleValueFilter("VMStats", null, "threads", null));
    filters.add(new SimpleValueFilter("StatSampler", null, "delayDuration", null));
    // Client only.
    filters.add(new SimpleValueFilter("PoolStats", null, "clientOps", null));
    // Servers and locators only.
    filters.add(new SimpleValueFilter("DistributionStats", null, "replyWaitsInProgress", null));
    // Locator Only.
    filters.add(new SimpleValueFilter("LocatorStats", null, "serverLoadUpdates", null));
    // Servers only.
    filters.add(new SimpleValueFilter("GatewaySenderStatistics", null, "eventsDistributed", null));
    filters.add(new SimpleValueFilter("GatewayReceiverStatistics", null, "createRequests", null));

  }

  private static void assertSamplingMetadata(SamplingMetadata expectedMetadata, SamplingMetadata actualMetadata) {
    assertThat(actualMetadata.getFileName()).isEqualTo(expectedMetadata.getFileName());
    assertThat(actualMetadata.getVersion()).isEqualTo(expectedMetadata.getVersion());
    assertThat(actualMetadata.isCompressed()).isEqualTo(expectedMetadata.isCompressed());
    assertThat(actualMetadata.getTimeZoneId()).isEqualTo(expectedMetadata.getTimeZoneId());
    assertThat(actualMetadata.getStartTimeStamp()).isEqualTo(expectedMetadata.getStartTimeStamp());
    assertThat(actualMetadata.getFinishTimeStamp()).isEqualTo(expectedMetadata.getFinishTimeStamp());
    assertThat(actualMetadata.getProductVersion()).isEqualTo(expectedMetadata.getProductVersion());
    assertThat(actualMetadata.getOperatingSystem()).isEqualTo(expectedMetadata.getOperatingSystem());
  }

  private static void assertSamplingMetadata(SamplingMetadata metadata, Path basePath, ZoneId zoneId, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    ZoneId formatZoneId = zoneId != null ? zoneId : metadata.getTimeZoneId();
    Instant startInstant = Instant.ofEpochMilli(metadata.getStartTimeStamp());
    Instant finishInstant = Instant.ofEpochMilli(metadata.getFinishTimeStamp());
    ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formatZoneId);
    ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formatZoneId);

    String expectedFileName = FormatUtils.relativizePath(basePath, new File(metadata.getFileName()).toPath());
    String expectedProductVersion = FormatUtils.trimProductVersion(metadata.getProductVersion());
    assertThat(fileName).isEqualTo(expectedFileName);
    assertThat(productVersion).isEqualTo(expectedProductVersion);
    assertThat(operatingSystem).isEqualTo(metadata.getOperatingSystem());
    assertThat(timeZoneId).isEqualTo(metadata.getTimeZoneId().toString());
    assertThat(startTimeStamp).isEqualTo(startTime.format(FormatUtils.getDateTimeFormatter()));
    assertThat(finishTimeStamp).isEqualTo(finishTime.format(FormatUtils.getDateTimeFormatter()));
  }

  public static void assertClientMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(clientMetadata, actualMetadata);
  }

  public static void assertClusterOneLocatorMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster1locatorMetadata, actualMetadata);
  }

  public static void assertClusterOneServerOneMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster1Server1Metadata, actualMetadata);
  }

  public static void assertClusterOneServerTwoMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster1Server2Metadata, actualMetadata);
  }

  public static void assertClusterTwoLocatorMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster2locatorMetadata, actualMetadata);
  }

  public static void assertClusterTwoServerOneMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster2Server1Metadata, actualMetadata);
  }

  public static void assertClusterTwoServerTwoMetadata(SamplingMetadata actualMetadata) {
    assertSamplingMetadata(cluster2Server2Metadata, actualMetadata);
  }

  public static void assertClientMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(clientMetadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneLocatorMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster1locatorMetadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneServerOneMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster1Server1Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneServerTwoMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster1Server2Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoLocatorMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster2locatorMetadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoServerOneMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster2Server1Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoServerTwoMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertSamplingMetadata(cluster2Server2Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  private static void assertStatistic(Statistic statistic, String name, String description, boolean isCounter, String units) {
    assertThat(statistic.getName()).isEqualTo(name);
    assertThat(statistic.getDescription()).isEqualTo(description);
    assertThat(statistic.getUnits()).isEqualTo(units);
    assertThat(statistic.isCounter()).isEqualTo(isCounter);
  }

  private static void assertCommonCategories(Map<String, Category> categoryMap) {
    assertThat(categoryMap.containsKey("StatSampler")).isTrue();
    Category statSamplerCategory = categoryMap.get("StatSampler");
    assertThat(statSamplerCategory.hasStatistic("delayDuration"));
    assertStatistic(statSamplerCategory.getStatistic("delayDuration"), "delayDuration", "Actual duration of sampling delay taken before taking this sample.", false, "milliseconds");

    assertThat(categoryMap.containsKey("VMStats")).isTrue();
    Category vmStatsCategory = categoryMap.get("VMStats");
    assertThat(vmStatsCategory.hasStatistic("threads"));
    assertStatistic(vmStatsCategory.getStatistic("threads"), "threads", "Current number of live threads (both daemon and non-daemon) in this VM.", false, "threads");
  }

  private static void assertDistributionStatsCategory(Map<String, Category> categoryMap) {
    assertThat(categoryMap.containsKey("DistributionStats")).isTrue();
    Category distributionStatsCategory = categoryMap.get("DistributionStats");
    assertThat(distributionStatsCategory.hasStatistic("replyWaitsInProgress"));
    assertStatistic(distributionStatsCategory.getStatistic("replyWaitsInProgress"), "replyWaitsInProgress", "Current number of threads waiting for a reply.", false, "operations");
  }

  public static void assertClientSampling(Sampling clientSampling) {
    assertSamplingMetadata(clientMetadata, clientSampling.getMetadata());
    assertThat(clientSampling.getCategories()).isNotNull();
    Map<String, Category> categoryMap = clientSampling.getCategories();
    assertCommonCategories(categoryMap);

    assertThat(categoryMap.containsKey("PoolStats")).isTrue();
    Category poolStatsCategory = categoryMap.get("PoolStats");
    assertThat(poolStatsCategory.hasStatistic("clientOps"));
    assertStatistic(poolStatsCategory.getStatistic("clientOps"), "clientOps", "Total number of clientOps completed successfully", true, "clientOps");
  }

  public static void assertLocatorSampling(Sampling locatorSampling) {
    assertThat(locatorSampling.getCategories()).isNotNull();
    Map<String, Category> categoryMap = locatorSampling.getCategories();
    assertCommonCategories(categoryMap);
    assertDistributionStatsCategory(categoryMap);

    assertThat(categoryMap.containsKey("LocatorStats")).isTrue();
    Category poolStatsCategory = categoryMap.get("LocatorStats");
    assertThat(poolStatsCategory.hasStatistic("serverLoadUpdates"));
    assertStatistic(poolStatsCategory.getStatistic("serverLoadUpdates"), "serverLoadUpdates", "Total number of times a server load update has been received.", true, "updates");
  }

  public static void assertServerSampling(Sampling serverSampling) {
    assertThat(serverSampling.getCategories()).isNotNull();
    Map<String, Category> categoryMap = serverSampling.getCategories();
    assertCommonCategories(categoryMap);
    assertDistributionStatsCategory(categoryMap);

    assertThat(categoryMap.containsKey("GatewaySenderStatistics")).isTrue();
    Category poolStatsCategory = categoryMap.get("GatewaySenderStatistics");
    assertThat(poolStatsCategory.hasStatistic("eventsDistributed"));
    assertStatistic(poolStatsCategory.getStatistic("eventsDistributed"), "eventsDistributed", "Number of events removed from the event queue and sent.", true, "operations");

    assertThat(categoryMap.containsKey("GatewayReceiverStatistics")).isTrue();
    Category distributionStatsCategory = categoryMap.get("GatewayReceiverStatistics");
    assertThat(distributionStatsCategory.hasStatistic("createRequests"));
    assertStatistic(distributionStatsCategory.getStatistic("createRequests"), "createRequests", "total number of create operations received by this GatewayReceiver", true, "operations");
  }
}
