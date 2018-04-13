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
package org.apache.geode.internal.statistics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.apache.geode.support.domain.marker.GeodeExtension;
import org.apache.geode.support.domain.marker.GeodeReplacement;

/**
 * Describes some global information about the archive.
 */
@GeodeReplacement(changes = "Made public.")
public class ArchiveInfo {
  private final StatArchiveFile archive;
  private final byte archiveVersion;
  private final long startTimeStamp; // in milliseconds
  private final long systemStartTimeStamp; // in milliseconds
  private final int timeZoneOffset;
  private final String timeZoneName;
  private final String systemDirectory;
  private final long systemId;
  private final String productVersion;
  private final String os;
  private final String machine;

  public ArchiveInfo(StatArchiveFile archive, byte archiveVersion, long startTimeStamp,
                     long systemStartTimeStamp, int timeZoneOffset, String timeZoneName, String systemDirectory,
                     long systemId, String productVersion, String os, String machine) {
    this.archive = archive;
    this.archiveVersion = archiveVersion;
    this.startTimeStamp = startTimeStamp;
    this.systemStartTimeStamp = systemStartTimeStamp;
    this.timeZoneOffset = timeZoneOffset;
    this.timeZoneName = timeZoneName;
    this.systemDirectory = systemDirectory;
    this.systemId = systemId;
    this.productVersion = productVersion;
    this.os = os;
    this.machine = machine;
    archive.setTimeZone(getTimeZone());
  }

  /**
   * Returns the difference, measured in milliseconds, between the time the archive file was
   * create and midnight, January 1, 1970 UTC.
   */
  public long getStartTimeMillis() {
    return this.startTimeStamp;
  }

  /**
   * Returns the difference, measured in milliseconds, between the time the archived system was
   * started and midnight, January 1, 1970 UTC.
   */
  public long getSystemStartTimeMillis() {
    return this.systemStartTimeStamp;
  }

  /**
   * Returns a numeric id of the archived system. It can be used in conjunction with the
   * {@link #getSystemStartTimeMillis} to uniquely identify an archived system.
   */
  public long getSystemId() {
    return this.systemId;
  }

  /**
   * Returns a string describing the operating system the archive was written on.
   */
  public String getOs() {
    return this.os;
  }

  /**
   * Returns a string describing the machine the archive was written on.
   */
  public String getMachine() {
    return this.machine;
  }

  /**
   * Returns the time zone used when the archive was created. This can be used to print timestamps
   * in the same time zone that was in effect when the archive was created.
   */
  public TimeZone getTimeZone() {
    TimeZone result = TimeZone.getTimeZone(this.timeZoneName);
    if (result.getRawOffset() != this.timeZoneOffset) {
      result = new SimpleTimeZone(this.timeZoneOffset, this.timeZoneName);
    }
    return result;
  }

  /**
   * Returns a string containing the version of the product that wrote this archive.
   */
  public String getProductVersion() {
    return this.productVersion;
  }

  /**
   * Returns a numeric code that represents the format version used to encode the archive as a
   * stream of bytes.
   */
  public int getArchiveFormatVersion() {
    return this.archiveVersion;
  }

  /**
   * Returns a string describing the system that this archive recorded.
   */
  public String getSystem() {
    return this.systemDirectory;
  }

  /**
   * Return the name of the file this archive was stored in or an empty string if the archive was
   * not stored in a file.
   */
  public String getArchiveFileName() {
    if (this.archive != null) {
      return this.archive.getFile().getPath();
    } else {
      return "";
    }
  }

  /**
   * Returns a string representation of this object.
   */
  @Override
  public String toString() {
    StringWriter sw = new StringWriter();
    this.dump(new PrintWriter(sw));
    return sw.toString();
  }

  protected void dump(PrintWriter stream) {
    if (archive != null) {
      stream.println("archive=" + archive.getFile());
    }
    stream.println("archiveVersion=" + archiveVersion);
    if (archive != null) {
      stream.println("startDate=" + archive.formatTimeMillis(startTimeStamp));
    }
    // stream.println("startTimeStamp=" + startTimeStamp +" tz=" + timeZoneName + " tzOffset=" +
    // timeZoneOffset);
    // stream.println("timeZone=" + getTimeZone().getDisplayName());
    stream.println("systemDirectory=" + systemDirectory);
    if (archive != null) {
      stream.println("systemStartDate=" + archive.formatTimeMillis(systemStartTimeStamp));
    }
    stream.println("systemId=" + systemId);
    stream.println("productVersion=" + productVersion);
    stream.println("osInfo=" + os);
    stream.println("machineInfo=" + machine);
  }

  @GeodeExtension
  public boolean isCompressed() {
    return archive.isCompressed();
  }
}
