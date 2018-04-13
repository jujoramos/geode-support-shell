package org.apache.geode.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.geode.support.domain.statistics.StatisticFileMetadata;
import org.apache.geode.support.utils.FormatUtils;

/**
 * Class used in integration tests to store and assert the hardcoded contents of the statistic files within the samples directory.
 * This should be removed once the tool has the ability to generate statistics files on its own, , without depending on an already created set of files for testing.
 */
public final class SampleDataUtils {
  private static final StatisticFileMetadata clientMetadata;
  private static final StatisticFileMetadata cluster1locatorMetadata;
  private static final StatisticFileMetadata cluster1Server1Metadata;
  private static final StatisticFileMetadata cluster1Server2Metadata;
  private static final StatisticFileMetadata cluster2locatorMetadata;
  private static final StatisticFileMetadata cluster2Server1Metadata;
  private static final StatisticFileMetadata cluster2Server2Metadata;
  public static final File rootFolder = new File(SampleDataUtils.class.getResource("/samples").getFile());

  /**
   *
   */
  public enum SampleType {
    CLIENT,
    UNPARSEABLE,
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
        case UNPARSEABLE: return rootFolder.getAbsolutePath() + File.separator + "unparseableFile.gfs";
        default: throw new IllegalArgumentException("Execution shouldn't reach this point.");
      }
    }

    public String getRelativeFilePath() {
      switch(this) {
        case CLIENT: return FormatUtils.relativizePath(rootFolder.toPath(), new File(clientMetadata.getFileName()).toPath());
        case CLUSTER1_LOCATOR: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster1locatorMetadata.getFileName()).toPath());
        case CLUSTER1_SERVER1: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster1Server1Metadata.getFileName()).toPath());
        case CLUSTER1_SERVER2: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster1Server2Metadata.getFileName()).toPath());
        case CLUSTER2_LOCATOR: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster2locatorMetadata.getFileName()).toPath());
        case CLUSTER2_SERVER1: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster2Server1Metadata.getFileName()).toPath());
        case CLUSTER2_SERVER2: return FormatUtils.relativizePath(rootFolder.toPath(), new File(cluster2Server2Metadata.getFileName()).toPath());
        case UNPARSEABLE: return FormatUtils.relativizePath(rootFolder.toPath(), new File(rootFolder.getAbsolutePath() + File.separator + "unparseableFile.gfs").toPath());
        default: throw new IllegalArgumentException("Execution shouldn't reach this point.");
      }
    }
  }

  static {
    String basePath = rootFolder.getAbsolutePath() + File.separator;
    clientMetadata = new StatisticFileMetadata(basePath + "sampleClient.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727611058L, 1521731228603L, "GemFire 9.1.0 #root 26 as of 2017-07-11 18:00:39 +0000", "Mac OS X 10.13.3");
    cluster1locatorMetadata = new StatisticFileMetadata(basePath + "cluster1-locator.gz", 4, true, ZoneId.of("Europe/Dublin"), 1521727569159L, 1521731825118L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster1Server1Metadata = new StatisticFileMetadata(basePath + "cluster1-server1.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727582894L, 1521731826120L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster1Server2Metadata = new StatisticFileMetadata(basePath + "cluster1-server2.gfs", 4, false, ZoneId.of("Europe/Dublin"), 1521727582886L, 1521731825048L, "GemFire 9.3.0 #root 12 as of 2018-01-26 16:35:20 +0000", "Mac OS X 10.13.3");
    cluster2locatorMetadata = new StatisticFileMetadata(basePath + "cluster2-locator.gz", 4, true, ZoneId.of("America/Chicago"), 1521727584309L, 1521731824225L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");
    cluster2Server1Metadata = new StatisticFileMetadata(basePath + "cluster2-server1.gfs", 4, false, ZoneId.of("America/Chicago"), 1521727593055L, 1521731823828L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");
    cluster2Server2Metadata = new StatisticFileMetadata(basePath + "cluster2-server2.gfs", 4, false, ZoneId.of("America/Chicago"), 1521727593056L, 1521731824281L, "GemFire 8.2.8 #build 29 as of Tue, 5 Dec 2017 11:51:14 -0800", "Mac OS X 10.13.3");
  }

  /**
   *
   * @param expectedMetadata
   * @param actualMetadata
   */
  private static void assertStatisticArchiveMetadata(StatisticFileMetadata expectedMetadata, StatisticFileMetadata actualMetadata) {
    assertThat(actualMetadata.getFileName()).isEqualTo(expectedMetadata.getFileName());
    assertThat(actualMetadata.getVersion()).isEqualTo(expectedMetadata.getVersion());
    assertThat(actualMetadata.isCompressed()).isEqualTo(expectedMetadata.isCompressed());
    assertThat(actualMetadata.getTimeZoneId()).isEqualTo(expectedMetadata.getTimeZoneId());
    assertThat(actualMetadata.getStartTimeStamp()).isEqualTo(expectedMetadata.getStartTimeStamp());
    assertThat(actualMetadata.getFinishTimeStamp()).isEqualTo(expectedMetadata.getFinishTimeStamp());
    assertThat(actualMetadata.getProductVersion()).isEqualTo(expectedMetadata.getProductVersion());
    assertThat(actualMetadata.getOperatingSystem()).isEqualTo(expectedMetadata.getOperatingSystem());
  }

  private static void assertStatisticArchiveMetadata(StatisticFileMetadata metadata, ZoneId zoneId, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    ZoneId formatZoneId = zoneId != null ? zoneId : metadata.getTimeZoneId();
    Instant startInstant = Instant.ofEpochMilli(metadata.getStartTimeStamp());
    Instant finishInstant = Instant.ofEpochMilli(metadata.getFinishTimeStamp());
    ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formatZoneId);
    ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formatZoneId);

    String expectedFileName = FormatUtils.relativizePath(rootFolder.toPath(), new File(metadata.getFileName()).toPath());
    String expectedProductVersion = FormatUtils.trimProductVersion(metadata.getProductVersion());
    assertThat(fileName).isEqualTo(expectedFileName);
    assertThat(productVersion).isEqualTo(expectedProductVersion);
    assertThat(operatingSystem).isEqualTo(metadata.getOperatingSystem());
    assertThat(timeZoneId).isEqualTo(metadata.getTimeZoneId().toString());
    assertThat(startTimeStamp).isEqualTo(startTime.format(FormatUtils.getDateTimeFormatter()));
    assertThat(finishTimeStamp).isEqualTo(finishTime.format(FormatUtils.getDateTimeFormatter()));
  }

  public static void assertClientMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(clientMetadata, actualMetadata);
  }

  public static void assertClusterOneLocatorMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster1locatorMetadata, actualMetadata);
  }

  public static void assertClusterOneServerOneMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster1Server1Metadata, actualMetadata);
  }

  public static void assertClusterOneServerTwoMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster1Server2Metadata, actualMetadata);
  }

  public static void assertClusterTwoLocatorMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster2locatorMetadata, actualMetadata);
  }

  public static void assertClusterTwoServerOneMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster2Server1Metadata, actualMetadata);
  }

  public static void assertClusterTwoServerTwoMetadata(StatisticFileMetadata actualMetadata) {
    assertStatisticArchiveMetadata(cluster2Server2Metadata, actualMetadata);
  }

  public static void assertClientMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(clientMetadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneLocatorMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster1locatorMetadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneServerOneMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster1Server1Metadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterOneServerTwoMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster1Server2Metadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoLocatorMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster2locatorMetadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoServerOneMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster2Server1Metadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertClusterTwoServerTwoMetadata(ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertStatisticArchiveMetadata(cluster2Server2Metadata, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }
}
