package org.apache.geode.support.domain.logs;

import java.time.ZoneId;
import java.util.Properties;

public class LogMetadata {
  private final String fileName;
  private final ZoneId timeZoneId;
  private final long startTimeStamp;
  private final long finishTimeStamp;
  private final String productVersion;
  private final String operatingSystem;
  private final Properties systemProperties;

  private LogMetadata(String fileName, ZoneId timeZoneId, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem, Properties systemProperties) {
    this.fileName = fileName;
    this.timeZoneId = timeZoneId;
    this.startTimeStamp = startTimeStamp;
    this.finishTimeStamp = finishTimeStamp;
    this.productVersion = productVersion;
    this.operatingSystem = operatingSystem;
    this.systemProperties = systemProperties;
  }

  public static LogMetadata of(String fileName, long startTimeStamp, long finishTimeStamp) {
    return new LogMetadata(fileName, null, startTimeStamp, finishTimeStamp, null, null, null);
  }

  public static LogMetadata of(String fileName, ZoneId timeZoneId, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem, Properties systemProperties) {
    return new LogMetadata(fileName, timeZoneId, startTimeStamp, finishTimeStamp, productVersion, operatingSystem, systemProperties);
  }

  public String getFileName() {
    return fileName;
  }

  public ZoneId getTimeZoneId() {
    return timeZoneId;
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

  public Properties getSystemProperties() {
    return systemProperties;
  }

  @Override
  public String toString() {
    return "LogMetadata{" +
        "fileName='" + fileName + '\'' +
        ", timeZoneId=" + timeZoneId +
        ", startTimeStamp=" + startTimeStamp +
        ", finishTimeStamp=" + finishTimeStamp +
        ", productVersion='" + productVersion + '\'' +
        ", operatingSystem='" + operatingSystem + '\'' +
        ", systemProperties=" + systemProperties +
        '}';
  }
}
