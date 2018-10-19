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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import org.apache.geode.support.domain.marker.GeodeReplacement;

/**
 * StatArchiveReader provides APIs to read statistic snapshots from an archive file.
 */
@GeodeReplacement(changes = "Internal classes were made top level.")
public class StatArchiveReader implements StatArchiveFormat {

  protected static final NumberFormat nf = NumberFormat.getNumberInstance();
  static {
    nf.setMaximumFractionDigits(2);
    nf.setGroupingUsed(false);
  }

  private final StatArchiveFile[] archives;
  private boolean dump;
  private boolean closed = false;

  /**
   * Creates a StatArchiveReader that will read the named archive file.
   *
   * @param autoClose if its <code>true</code> then the reader will close input files as soon as it
   *        finds their end.
   * @throws IOException if <code>archiveName</code> could not be opened read, or closed.
   */
  public StatArchiveReader(File[] archiveNames, ValueFilter[] filters, boolean autoClose)
      throws IOException {
    this.archives = new StatArchiveFile[archiveNames.length];
    this.dump = Boolean.getBoolean("StatArchiveReader.dumpall");
    for (int i = 0; i < archiveNames.length; i++) {
      this.archives[i] = new StatArchiveFile(this, archiveNames[i], dump, filters);
    }

    update(false, autoClose);

    if (this.dump || Boolean.getBoolean("StatArchiveReader.dump")) {
      this.dump(new PrintWriter(System.out));
    }
  }

  /**
   * Creates a StatArchiveReader that will read the named archive file.
   *
   * @throws IOException if <code>archiveName</code> could not be opened read, or closed.
   */
  public StatArchiveReader(String archiveName) throws IOException {
    this(new File[] {new File(archiveName)}, null, false);
  }

  /**
   * Returns an array of stat values that match the specified spec. If nothing matches then an empty
   * array is returned.
   */
  public StatValue[] matchSpec(StatSpec spec) {
    if (spec.getCombineType() == StatSpec.GLOBAL) {
      StatValue[] allValues = matchSpec(new RawStatSpec(spec));
      if (allValues.length == 0) {
        return allValues;
      } else {
        ComboValue cv = new ComboValue(allValues);
        // need to save this in reader's combo value list
        return new StatValue[] {cv};
      }
    } else {
      List l = new ArrayList();
      StatArchiveFile[] archives = getArchives();
      for (int i = 0; i < archives.length; i++) {
        StatArchiveFile f = archives[i];
        if (spec.archiveMatches(f.getFile())) {
          f.matchSpec(spec, l);
        }
      }
      StatValue[] result = new StatValue[l.size()];
      return (StatValue[]) l.toArray(result);
    }
  }

  /**
   * Checks to see if any archives have changed since the StatArchiverReader instance was created or
   * last updated. If an archive has additional samples then those are read the resource instances
   * maintained by the reader are updated.
   * <p>
   * Once closed a reader can no longer be updated.
   *
   * @return true if update read some new data.
   * @throws IOException if an archive could not be opened read, or closed.
   */
  public boolean update() throws IOException {
    return update(true, false);
  }

  private boolean update(boolean doReset, boolean autoClose) throws IOException {
    if (this.closed) {
      return false;
    }
    boolean result = false;
    StatArchiveFile[] archives = getArchives();
    for (int i = 0; i < archives.length; i++) {
      StatArchiveFile f = archives[i];
      if (f.update(doReset)) {
        result = true;
      }
      if (autoClose) {
        f.close();
      }
    }
    return result;
  }

  /**
   * Returns an unmodifiable list of all the {@link ResourceInst} this reader contains.
   */
  public List getResourceInstList() {
    return new ResourceInstList();
  }

  public StatArchiveFile[] getArchives() {
    return this.archives;
  }

  /**
   * Closes all archives.
   */
  public void close() throws IOException {
    if (!this.closed) {
      StatArchiveFile[] archives = getArchives();
      for (int i = 0; i < archives.length; i++) {
        StatArchiveFile f = archives[i];
        f.close();
      }
      this.closed = true;
    }
  }

  private int getMemoryUsed() {
    int result = 0;
    StatArchiveFile[] archives = getArchives();
    for (int i = 0; i < archives.length; i++) {
      StatArchiveFile f = archives[i];
      result += f.getMemoryUsed();
    }
    return result;
  }

  private void dump(PrintWriter stream) {
    StatArchiveFile[] archives = getArchives();
    for (int i = 0; i < archives.length; i++) {
      StatArchiveFile f = archives[i];
      f.dump(stream);
    }
  }

  @GeodeReplacement(changes = "Replaced LocalizedStrings and GemFireExceptions.")
  protected static double bitsToDouble(int type, long bits) {
    switch (type) {
      case BOOLEAN_CODE:
      case BYTE_CODE:
      case CHAR_CODE:
      case WCHAR_CODE:
      case SHORT_CODE:
      case INT_CODE:
      case LONG_CODE:
        return bits;
      case FLOAT_CODE:
        return Float.intBitsToFloat((int) bits);
      case DOUBLE_CODE:
        return Double.longBitsToDouble(bits);
      default:
//        throw new InternalGemFireException(LocalizedStrings.StatArchiveReader_UNEXPECTED_TYPECODE_0
//            .toLocalizedString(Integer.valueOf(type)));

        throw new RuntimeException(String.format("Unexpected typecode %s", Integer.valueOf(type)));
    }
  }

  /**
   * Simple utility to read and dump statistic archive.
   */
  @GeodeReplacement(changes = "Replaced org.apache.geode.internal.ExitCode by System.exit(int)")
  public static void main(String args[]) throws IOException {
    String archiveName = null;
    if (args.length > 1) {
      System.err.println("Usage: [archiveName]");
//      ExitCode.FATAL.doSystemExit();
      System.exit(1);
    } else if (args.length == 1) {
      archiveName = args[0];
    } else {
      archiveName = "statArchive.gfs";
    }
    StatArchiveReader reader = new StatArchiveReader(archiveName);
    System.out.println("DEBUG: memory used = " + reader.getMemoryUsed());
    reader.close();
  }

  private class ResourceInstList extends AbstractList {
    protected ResourceInstList() {
      // nothing needed.
    }

    @Override
    public Object get(int idx) {
      int archiveIdx = 0;
      StatArchiveFile[] archives = getArchives();
      for (int i = 0; i < archives.length; i++) {
        StatArchiveFile f = archives[i];
        if (idx < (archiveIdx + f.resourceInstSize)) {
          return f.resourceInstTable[idx - archiveIdx];
        }
        archiveIdx += f.resourceInstSize;
      }
      return null;
    }

    @Override
    public int size() {
      int result = 0;
      StatArchiveFile[] archives = getArchives();
      for (int i = 0; i < archives.length; i++) {
        result += archives[i].resourceInstSize;
      }
      return result;
    }
  }

  /**
   * Describes a single statistic.
   */
  public static class StatDescriptor {
    private boolean loaded;
    private String name;
    private final int offset;
    private final boolean isCounter;
    private final boolean largerBetter;
    private final byte typeCode;
    private String units;
    private String desc;

    protected void dump(PrintWriter stream) {
      stream.println(
          "  " + name + ": type=" + typeCode + " offset=" + offset + (isCounter ? " counter" : "")
              + " units=" + units + " largerBetter=" + largerBetter + " desc=" + desc);
    }

    protected StatDescriptor(String name, int offset, boolean isCounter, boolean largerBetter,
                             byte typeCode, String units, String desc) {
      this.loaded = true;
      this.name = name;
      this.offset = offset;
      this.isCounter = isCounter;
      this.largerBetter = largerBetter;
      this.typeCode = typeCode;
      this.units = units;
      this.desc = desc;
    }

    public boolean isLoaded() {
      return this.loaded;
    }

    void unload() {
      this.loaded = false;
      this.name = null;
      this.units = null;
      this.desc = null;
    }

    /**
     * Returns the type code of this statistic. It will be one of the following values:
     * <ul>
     * <li>{@link #BOOLEAN_CODE}
     * <li>{@link #WCHAR_CODE}
     * <li>{@link #CHAR_CODE}
     * <li>{@link #BYTE_CODE}
     * <li>{@link #SHORT_CODE}
     * <li>{@link #INT_CODE}
     * <li>{@link #LONG_CODE}
     * <li>{@link #FLOAT_CODE}
     * <li>{@link #DOUBLE_CODE}
     * </ul>
     */
    public byte getTypeCode() {
      return this.typeCode;
    }

    /**
     * Returns the name of this statistic.
     */
    public String getName() {
      return this.name;
    }

    /**
     * Returns true if this statistic's value will always increase.
     */
    public boolean isCounter() {
      return this.isCounter;
    }

    /**
     * Returns true if larger values indicate better performance.
     */
    public boolean isLargerBetter() {
      return this.largerBetter;
    }

    /**
     * Returns a string that describes the units this statistic measures.
     */
    public String getUnits() {
      return this.units;
    }

    /**
     * Returns a textual description of this statistic.
     */
    public String getDescription() {
      return this.desc;
    }

    /**
     * Returns the offset of this stat in its type.
     */
    public int getOffset() {
      return this.offset;
    }
  }

  private abstract static class BitInterval {
    /** Returns number of items added to values */
    abstract int fill(double[] values, int valueOffset, int typeCode, int skipCount);

    abstract void dump(PrintWriter stream);

    abstract boolean attemptAdd(long addBits, long addInterval, int addCount);

    int getMemoryUsed() {
      return 0;
    }

    protected int count;

    public int getSampleCount() {
      return this.count;
    }

    static BitInterval create(long bits, long interval, int count) {
      if (interval == 0) {
        if (bits <= Integer.MAX_VALUE && bits >= Integer.MIN_VALUE) {
          return new BitZeroIntInterval((int) bits, count);
        } else {
          return new BitZeroLongInterval(bits, count);
        }
      } else if (count <= 3) {
        if (interval <= Byte.MAX_VALUE && interval >= Byte.MIN_VALUE) {
          return new BitExplicitByteInterval(bits, interval, count);
        } else if (interval <= Short.MAX_VALUE && interval >= Short.MIN_VALUE) {
          return new BitExplicitShortInterval(bits, interval, count);
        } else if (interval <= Integer.MAX_VALUE && interval >= Integer.MIN_VALUE) {
          return new BitExplicitIntInterval(bits, interval, count);
        } else {
          return new BitExplicitLongInterval(bits, interval, count);
        }
      } else {
        boolean smallBits = false;
        boolean smallInterval = false;
        if (bits <= Integer.MAX_VALUE && bits >= Integer.MIN_VALUE) {
          smallBits = true;
        }
        if (interval <= Integer.MAX_VALUE && interval >= Integer.MIN_VALUE) {
          smallInterval = true;
        }
        if (smallBits) {
          if (smallInterval) {
            return new BitNonZeroIntIntInterval((int) bits, (int) interval, count);
          } else {
            return new BitNonZeroIntLongInterval((int) bits, interval, count);
          }
        } else {
          if (smallInterval) {
            return new BitNonZeroLongIntInterval(bits, (int) interval, count);
          } else {
            return new BitNonZeroLongLongInterval(bits, interval, count);
          }
        }
      }
    }
  }

  private abstract static class BitNonZeroInterval extends BitInterval {
    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 4;
    }

    abstract long getBits();

    abstract long getInterval();

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      long base = getBits();
      long interval = getInterval();
      base += skipCount * interval;
      for (int i = 0; i < fillcount; i++) {
        values[valueOffset + i] = bitsToDouble(typeCode, base);
        base += interval;
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print(getBits());
      if (count > 1) {
        long interval = getInterval();
        if (interval != 0) {
          stream.print("+=" + interval);
        }
        stream.print("r" + count);
      }
    }

    BitNonZeroInterval(int count) {
      this.count = count;
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addInterval == getInterval()) {
        if (addBits == (getBits() + (addInterval * (count - 1)))) {
          count += addCount;
          return true;
        }
      }
      return false;
    }
  }

  private static class BitNonZeroIntIntInterval extends BitNonZeroInterval {
    int bits;
    int interval;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 8;
    }

    @Override
    long getBits() {
      return this.bits;
    }

    @Override
    long getInterval() {
      return this.interval;
    }

    BitNonZeroIntIntInterval(int bits, int interval, int count) {
      super(count);
      this.bits = bits;
      this.interval = interval;
    }
  }

  private static class BitNonZeroIntLongInterval extends BitNonZeroInterval {
    int bits;
    long interval;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 12;
    }

    @Override
    long getBits() {
      return this.bits;
    }

    @Override
    long getInterval() {
      return this.interval;
    }

    BitNonZeroIntLongInterval(int bits, long interval, int count) {
      super(count);
      this.bits = bits;
      this.interval = interval;
    }
  }

  private static class BitNonZeroLongIntInterval extends BitNonZeroInterval {
    long bits;
    int interval;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 12;
    }

    @Override
    long getBits() {
      return this.bits;
    }

    @Override
    long getInterval() {
      return this.interval;
    }

    BitNonZeroLongIntInterval(long bits, int interval, int count) {
      super(count);
      this.bits = bits;
      this.interval = interval;
    }
  }

  private static class BitNonZeroLongLongInterval extends BitNonZeroInterval {
    long bits;
    long interval;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 16;
    }

    @Override
    long getBits() {
      return this.bits;
    }

    @Override
    long getInterval() {
      return this.interval;
    }

    BitNonZeroLongLongInterval(long bits, long interval, int count) {
      super(count);
      this.bits = bits;
      this.interval = interval;
    }
  }

  private abstract static class BitZeroInterval extends BitInterval {
    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 4;
    }

    abstract long getBits();

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      double value = bitsToDouble(typeCode, getBits());
      for (int i = 0; i < fillcount; i++) {
        values[valueOffset + i] = value;
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print(getBits());
      if (count > 1) {
        stream.print("r" + count);
      }
    }

    BitZeroInterval(int count) {
      this.count = count;
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addInterval == 0 && addBits == getBits()) {
        count += addCount;
        return true;
      }
      return false;
    }
  }

  private static class BitZeroIntInterval extends BitZeroInterval {
    int bits;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 4;
    }

    @Override
    long getBits() {
      return bits;
    }

    BitZeroIntInterval(int bits, int count) {
      super(count);
      this.bits = bits;
    }
  }

  private static class BitZeroLongInterval extends BitZeroInterval {
    long bits;

    @Override
    int getMemoryUsed() {
      return super.getMemoryUsed() + 8;
    }

    @Override
    long getBits() {
      return bits;
    }

    BitZeroLongInterval(long bits, int count) {
      super(count);
      this.bits = bits;
    }
  }

  private static class BitExplicitByteInterval extends BitInterval {
    long firstValue;
    long lastValue;
    byte[] bitIntervals = null;

    @Override
    int getMemoryUsed() {
      int result = super.getMemoryUsed() + 4 + 8 + 8 + 4;
      if (bitIntervals != null) {
        result += bitIntervals.length;
      }
      return result;
    }

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      long bitValue = firstValue;
      for (int i = 0; i < skipCount; i++) {
        bitValue += bitIntervals[i];
      }
      for (int i = 0; i < fillcount; i++) {
        bitValue += bitIntervals[skipCount + i];
        values[valueOffset + i] = bitsToDouble(typeCode, bitValue);
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print("(byteIntervalCount=" + count + " start=" + firstValue);
      for (int i = 0; i < count; i++) {
        if (i != 0) {
          stream.print(", ");
        }
        stream.print(bitIntervals[i]);
      }
      stream.print(")");
    }

    BitExplicitByteInterval(long bits, long interval, int addCount) {
      count = addCount;
      firstValue = bits;
      lastValue = bits + (interval * (addCount - 1));
      bitIntervals = new byte[count * 2];
      bitIntervals[0] = 0;
      for (int i = 1; i < count; i++) {
        bitIntervals[i] = (byte) interval;
      }
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addCount <= 11) {
        if (addInterval <= Byte.MAX_VALUE && addInterval >= Byte.MIN_VALUE) {
          long firstInterval = addBits - lastValue;
          if (firstInterval <= Byte.MAX_VALUE && firstInterval >= Byte.MIN_VALUE) {
            lastValue = addBits + (addInterval * (addCount - 1));
            if ((count + addCount) >= bitIntervals.length) {
              byte[] tmp = new byte[(count + addCount) * 2];
              System.arraycopy(bitIntervals, 0, tmp, 0, bitIntervals.length);
              bitIntervals = tmp;
            }
            bitIntervals[count++] = (byte) firstInterval;
            for (int i = 1; i < addCount; i++) {
              bitIntervals[count++] = (byte) addInterval;
            }
            return true;
          }
        }
      }
      return false;
    }
  }

  private static class BitExplicitShortInterval extends BitInterval {
    long firstValue;
    long lastValue;
    short[] bitIntervals = null;

    @Override
    int getMemoryUsed() {
      int result = super.getMemoryUsed() + 4 + 8 + 8 + 4;
      if (bitIntervals != null) {
        result += bitIntervals.length * 2;
      }
      return result;
    }

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      long bitValue = firstValue;
      for (int i = 0; i < skipCount; i++) {
        bitValue += bitIntervals[i];
      }
      for (int i = 0; i < fillcount; i++) {
        bitValue += bitIntervals[skipCount + i];
        values[valueOffset + i] = bitsToDouble(typeCode, bitValue);
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print("(shortIntervalCount=" + count + " start=" + firstValue);
      for (int i = 0; i < count; i++) {
        if (i != 0) {
          stream.print(", ");
        }
        stream.print(bitIntervals[i]);
      }
      stream.print(")");
    }

    BitExplicitShortInterval(long bits, long interval, int addCount) {
      count = addCount;
      firstValue = bits;
      lastValue = bits + (interval * (addCount - 1));
      bitIntervals = new short[count * 2];
      bitIntervals[0] = 0;
      for (int i = 1; i < count; i++) {
        bitIntervals[i] = (short) interval;
      }
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addCount <= 6) {
        if (addInterval <= Short.MAX_VALUE && addInterval >= Short.MIN_VALUE) {
          long firstInterval = addBits - lastValue;
          if (firstInterval <= Short.MAX_VALUE && firstInterval >= Short.MIN_VALUE) {
            lastValue = addBits + (addInterval * (addCount - 1));
            if ((count + addCount) >= bitIntervals.length) {
              short[] tmp = new short[(count + addCount) * 2];
              System.arraycopy(bitIntervals, 0, tmp, 0, bitIntervals.length);
              bitIntervals = tmp;
            }
            bitIntervals[count++] = (short) firstInterval;
            for (int i = 1; i < addCount; i++) {
              bitIntervals[count++] = (short) addInterval;
            }
            return true;
          }
        }
      }
      return false;
    }
  }

  private static class BitExplicitIntInterval extends BitInterval {
    long firstValue;
    long lastValue;
    int[] bitIntervals = null;

    @Override
    int getMemoryUsed() {
      int result = super.getMemoryUsed() + 4 + 8 + 8 + 4;
      if (bitIntervals != null) {
        result += bitIntervals.length * 4;
      }
      return result;
    }

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      long bitValue = firstValue;
      for (int i = 0; i < skipCount; i++) {
        bitValue += bitIntervals[i];
      }
      for (int i = 0; i < fillcount; i++) {
        bitValue += bitIntervals[skipCount + i];
        values[valueOffset + i] = bitsToDouble(typeCode, bitValue);
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print("(intIntervalCount=" + count + " start=" + firstValue);
      for (int i = 0; i < count; i++) {
        if (i != 0) {
          stream.print(", ");
        }
        stream.print(bitIntervals[i]);
      }
      stream.print(")");
    }

    BitExplicitIntInterval(long bits, long interval, int addCount) {
      count = addCount;
      firstValue = bits;
      lastValue = bits + (interval * (addCount - 1));
      bitIntervals = new int[count * 2];
      bitIntervals[0] = 0;
      for (int i = 1; i < count; i++) {
        bitIntervals[i] = (int) interval;
      }
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addCount <= 4) {
        if (addInterval <= Integer.MAX_VALUE && addInterval >= Integer.MIN_VALUE) {
          long firstInterval = addBits - lastValue;
          if (firstInterval <= Integer.MAX_VALUE && firstInterval >= Integer.MIN_VALUE) {
            lastValue = addBits + (addInterval * (addCount - 1));
            if ((count + addCount) >= bitIntervals.length) {
              int[] tmp = new int[(count + addCount) * 2];
              System.arraycopy(bitIntervals, 0, tmp, 0, bitIntervals.length);
              bitIntervals = tmp;
            }
            bitIntervals[count++] = (int) firstInterval;
            for (int i = 1; i < addCount; i++) {
              bitIntervals[count++] = (int) addInterval;
            }
            return true;
          }
        }
      }
      return false;
    }
  }

  private static class BitExplicitLongInterval extends BitInterval {
    long[] bitArray = null;

    @Override
    int getMemoryUsed() {
      int result = super.getMemoryUsed() + 4 + 4;
      if (bitArray != null) {
        result += bitArray.length * 8;
      }
      return result;
    }

    @Override
    int fill(double[] values, int valueOffset, int typeCode, int skipCount) {
      int fillcount = values.length - valueOffset; // space left in values
      int maxCount = count - skipCount; // maximum values this interval can produce
      if (fillcount > maxCount) {
        fillcount = maxCount;
      }
      for (int i = 0; i < fillcount; i++) {
        values[valueOffset + i] = bitsToDouble(typeCode, bitArray[skipCount + i]);
      }
      return fillcount;
    }

    @Override
    void dump(PrintWriter stream) {
      stream.print("(count=" + count + " ");
      for (int i = 0; i < count; i++) {
        if (i != 0) {
          stream.print(", ");
        }
        stream.print(bitArray[i]);
      }
      stream.print(")");
    }

    BitExplicitLongInterval(long bits, long interval, int addCount) {
      count = addCount;
      bitArray = new long[count * 2];
      for (int i = 0; i < count; i++) {
        bitArray[i] = bits;
        bits += interval;
      }
    }

    @Override
    boolean attemptAdd(long addBits, long addInterval, int addCount) {
      // addCount >= 2; count >= 2
      if (addCount <= 3) {
        if ((count + addCount) >= bitArray.length) {
          long[] tmp = new long[(count + addCount) * 2];
          System.arraycopy(bitArray, 0, tmp, 0, bitArray.length);
          bitArray = tmp;
        }
        for (int i = 0; i < addCount; i++) {
          bitArray[count++] = addBits;
          addBits += addInterval;
        }
        return true;
      }
      return false;
    }
  }

  @GeodeReplacement(changes = "Made protected.")
  protected static class BitSeries {
    int count; // number of items in this series
    long currentStartBits;
    long currentEndBits;
    long currentInterval;
    int currentCount;
    int intervalIdx; // index of most recent BitInterval
    BitInterval intervals[];

    /**
     * Returns the amount of memory used to implement this series.
     */
    protected int getMemoryUsed() {
      int result = 4 + 8 + 8 + 8 + 4 + 4 + 4;
      if (intervals != null) {
        result += 4 * intervals.length;
        for (int i = 0; i <= intervalIdx; i++) {
          result += intervals[i].getMemoryUsed();
        }
      }
      return result;
    }

    public double[] getValues(int typeCode) {
      return getValuesEx(typeCode, 0, getSize());
    }

    /**
     * Gets the first "resultSize" values of this series skipping over the first "samplesToSkip"
     * ones. The first value in a series is at index 0. The maximum result size can be obtained by
     * calling "getSize()".
     */
    @GeodeReplacement(changes = "Replaced LocalizedStrings and GemFire Exception Types.")
    public double[] getValuesEx(int typeCode, int samplesToSkip, int resultSize) {
      double[] result = new double[resultSize];
      int firstInterval = 0;
      int idx = 0;
      while (samplesToSkip > 0 && firstInterval <= intervalIdx
          && intervals[firstInterval].getSampleCount() <= samplesToSkip) {
        samplesToSkip -= intervals[firstInterval].getSampleCount();
        firstInterval++;
      }
      for (int i = firstInterval; i <= intervalIdx; i++) {
        idx += intervals[i].fill(result, idx, typeCode, samplesToSkip);
        samplesToSkip = 0;
      }
      if (currentCount != 0) {
        idx += BitInterval.create(currentStartBits, currentInterval, currentCount).fill(result, idx,
            typeCode, samplesToSkip);
      }
      // assert
      if (idx != resultSize) {
//        throw new InternalGemFireException(
//            LocalizedStrings.StatArchiveReader_GETVALUESEX_DIDNT_FILL_THE_LAST_0_ENTRIES_OF_ITS_RESULT
//                .toLocalizedString(Integer.valueOf(resultSize - idx)));
        throw new RuntimeException(String.format("getValuesEx did not fill the last %s entries of its result.", Integer.valueOf(resultSize - idx)));
      }
      return result;
    }

    void dump(PrintWriter stream) {
      stream.print("[size=" + count + " intervals=" + (intervalIdx + 1) + " memused="
          + getMemoryUsed() + " ");
      for (int i = 0; i <= intervalIdx; i++) {
        if (i != 0) {
          stream.print(", ");
        }
        intervals[i].dump(stream);
      }
      if (currentCount != 0) {
        if (intervalIdx != -1) {
          stream.print(", ");
        }
        BitInterval.create(currentStartBits, currentInterval, currentCount).dump(stream);
      }
      stream.println("]");
    }

    BitSeries() {
      count = 0;
      currentStartBits = 0;
      currentEndBits = 0;
      currentInterval = 0;
      currentCount = 0;
      intervalIdx = -1;
      intervals = null;
    }

    void initialBits(long bits) {
      this.currentEndBits = bits;
    }

    int getSize() {
      return this.count;
    }

    void addBits(long deltaBits) {
      long bits = currentEndBits + deltaBits;
      if (currentCount == 0) {
        currentStartBits = bits;
        currentCount = 1;
      } else if (currentCount == 1) {
        currentInterval = deltaBits;
        currentCount++;
      } else if (deltaBits == currentInterval) {
        currentCount++;
      } else {
        // we need to move currentBits into a BitInterval
        if (intervalIdx == -1) {
          intervals = new BitInterval[2];
          intervalIdx = 0;
          intervals[0] = BitInterval.create(currentStartBits, currentInterval, currentCount);
        } else {
          if (!intervals[intervalIdx].attemptAdd(currentStartBits, currentInterval, currentCount)) {
            // wouldn't fit in current bit interval so add a new one
            intervalIdx++;
            if (intervalIdx >= intervals.length) {
              BitInterval[] tmp = new BitInterval[intervals.length * 2];
              System.arraycopy(intervals, 0, tmp, 0, intervals.length);
              intervals = tmp;
            }
            intervals[intervalIdx] =
                BitInterval.create(currentStartBits, currentInterval, currentCount);
          }
        }
        // now start a new currentBits
        currentStartBits = bits;
        currentCount = 1;
      }
      currentEndBits = bits;
      count++;
    }

    /**
     * Free up any unused memory
     */
    void shrink() {
      if (intervals != null) {
        int currentSize = intervalIdx + 1;
        if (currentSize < intervals.length) {
          BitInterval[] tmp = new BitInterval[currentSize];
          System.arraycopy(intervals, 0, tmp, 0, currentSize);
          intervals = tmp;
        }
      }
    }
  }

  /**
   * Defines a statistic resource type. Each resource instance must be of a single type. The type
   * defines what statistics each instance of it will support. The type also has a description of
   * itself.
   */
  public static class ResourceType {
    private boolean loaded;
    private final String name;
    private String desc;
    private final StatDescriptor[] stats;
    private Map descriptorMap;

    public void dump(PrintWriter stream) {
      if (loaded) {
        stream.println(name + ": " + desc);
        for (int i = 0; i < stats.length; i++) {
          stats[i].dump(stream);
        }
      }
    }

    protected ResourceType(int id, String name, int statCount) {
      this.loaded = false;
      this.name = name;
      this.desc = null;
      this.stats = new StatDescriptor[statCount];
      this.descriptorMap = null;
    }

    protected ResourceType(int id, String name, String desc, int statCount) {
      this.loaded = true;
      this.name = name;
      this.desc = desc;
      this.stats = new StatDescriptor[statCount];
      this.descriptorMap = new HashMap();
    }

    public boolean isLoaded() {
      return this.loaded;
    }

    /**
     * Frees up any resources no longer needed after the archive file is closed. Returns true if
     * this guy is no longer needed.
     */
    protected boolean close() {
      if (isLoaded()) {
        for (int i = 0; i < stats.length; i++) {
          if (stats[i] != null) {
            if (!stats[i].isLoaded()) {
              stats[i] = null;
            }
          }
        }
        return false;
      } else {
        return true;
      }
    }

    void unload() {
      this.loaded = false;
      this.desc = null;
      for (int i = 0; i < this.stats.length; i++) {
        this.stats[i].unload();
      }
      this.descriptorMap.clear();
      this.descriptorMap = null;
    }

    protected void addStatDescriptor(StatArchiveFile archive, int offset, String name,
                                     boolean isCounter, boolean largerBetter, byte typeCode, String units, String desc) {
      StatDescriptor descriptor =
          new StatDescriptor(name, offset, isCounter, largerBetter, typeCode, units, desc);
      this.stats[offset] = descriptor;
      if (archive.loadStatDescriptor(descriptor, this)) {
        descriptorMap.put(name, descriptor);
      }
    }

    /**
     * Returns the name of this resource type.
     */
    public String getName() {
      return this.name;
    }

    /**
     * Returns an array of descriptors for each statistic this resource type supports.
     */
    public StatDescriptor[] getStats() {
      return this.stats;
    }

    /**
     * Gets a stat descriptor contained in this type given the stats name.
     *
     * @param name the name of the stat to find in the current type
     * @return the descriptor that matches the name or null if the type does not have a stat of the
     *         given name
     */
    public StatDescriptor getStat(String name) {
      return (StatDescriptor) descriptorMap.get(name);
    }

    /**
     * Returns a description of this resource type.
     */
    public String getDescription() {
      return this.desc;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ResourceType other = (ResourceType) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }
  }

  /**
   * Defines a single instance of a resource type.
   */
  public static class ResourceInst {
    private final boolean loaded;
    private final StatArchiveFile archive;
    private final ResourceType type;
    private final String name;
    private final long id;
    private boolean active = true;
    private final SimpleValue[] values;
    private int firstTSidx = -1;
    private int lastTSidx = -1;

    /**
     * Returns the approximate amount of memory used to implement this object.
     */
    protected int getMemoryUsed() {
      int result = 0;
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          result += this.values[i].getMemoryUsed();
        }
      }
      return result;
    }

    public StatArchiveReader getReader() {
      return archive.getReader();
    }

    /**
     * Returns a string representation of this object.
     */
    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append(name).append(", ").append(id).append(", ").append(type.getName()).append(": \"")
          .append(archive.formatTimeMillis(getFirstTimeMillis())).append('\"');
      if (!active) {
        result.append(" inactive");
      }
      result.append(" samples=" + getSampleCount());
      return result.toString();
    }

    /**
     * Returns the number of times this resource instance has been sampled.
     */
    public int getSampleCount() {
      if (active) {
        return archive.getTimeStamps().getSize() - firstTSidx;
      } else {
        return (lastTSidx + 1) - firstTSidx;
      }
    }

    public StatArchiveFile getArchive() {
      return this.archive;
    }

    protected void dump(PrintWriter stream) {
      stream.println(
          name + ":" + " file=" + getArchive().getFile() + " id=" + id + (active ? "" : " deleted")
              + " start=" + archive.formatTimeMillis(getFirstTimeMillis()));
      for (int i = 0; i < values.length; i++) {
        values[i].dump(stream);
      }
    }

    @GeodeReplacement(changes = "Replaced org.apache.geode.internal.Assert with org.springframework.util.Assert.")
    protected ResourceInst(StatArchiveFile archive, int uniqueId, String name, long id,
                           ResourceType type, boolean loaded) {
      this.loaded = loaded;
      this.archive = archive;
      this.name = name;
      this.id = id;
      Assert.isTrue(type != null);
      this.type = type;
      if (loaded) {
        StatDescriptor[] stats = type.getStats();
        this.values = new SimpleValue[stats.length];
        for (int i = 0; i < stats.length; i++) {
          if (archive.loadStat(stats[i], this)) {
            this.values[i] = new SimpleValue(this, stats[i]);
          } else {
            this.values[i] = null;
          }
        }
      } else {
        this.values = null;
      }
    }

    void matchSpec(StatSpec spec, List matchedValues) {
      if (spec.typeMatches(this.type.getName())) {
        if (spec.instanceMatches(this.getName(), this.getId())) {
          for (int statIdx = 0; statIdx < values.length; statIdx++) {
            if (values[statIdx] != null) {
              if (spec.statMatches(values[statIdx].getDescriptor().getName())) {
                matchedValues.add(values[statIdx]);
              }
            }
          }
        }
      }
    }

    protected void initialValue(int statOffset, long v) {
      if (this.values != null && this.values[statOffset] != null) {
        this.values[statOffset].initialValue(v);
      }
    }

    /**
     * Returns true if sample was added.
     */
    protected boolean addValueSample(int statOffset, long statDeltaBits) {
      if (this.values != null && this.values[statOffset] != null) {
        this.values[statOffset].prepareNextBits(statDeltaBits);
        return true;
      } else {
        return false;
      }
    }

    public boolean isLoaded() {
      return this.loaded;
    }

    /**
     * Frees up any resources no longer needed after the archive file is closed. Returns true if
     * this guy is no longer needed.
     */
    protected boolean close() {
      if (isLoaded()) {
        for (int i = 0; i < values.length; i++) {
          if (values[i] != null) {
            values[i].shrink();
          }
        }
        return false;
      } else {
        return true;
      }
    }

    protected int getFirstTimeStampIdx() {
      return this.firstTSidx;
    }

    protected long[] getAllRawTimeStamps() {
      return archive.getTimeStamps().getRawTimeStamps();
    }

    protected long getTimeBase() {
      return archive.getTimeStamps().getBase();
    }

    /**
     * Returns an array of doubles containing the timestamps at which this instances samples where
     * taken. Each of these timestamps is the difference, measured in milliseconds, between the
     * sample time and midnight, January 1, 1970 UTC. Although these values are double they can
     * safely be converted to <code>long</code> with no loss of information.
     */
    public double[] getSnapshotTimesMillis() {
      return archive.getTimeStamps().getTimeValuesSinceIdx(firstTSidx);
    }

    /**
     * Returns an array of statistic value descriptors. Each element of the array describes the
     * corresponding statistic this instance supports. The <code>StatValue</code> instances can be
     * used to obtain the actual sampled values of the instances statistics.
     */
    public StatValue[] getStatValues() {
      return this.values;
    }

    /**
     * Gets the value of the stat in the current instance given the stat name.
     *
     * @param name the name of the stat to find in the current instance
     * @return the value that matches the name or null if the instance does not have a stat of the
     *         given name
     *
     */
    public StatValue getStatValue(String name) {
      StatValue result = null;
      StatDescriptor desc = getType().getStat(name);
      if (desc != null) {
        result = values[desc.getOffset()];
      }
      return result;
    }

    /**
     * Returns the name of this instance.
     */
    public String getName() {
      return this.name;
    }

    /**
     * Returns the id of this instance.
     */
    public long getId() {
      return this.id;
    }

    /**
     * Returns the difference, measured in milliseconds, between the time of the instance's first
     * sample and midnight, January 1, 1970 UTC.
     */
    public long getFirstTimeMillis() {
      return archive.getTimeStamps().getMilliTimeStamp(firstTSidx);
    }

    /**
     * Returns resource type of this instance.
     */
    public ResourceType getType() {
      return this.type;
    }

    protected void makeInactive() {
      this.active = false;
      lastTSidx = archive.getTimeStamps().getSize() - 1;
      close(); // this frees up unused memory now that no more samples
    }

    /**
     * Returns true if archive might still have future samples for this instance.
     */
    public boolean isActive() {
      return this.active;
    }

    protected void addTimeStamp() {
      if (this.loaded) {
        if (firstTSidx == -1) {
          firstTSidx = archive.getTimeStamps().getSize() - 1;
        }
        for (int i = 0; i < values.length; i++) {
          if (values[i] != null) {
            values[i].addSample();
          }
        }
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (id ^ (id >>> 32));
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ResourceInst other = (ResourceInst) obj;
      if (id != other.id)
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      if (type == null) {
        if (other.type != null)
          return false;
      } else if (!type.equals(other.type))
        return false;
      if (this.firstTSidx != other.firstTSidx) {
        return false;
      }
      return true;
    }
  }
}
