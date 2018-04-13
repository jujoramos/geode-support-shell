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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

import org.springframework.util.Assert;

import org.apache.geode.support.domain.marker.GeodeExtension;
import org.apache.geode.support.domain.marker.GeodeImprovement;
import org.apache.geode.support.domain.marker.GeodeReplacement;
import org.apache.geode.internal.logging.DateFormatter;

@GeodeReplacement(
    methods = { "readCompactValue", "readHeaderToken", "readResourceInstanceCreateToken", "readResourceInstanceDeleteToken", "readResourceTypeToken", "readSampleToken", "readToken", "update" },
    changes = "Made public."
)
public class StatArchiveFile {
  private final StatArchiveReader reader;
  private InputStream is;
  private DataInputStream dataIn;
  private ValueFilter[] filters;
  private final File archiveName;
  private /* final */ int archiveVersion;
  private /* final */ ArchiveInfo info;
  private final boolean compressed;
  private boolean updateOK;
  private final boolean dump;
  private boolean closed = false;
  protected int resourceInstSize = 0;
  protected StatArchiveReader.ResourceInst[] resourceInstTable = null;
  private StatArchiveReader.ResourceType[] resourceTypeTable = null;
  private final TimeStampSeries
      timeSeries = new TimeStampSeries();
  private final DateFormat timeFormatter = new SimpleDateFormat(DateFormatter.FORMAT_STRING);
  private static final int BUFFER_SIZE = 1024 * 1024;
  private final ArrayList fileComboValues = new ArrayList();


  public StatArchiveFile(StatArchiveReader reader, File archiveName, boolean dump,
                         ValueFilter[] filters) throws IOException {
    this.reader = reader;
    this.archiveName = archiveName;
    this.dump = dump;
    this.compressed = archiveName.getPath().endsWith(".gz");
    this.is = new FileInputStream(this.archiveName);
    if (this.compressed) {
      this.dataIn = new DataInputStream(
          new BufferedInputStream(new GZIPInputStream(this.is, BUFFER_SIZE), BUFFER_SIZE));
    } else {
      this.dataIn = new DataInputStream(new BufferedInputStream(this.is, BUFFER_SIZE));
    }
    this.updateOK = this.dataIn.markSupported();
    this.filters = createFilters(filters);
  }

  private ValueFilter[] createFilters(ValueFilter[] allFilters) {
    if (allFilters == null) {
      return new ValueFilter[0];
    }
    ArrayList l = new ArrayList();
    for (int i = 0; i < allFilters.length; i++) {
      if (allFilters[i].archiveMatches(this.getFile())) {
        l.add(allFilters[i]);
      }
    }
    if (l.size() == allFilters.length) {
      return allFilters;
    } else {
      ValueFilter[] result = new ValueFilter[l.size()];
      return (ValueFilter[]) l.toArray(result);
    }
  }

  StatArchiveReader getReader() {
    return this.reader;
  }

  void matchSpec(StatSpec spec, List matchedValues) {
    if (spec.getCombineType() == StatSpec.FILE) {
      // search for previous ComboValue
      Iterator it = this.fileComboValues.iterator();
      while (it.hasNext()) {
        ComboValue v = (ComboValue) it.next();
        if (!spec.statMatches(v.getDescriptor().getName())) {
          continue;
        }
        if (!spec.typeMatches(v.getType().getName())) {
          continue;
        }
        StatArchiveReader.ResourceInst[] resources = v.getResources();
        for (int i = 0; i < resources.length; i++) {
          if (!spec.instanceMatches(resources[i].getName(), resources[i].getId())) {
            continue;
          }
          // note: we already know the archive file matches
        }
        matchedValues.add(v);
        return;
      }
      ArrayList l = new ArrayList();
      matchSpec(new RawStatSpec(spec), l);
      if (l.size() != 0) {
        ComboValue cv = new ComboValue(l);
        // save this in file's combo value list
        this.fileComboValues.add(cv);
        matchedValues.add(cv);
      }
    } else {
      for (int instIdx = 0; instIdx < resourceInstSize; instIdx++) {
        resourceInstTable[instIdx].matchSpec(spec, matchedValues);
      }
    }
  }

  /**
   * Formats an archive timestamp in way consistent with GemFire log dates. It will also be
   * formatted to reflect the time zone the archive was created in.
   *
   * @param ts The difference, measured in milliseconds, between the time marked by this time
   *        stamp and midnight, January 1, 1970 UTC.
   */
  public String formatTimeMillis(long ts) {
    synchronized (timeFormatter) {
      return timeFormatter.format(new Date(ts));
    }
  }

  /**
   * sets the time zone this archive was written in.
   */
  void setTimeZone(TimeZone z) {
    timeFormatter.setTimeZone(z);
  }

  /**
   * Returns the time series for this archive.
   */
  TimeStampSeries getTimeStamps() {
    return timeSeries;
  }

  /**
   * Checks to see if the archive has changed since the StatArchiverReader instance was created or
   * last updated. If the archive has additional samples then those are read the resource
   * instances maintained by the reader are updated.
   * <p>
   * Once closed a reader can no longer be updated.
   *
   * @return true if update read some new data.
   * @throws IOException if <code>archiveName</code> could not be opened read, or closed.
   */
  @GeodeReplacement(changes = "Replaced LocalizedStrings and GemFireExceptions.")
  public boolean update(boolean doReset) throws IOException {
    if (this.closed) {
      return false;
    }
    if (!this.updateOK) {
//      throw new InternalGemFireException(
//          LocalizedStrings.StatArchiveReader_UPDATE_OF_THIS_TYPE_OF_FILE_IS_NOT_SUPPORTED
//              .toLocalizedString());
      throw new RuntimeException("Update of this type of file is not supported.");
    }

    if (doReset) {
      this.dataIn.reset();
    }

    int updateTokenCount = 0;
    while (this.readToken()) {
      updateTokenCount++;
    }
    return updateTokenCount != 0;
  }

  public void dump(PrintWriter stream) {
    stream.print("archive=" + archiveName);
    if (info != null) {
      info.dump(stream);
    }
    for (int i = 0; i < resourceTypeTable.length; i++) {
      if (resourceTypeTable[i] != null) {
        resourceTypeTable[i].dump(stream);
      }
    }
    stream.print("time=");
    timeSeries.dump(stream);
    for (int i = 0; i < resourceInstTable.length; i++) {
      if (resourceInstTable[i] != null) {
        resourceInstTable[i].dump(stream);
      }
    }
  }

  public File getFile() {
    return this.archiveName;
  }

  /**
   * Closes the archive.
   */
  public void close() throws IOException {
    if (!this.closed) {
      this.closed = true;
      this.is.close();
      this.dataIn.close();
      this.is = null;
      this.dataIn = null;
      int typeCount = 0;
      if (this.resourceTypeTable != null) { // fix for bug 32320
        for (int i = 0; i < this.resourceTypeTable.length; i++) {
          if (this.resourceTypeTable[i] != null) {
            if (this.resourceTypeTable[i].close()) {
              this.resourceTypeTable[i] = null;
            } else {
              typeCount++;
            }
          }
        }
        StatArchiveReader.ResourceType[] newTypeTable = new StatArchiveReader.ResourceType[typeCount];
        typeCount = 0;
        for (int i = 0; i < this.resourceTypeTable.length; i++) {
          if (this.resourceTypeTable[i] != null) {
            newTypeTable[typeCount] = this.resourceTypeTable[i];
            typeCount++;
          }
        }
        this.resourceTypeTable = newTypeTable;
      }

      if (this.resourceInstTable != null) { // fix for bug 32320
        int instCount = 0;
        for (int i = 0; i < this.resourceInstTable.length; i++) {
          if (this.resourceInstTable[i] != null) {
            if (this.resourceInstTable[i].close()) {
              this.resourceInstTable[i] = null;
            } else {
              instCount++;
            }
          }
        }
        StatArchiveReader.ResourceInst[] newInstTable = new StatArchiveReader.ResourceInst[instCount];
        instCount = 0;
        for (int i = 0; i < this.resourceInstTable.length; i++) {
          if (this.resourceInstTable[i] != null) {
            newInstTable[instCount] = this.resourceInstTable[i];
            instCount++;
          }
        }
        this.resourceInstTable = newInstTable;
        this.resourceInstSize = instCount;
      }
      // optimize memory usage of timeSeries now that no more samples
      this.timeSeries.shrink();
      // filters are no longer needed since file will not be read from
      this.filters = null;
    }
  }

  /**
   * Returns global information about the read archive. Returns null if no information is
   * available.
   */
  public ArchiveInfo getArchiveInfo() {
    return this.info;
  }

  @GeodeReplacement(changes = "Replaced LocalizedStrings and GemFire Exception Types.")
  private void readHeaderToken() throws IOException {
    byte archiveVersion = dataIn.readByte();
    long startTimeStamp = dataIn.readLong();
    long systemId = dataIn.readLong();
    long systemStartTimeStamp = dataIn.readLong();
    int timeZoneOffset = dataIn.readInt();
    String timeZoneName = dataIn.readUTF();
    String systemDirectory = dataIn.readUTF();
    String productVersion = dataIn.readUTF();
    String os = dataIn.readUTF();
    String machine = dataIn.readUTF();
    if (archiveVersion <= 1) {
//      throw new GemFireIOException(
//          LocalizedStrings.StatArchiveReader_ARCHIVE_VERSION_0_IS_NO_LONGER_SUPPORTED
//              .toLocalizedString(Byte.valueOf(archiveVersion)),
//          null);
      throw new RuntimeException(String.format("Archive version: %s is no longer supported.", Byte.valueOf(archiveVersion)));
    }
    if (archiveVersion > StatArchiveFormat.ARCHIVE_VERSION) {
//      throw new GemFireIOException(
//          LocalizedStrings.StatArchiveReader_UNSUPPORTED_ARCHIVE_VERSION_0_THE_SUPPORTED_VERSION_IS_1
//              .toLocalizedString(
//                  new Object[] {Byte.valueOf(archiveVersion), Byte.valueOf(StatArchiveFormat.ARCHIVE_VERSION)}),
//          null);

      throw new RuntimeException(String.format("Unsupported archive version: %s.  The supported version is: %s.", Byte.valueOf(archiveVersion), Byte.valueOf(StatArchiveFormat.ARCHIVE_VERSION)));
    }
    this.archiveVersion = archiveVersion;
    this.info = new ArchiveInfo(this, archiveVersion, startTimeStamp, systemStartTimeStamp,
        timeZoneOffset, timeZoneName, systemDirectory, systemId, productVersion, os, machine);
    // Clear all previously read types and instances
    this.resourceInstSize = 0;
    this.resourceInstTable = new StatArchiveReader.ResourceInst[1024];
    this.resourceTypeTable = new StatArchiveReader.ResourceType[256];
    timeSeries.setBase(startTimeStamp);
    if (dump) {
      info.dump(new PrintWriter(System.out));
    }
  }

  boolean loadType(String typeName) {
    // note we don't have instance data or descriptor data yet
    if (filters == null || filters.length == 0) {
      return true;
    } else {
      for (int i = 0; i < filters.length; i++) {
        if (filters[i].typeMatches(typeName)) {
          return true;
        }
      }
      return false;
    }
  }

  boolean loadStatDescriptor(StatArchiveReader.StatDescriptor stat, StatArchiveReader.ResourceType type) {
    // note we don't have instance data yet
    if (!type.isLoaded()) {
      return false;
    }
    if (filters == null || filters.length == 0) {
      return true;
    } else {
      for (int i = 0; i < filters.length; i++) {
        if (filters[i].statMatches(stat.getName()) && filters[i].typeMatches(type.getName())) {
          return true;
        }
      }
      stat.unload();
      return false;
    }
  }

  boolean loadInstance(String textId, long numericId, StatArchiveReader.ResourceType type) {
    if (!type.isLoaded()) {
      return false;
    }
    if (filters == null || filters.length == 0) {
      return true;
    } else {
      for (int i = 0; i < filters.length; i++) {
        if (filters[i].typeMatches(type.getName())) {
          if (filters[i].instanceMatches(textId, numericId)) {
            StatArchiveReader.StatDescriptor[] stats = type.getStats();
            for (int j = 0; j < stats.length; j++) {
              if (stats[j].isLoaded()) {
                if (filters[i].statMatches(stats[j].getName())) {
                  return true;
                }
              }
            }
          }
        }
      }
      return false;
    }
  }

  boolean loadStat(StatArchiveReader.StatDescriptor stat, StatArchiveReader.ResourceInst resource) {
    StatArchiveReader.ResourceType type = resource.getType();
    if (!resource.isLoaded() || !type.isLoaded() || !stat.isLoaded()) {
      return false;
    }
    if (filters == null || filters.length == 0) {
      return true;
    } else {
      String textId = resource.getName();
      long numericId = resource.getId();
      for (int i = 0; i < filters.length; i++) {
        if (filters[i].statMatches(stat.getName()) && filters[i].typeMatches(type.getName())
            && filters[i].instanceMatches(textId, numericId)) {
          return true;
        }
      }
      return false;
    }
  }

  @GeodeReplacement(changes = "Replaced org.apache.geode.internal.Assert with org.springframework.util.Assert.")
  private void readResourceTypeToken() throws IOException {
    int resourceTypeId = dataIn.readInt();
    String resourceTypeName = dataIn.readUTF();
    String resourceTypeDesc = dataIn.readUTF();
    int statCount = dataIn.readUnsignedShort();
    while (resourceTypeId >= resourceTypeTable.length) {
      StatArchiveReader.ResourceType[] tmp = new StatArchiveReader.ResourceType[resourceTypeTable.length + 128];
      System.arraycopy(resourceTypeTable, 0, tmp, 0, resourceTypeTable.length);
      resourceTypeTable = tmp;
    }
    Assert.isTrue(resourceTypeTable[resourceTypeId] == null);

    StatArchiveReader.ResourceType rt;
    if (loadType(resourceTypeName)) {
      rt = new StatArchiveReader.ResourceType(resourceTypeId, resourceTypeName, resourceTypeDesc, statCount);
      if (dump) {
        System.out.println("ResourceType id=" + resourceTypeId + " name=" + resourceTypeName
            + " statCount=" + statCount + " desc=" + resourceTypeDesc);
      }
    } else {
      rt = new StatArchiveReader.ResourceType(resourceTypeId, resourceTypeName, statCount);
      if (dump) {
        System.out.println(
            "Not loading ResourceType id=" + resourceTypeId + " name=" + resourceTypeName);
      }
    }
    resourceTypeTable[resourceTypeId] = rt;
    for (int i = 0; i < statCount; i++) {
      String statName = dataIn.readUTF();
      byte typeCode = dataIn.readByte();
      boolean isCounter = dataIn.readBoolean();
      boolean largerBetter = isCounter; // default
      if (this.archiveVersion >= 4) {
        largerBetter = dataIn.readBoolean();
      }
      String units = dataIn.readUTF();
      String desc = dataIn.readUTF();
      rt.addStatDescriptor(this, i, statName, isCounter, largerBetter, typeCode, units, desc);
      if (dump) {
        System.out.println("  " + i + "=" + statName + " isCtr=" + isCounter + " largerBetter="
            + largerBetter + " typeCode=" + typeCode + " units=" + units + " desc=" + desc);
      }
    }
  }

  @GeodeReplacement(changes = { "Replaced org.apache.geode.internal.Assert with org.springframework.util.Assert" , "Replaced LocalizedStrings" })
  private void readResourceInstanceCreateToken(boolean initialize) throws IOException {
    int resourceInstId = dataIn.readInt();
    String name = dataIn.readUTF();
    long id = dataIn.readLong();
    int resourceTypeId = dataIn.readInt();
    while (resourceInstId >= resourceInstTable.length) {
      StatArchiveReader.ResourceInst[] tmp = new StatArchiveReader.ResourceInst[resourceInstTable.length + 128];
      System.arraycopy(resourceInstTable, 0, tmp, 0, resourceInstTable.length);
      resourceInstTable = tmp;
    }
    Assert.isTrue(resourceInstTable[resourceInstId] == null);
    if ((resourceInstId + 1) > this.resourceInstSize) {
      this.resourceInstSize = resourceInstId + 1;
    }
    StatArchiveReader.ResourceType type = resourceTypeTable[resourceTypeId];
    if (type == null) {
      throw new IllegalStateException("ResourceType is missing for resourceTypeId "
          + resourceTypeId + ", resourceName " + name);
    }
    boolean loadInstance = loadInstance(name, id, resourceTypeTable[resourceTypeId]);
    resourceInstTable[resourceInstId] = new StatArchiveReader.ResourceInst(this, resourceInstId, name, id,
        resourceTypeTable[resourceTypeId], loadInstance);
    if (dump) {
      System.out.println(
          (loadInstance ? "Loaded" : "Did not load") + " resource instance " + resourceInstId);
      System.out.println("  name=" + name + " id=" + id + " typeId=" + resourceTypeId);
    }
    if (initialize) {
      StatArchiveReader.StatDescriptor[] stats = resourceInstTable[resourceInstId].getType().getStats();
      for (int i = 0; i < stats.length; i++) {
        long v;
        switch (stats[i].getTypeCode()) {
          case StatArchiveFormat.BOOLEAN_CODE:
            v = dataIn.readByte();
            break;
          case StatArchiveFormat.BYTE_CODE:
          case StatArchiveFormat.CHAR_CODE:
            v = dataIn.readByte();
            break;
          case StatArchiveFormat.WCHAR_CODE:
            v = dataIn.readUnsignedShort();
            break;
          case StatArchiveFormat.SHORT_CODE:
            v = dataIn.readShort();
            break;
          case StatArchiveFormat.INT_CODE:
          case StatArchiveFormat.FLOAT_CODE:
          case StatArchiveFormat.LONG_CODE:
          case StatArchiveFormat.DOUBLE_CODE:
            v = readCompactValue();
            break;
          default:
//            throw new IOException(LocalizedStrings.StatArchiveReader_UNEXPECTED_TYPECODE_VALUE_0
//                .toLocalizedString(Byte.valueOf(stats[i].getTypeCode())));
            throw new IOException(String.format("Unexpected typeCode value %s", Byte.valueOf(stats[i].getTypeCode())));
        }
        resourceInstTable[resourceInstId].initialValue(i, v);
      }
    }
  }

  @GeodeReplacement(changes = "Replaced org.apache.geode.internal.Assert with org.springframework.util.Assert.")
  private void readResourceInstanceDeleteToken() throws IOException {
    int resourceInstId = dataIn.readInt();
    Assert.isTrue(resourceInstTable[resourceInstId] != null);
    resourceInstTable[resourceInstId].makeInactive();
    if (dump) {
      System.out.println("Delete resource instance " + resourceInstId);
    }
  }

  private int readResourceInstId() throws IOException {
    int token = dataIn.readUnsignedByte();
    if (token <= StatArchiveFormat.MAX_BYTE_RESOURCE_INST_ID) {
      return token;
    } else if (token == StatArchiveFormat.ILLEGAL_RESOURCE_INST_ID_TOKEN) {
      return StatArchiveFormat.ILLEGAL_RESOURCE_INST_ID;
    } else if (token == StatArchiveFormat.SHORT_RESOURCE_INST_ID_TOKEN) {
      return dataIn.readUnsignedShort();
    } else { /* token == INT_RESOURCE_INST_ID_TOKEN */
      return dataIn.readInt();
    }
  }

  private int readTimeDelta() throws IOException {
    int result = dataIn.readUnsignedShort();
    if (result == StatArchiveFormat.INT_TIMESTAMP_TOKEN) {
      result = dataIn.readInt();
    }
    return result;
  }

  @GeodeReplacement(changes = "Replaced with implementation from StatArchiveWriter.readCompactValue")
  private long readCompactValue() throws IOException {
//    return StatArchiveWriter.readCompactValue(this.dataIn);

    long v = dataIn.readByte();
    boolean dump = false;
    if (dump) {
      System.out.print("compactValue(byte1)=" + v);
    }
    if (v < StatArchiveFormat.MIN_1BYTE_COMPACT_VALUE) {
      if (v == StatArchiveFormat.COMPACT_VALUE_2_TOKEN) {
        v = dataIn.readShort();
        if (dump) {
          System.out.print("compactValue(short)=" + v);
        }
      } else {
        int bytesToRead = ((byte) v - StatArchiveFormat.COMPACT_VALUE_2_TOKEN) + 2;
        v = dataIn.readByte(); // note the first byte will be a signed byte.
        if (dump) {
          System.out.print("compactValue(" + bytesToRead + ")=" + v);
        }
        bytesToRead--;
        while (bytesToRead > 0) {
          v <<= 8;
          v |= dataIn.readUnsignedByte();
          bytesToRead--;
        }
      }
    }

    return v;
  }

  @GeodeReplacement(changes = "Replaced LocalizedStrings.")
  private void readSampleToken() throws IOException {
    int millisSinceLastSample = readTimeDelta();
    if (dump) {
      System.out.println("ts=" + millisSinceLastSample);
    }
    int resourceInstId = readResourceInstId();
    while (resourceInstId != StatArchiveFormat.ILLEGAL_RESOURCE_INST_ID) {
      if (dump) {
        System.out.print("  instId=" + resourceInstId);
      }
      StatArchiveReader.StatDescriptor[] stats = resourceInstTable[resourceInstId].getType().getStats();
      int statOffset = dataIn.readUnsignedByte();
      while (statOffset != StatArchiveFormat.ILLEGAL_STAT_OFFSET) {
        long statDeltaBits;
        switch (stats[statOffset].getTypeCode()) {
          case StatArchiveFormat.BOOLEAN_CODE:
            statDeltaBits = dataIn.readByte();
            break;
          case StatArchiveFormat.BYTE_CODE:
          case StatArchiveFormat.CHAR_CODE:
            statDeltaBits = dataIn.readByte();
            break;
          case StatArchiveFormat.WCHAR_CODE:
            statDeltaBits = dataIn.readUnsignedShort();
            break;
          case StatArchiveFormat.SHORT_CODE:
            statDeltaBits = dataIn.readShort();
            break;
          case StatArchiveFormat.INT_CODE:
          case StatArchiveFormat.FLOAT_CODE:
          case StatArchiveFormat.LONG_CODE:
          case StatArchiveFormat.DOUBLE_CODE:
            statDeltaBits = readCompactValue();
            break;
          default:
//            throw new IOException(LocalizedStrings.StatArchiveReader_UNEXPECTED_TYPECODE_VALUE_0
//                .toLocalizedString(Byte.valueOf(stats[statOffset].getTypeCode())));
            throw new IOException(String.format("Unexpected typeCode value %s", Byte.valueOf(stats[statOffset].getTypeCode())));
        }
        if (resourceInstTable[resourceInstId].addValueSample(statOffset, statDeltaBits)) {
          if (dump) {
            System.out.print(" [" + statOffset + "]=" + statDeltaBits);
          }
        }
        statOffset = dataIn.readUnsignedByte();
      }
      if (dump) {
        System.out.println();
      }
      resourceInstId = readResourceInstId();
    }
    timeSeries.addTimeStamp(millisSinceLastSample);
    for (int i = 0; i < resourceInstTable.length; i++) {
      StatArchiveReader.ResourceInst inst = resourceInstTable[i];
      if (inst != null && inst.isActive()) {
        inst.addTimeStamp();
      }
    }
  }

  /**
   * Returns true if token read, false if eof.
   */
  @GeodeImprovement(reason = "The readXXX methods need to sequentially read the file, even when using filters and not loading unwanted stats into memory, which slows the overall process.")
  @GeodeReplacement(changes = "Replaced LocalizedStrings.")
  private boolean readToken() throws IOException {
    byte token;
    try {
      if (this.updateOK) {
        this.dataIn.mark(BUFFER_SIZE);
      }
      token = this.dataIn.readByte();
      switch (token) {
        case StatArchiveFormat.HEADER_TOKEN:
          readHeaderToken();
          break;
        case StatArchiveFormat.RESOURCE_TYPE_TOKEN:
          readResourceTypeToken();
          break;
        case StatArchiveFormat.RESOURCE_INSTANCE_CREATE_TOKEN:
          readResourceInstanceCreateToken(false);
          break;
        case StatArchiveFormat.RESOURCE_INSTANCE_INITIALIZE_TOKEN:
          readResourceInstanceCreateToken(true);
          break;
        case StatArchiveFormat.RESOURCE_INSTANCE_DELETE_TOKEN:
          readResourceInstanceDeleteToken();
          break;
        case StatArchiveFormat.SAMPLE_TOKEN:
          readSampleToken();
          break;
        default:
//          throw new IOException(LocalizedStrings.StatArchiveReader_UNEXPECTED_TOKEN_BYTE_VALUE_0
//              .toLocalizedString(Byte.valueOf(token)));
          throw new IOException(String.format("Unexpected token byte value: %s", Byte.valueOf(token)));
      }
      return true;
    } catch (EOFException ignore) {
      return false;
    }
  }

  /**
   * Returns the approximate amount of memory used to implement this object.
   */
  protected int getMemoryUsed() {
    int result = 0;
    for (int i = 0; i < resourceInstTable.length; i++) {
      if (resourceInstTable[i] != null) {
        result += resourceInstTable[i].getMemoryUsed();
      }
    }
    return result;
  }

  @GeodeExtension
  public StatArchiveFile(File archive, ValueFilter[] filters) throws IOException {
    this.dump = false;
    this.reader = null;
    this.archiveName = archive;
    this.filters = createFilters(filters);
    this.is = new FileInputStream(this.archiveName);
    this.compressed = archiveName.getPath().endsWith(".gz");

    if (this.compressed) {
      this.dataIn = new DataInputStream(new BufferedInputStream(new GZIPInputStream(this.is, BUFFER_SIZE), BUFFER_SIZE));
    } else {
      this.dataIn = new DataInputStream(new BufferedInputStream(this.is, BUFFER_SIZE));
    }

    this.updateOK = this.dataIn.markSupported();
  }

  @GeodeExtension
  public boolean isCompressed() {
    return this.compressed;
  }

  @GeodeExtension
  public StatArchiveReader.ResourceInst[] getResourceInstancesTable() {
    return this.resourceInstTable;
  }

//  @GeodeExtension
//  public void readHeader() throws IOException {
//    byte token;
//
//    if (this.updateOK) {
//      this.dataIn.mark(BUFFER_SIZE);
//    }
//
//    do {
//      token = this.dataIn.readByte();
//    } while (token != StatArchiveFormat.HEADER_TOKEN);
//    readHeaderToken();
//  }
}
