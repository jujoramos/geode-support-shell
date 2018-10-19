package org.apache.geode.support.domain.logs;

import java.util.Properties;

public class LogMetadata {
  private final String fileName;
  private final long startTimeStamp;
  private final long finishTimeStamp;
  private final String productVersion;
  private final String operatingSystem;
  private final Properties systemProperties;

  private LogMetadata(String fileName, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem, Properties systemProperties) {
    this.fileName = fileName;
    this.startTimeStamp = startTimeStamp;
    this.finishTimeStamp = finishTimeStamp;
    this.productVersion = productVersion;
    this.operatingSystem = operatingSystem;
    this.systemProperties = systemProperties;
  }

  public static LogMetadata of(String fileName, long startTimeStamp, long finishTimeStamp) {
    return new LogMetadata(fileName, startTimeStamp, finishTimeStamp, null, null, null);
  }

  public static LogMetadata of(String fileName, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem, Properties systemProperties) {
    return new LogMetadata(fileName, startTimeStamp, finishTimeStamp, productVersion, operatingSystem, systemProperties);
  }

  public String getFileName() {
    return fileName;
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
    return "LogMetadata[" +
        "fileName='" + fileName + '\'' +
        ", startTimeStamp=" + startTimeStamp +
        ", finishTimeStamp=" + finishTimeStamp +
        ", productVersion='" + productVersion + '\'' +
        ", operatingSystem='" + operatingSystem + '\'' +
        ", systemProperties='" + systemProperties + '\'' +
        ']';
  }
}
