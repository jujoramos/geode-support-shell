package org.apache.geode.support.domain.statistics;

import java.time.ZoneId;

/**
 *
 */
public class StatisticFileMetadata {
  private final int version;
  private final String fileName;
  private final ZoneId timeZoneId;
  private final boolean compressed;
  private final long startTimeStamp;
  private final long finishTimeStamp;
  private final String productVersion;
  private final String operatingSystem;

  /**
   *
   * @param fileName
   * @param version
   * @param compressed
   * @param timeZoneId
   * @param startTimeStamp
   * @param finishTimeStamp
   * @param productVersion
   * @param operatingSystem
   */
  public StatisticFileMetadata(String fileName, int version, boolean compressed, ZoneId timeZoneId, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem) {
    this.fileName = fileName;
    this.version = version;
    this.timeZoneId = timeZoneId;
    this.compressed = compressed;
    this.startTimeStamp = startTimeStamp;
    this.finishTimeStamp = finishTimeStamp;
    this.productVersion = productVersion;
    this.operatingSystem = operatingSystem;
  }

  public String getFileName() {
    return fileName;
  }

  public int getVersion() {
    return version;
  }

  public ZoneId getTimeZoneId() {
    return timeZoneId;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public long getStartTimeStamp() {
    return startTimeStamp;
  }

  public long getFinishTimeStamp() {
    return finishTimeStamp;
  }

  public String getProductVersion() {
    return productVersion;
  }

  public String getOperatingSystem() {
    return operatingSystem;
  }

  /**
   *
   * @return
   */
  @Override
  public String toString() {
    return "StatisticFileMetadata[" +
        "fileName='" + fileName + '\'' +
        ", version=" + version +
        ", timeZoneId=" + timeZoneId +
        ", startTimeStamp=" + startTimeStamp +
        ", finishTimeStamp=" + finishTimeStamp +
        ", productVersion='" + productVersion + '\'' +
        ", operatingSystem='" + operatingSystem + '\'' +
        ']';
  }
}
