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
package org.apache.geode.support.domain.statistics;

import java.time.ZoneId;

/**
 *
 */
public class SamplingMetadata {
  private final int version;
  private final String fileName;
  private final ZoneId timeZoneId;
  private final boolean compressed;
  private final long startTimeStamp;
  private final long finishTimeStamp;
  private final String productVersion;
  private final String operatingSystem;

  public SamplingMetadata(String fileName, int version, boolean compressed, ZoneId timeZoneId, long startTimeStamp, long finishTimeStamp, String productVersion, String operatingSystem) {
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

  @Override
  public String toString() {
    return "SamplingMetadata[" +
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
