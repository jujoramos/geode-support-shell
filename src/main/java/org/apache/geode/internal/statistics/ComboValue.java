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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.Assert;

import org.apache.geode.support.domain.marker.GeodeReplacement;

/**
 * A ComboValue is a value that is the logical combination of a set of other stat values.
 * <p>
 * For now ComboValue has a simple implementation that does not support updates.
 */
@GeodeReplacement(methods = { "Constructor", "getRawAbsoluteTimeStamps"})
class ComboValue extends AbstractValue {
  private final StatArchiveReader.ResourceType type;
  private final StatValue[] values;

  /**
   * Creates a ComboValue by adding all the specified values together.
   */
  ComboValue(List valueList) {
    this((StatValue[]) valueList.toArray(new StatValue[valueList.size()]));
  }

  /**
   * Creates a ComboValue by adding all the specified values together.
   */
  @GeodeReplacement(changes = "Replaced LocalizedStrings.")
  ComboValue(StatValue[] values) {
    this.values = values;
    this.filter = this.values[0].getFilter();
    String typeName = this.values[0].getType().getName();
    String statName = this.values[0].getDescriptor().getName();
    int bestTypeIdx = 0;
    for (int i = 1; i < this.values.length; i++) {
      if (this.filter != this.values[i].getFilter()) {
        /*
         * I'm not sure why this would happen. If it really can happen then this code should
         * change the filter since a client has no way to select values based on the filter.
         */
        throw new IllegalArgumentException(
//            LocalizedStrings.StatArchiveReader_CANT_COMBINE_VALUES_WITH_DIFFERENT_FILTERS
//                .toLocalizedString()
            "Cannot combine values with different filters."
        );
      }
      if (!typeName.equals(this.values[i].getType().getName())) {
        throw new IllegalArgumentException(
//            LocalizedStrings.StatArchiveReader_CANT_COMBINE_VALUES_WITH_DIFFERENT_TYPES
//                .toLocalizedString()
            "Cannot combine values with different types."
        );
      }
      if (!statName.equals(this.values[i].getDescriptor().getName())) {
        throw new IllegalArgumentException(
//            LocalizedStrings.StatArchiveReader_CANT_COMBINE_DIFFERENT_STATS.toLocalizedString()
            "Cannot combine different stats."
        );
      }
      if (this.values[i].getDescriptor().isCounter()) {
        // its a counter which is not the default
        if (!this.values[i].getDescriptor().isLargerBetter()) {
          // this guy has non-defaults for both use him
          bestTypeIdx = i;
        } else if (this.values[bestTypeIdx].getDescriptor()
            .isCounter() == this.values[bestTypeIdx].getDescriptor().isLargerBetter()) {
          // as long as we haven't already found a guy with defaults
          // make this guy the best type
          bestTypeIdx = i;
        }
      } else {
        // its a gauge, see if it has a non-default largerBetter
        if (this.values[i].getDescriptor().isLargerBetter()) {
          // as long as we haven't already found a guy with defaults
          if (this.values[bestTypeIdx].getDescriptor().isCounter() == this.values[bestTypeIdx]
              .getDescriptor().isLargerBetter()) {
            // make this guy the best type
            bestTypeIdx = i;
          }
        }
      }
    }
    this.type = this.values[bestTypeIdx].getType();
    this.descriptor = this.values[bestTypeIdx].getDescriptor();
  }

  private ComboValue(ComboValue original, long startTime, long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.type = original.getType();
    this.descriptor = original.getDescriptor();
    this.filter = original.getFilter();
    this.values = new StatValue[original.values.length];
    for (int i = 0; i < this.values.length; i++) {
      this.values[i] = original.values[i].createTrimmed(startTime, endTime);
    }
  }

  public StatValue createTrimmed(long startTime, long endTime) {
    if (startTime == this.startTime && endTime == this.endTime) {
      return this;
    } else {
      return new ComboValue(this, startTime, endTime);
    }
  }

  public StatArchiveReader.ResourceType getType() {
    return this.type;
  }

  public StatArchiveReader.ResourceInst[] getResources() {
    Set set = new HashSet();
    for (int i = 0; i < values.length; i++) {
      set.addAll(Arrays.asList(values[i].getResources()));
    }
    StatArchiveReader.ResourceInst[] result = new StatArchiveReader.ResourceInst[set.size()];
    return (StatArchiveReader.ResourceInst[]) set.toArray(result);
  }

  public boolean hasValueChanged() {
    return true;
  }

  public static boolean closeEnough(long v1, long v2, long delta) {
    return (v1 == v2) || ((Math.abs(v1 - v2) / 2) <= delta);
  }

  /**
   * Return true if v is closer to prev. Return false if v is closer to next. Return true if v is
   * the same distance from both.
   */
  public static boolean closer(long v, long prev, long next) {
    return Math.abs(v - prev) <= Math.abs(v - next);
  }


  /**
   * Return true if the current ts must be inserted instead of being mapped to the tsAtInsertPoint
   */
  private static boolean mustInsert(int nextIdx, long[] valueTimeStamps, long tsAtInsertPoint) {
    return (nextIdx < valueTimeStamps.length) && (valueTimeStamps[nextIdx] <= tsAtInsertPoint);
  }

  public long[] getRawAbsoluteTimeStampsWithSecondRes() {
    return getRawAbsoluteTimeStamps();
  }

  @GeodeReplacement(changes = "Replaced org.apache.geode.internal.Assert with org.springframework.util.Assert.")
  public long[] getRawAbsoluteTimeStamps() {
    if (values.length == 0) {
      return new long[0];
    }
    long[] valueTimeStamps = values[0].getRawAbsoluteTimeStamps();
    int tsCount = valueTimeStamps.length + 1;
    long[] ourTimeStamps = new long[(tsCount * 2) + 1];
    System.arraycopy(valueTimeStamps, 0, ourTimeStamps, 0, valueTimeStamps.length);
    // Note we add a MAX sample to make the insert logic simple
    ourTimeStamps[valueTimeStamps.length] = Long.MAX_VALUE;
    for (int i = 1; i < values.length; i++) {
      valueTimeStamps = values[i].getRawAbsoluteTimeStamps();
      if (valueTimeStamps.length == 0) {
        continue;
      }
      int ourIdx = 0;
      int j = 0;
      long tsToInsert = valueTimeStamps[0] - 1000; // default to 1 second
      if (valueTimeStamps.length > 1) {
        tsToInsert = valueTimeStamps[0] - (valueTimeStamps[1] - valueTimeStamps[0]);
      }
      // tsToInsert is now initialized to a value that we can pretend
      // was the previous timestamp inserted.
      while (j < valueTimeStamps.length) {
        long timeDelta = (valueTimeStamps[j] - tsToInsert) / 2;
        tsToInsert = valueTimeStamps[j];
        long tsAtInsertPoint = ourTimeStamps[ourIdx];
        while (tsToInsert > tsAtInsertPoint
            && !closeEnough(tsToInsert, tsAtInsertPoint, timeDelta)) {
          // System.out.println("DEBUG: skipping " + ourIdx + " because it was not closeEnough");
          ourIdx++;
          tsAtInsertPoint = ourTimeStamps[ourIdx];
        }
        if (closeEnough(tsToInsert, tsAtInsertPoint, timeDelta)
            && !mustInsert(j + 1, valueTimeStamps, tsAtInsertPoint)) {
          // It was already in our list so just go to the next one
          j++;
          ourIdx++; // never put the next timestamp at this index
          while (!closer(tsToInsert, ourTimeStamps[ourIdx - 1], ourTimeStamps[ourIdx])
              && !mustInsert(j, valueTimeStamps, ourTimeStamps[ourIdx])) {
            ourIdx++; // it is closer to the next one so skip forward on more
          }
        } else {
          // its not in our list so add it
          int endRunIdx = j + 1;
          while (endRunIdx < valueTimeStamps.length
              && valueTimeStamps[endRunIdx] < tsAtInsertPoint
              && !closeEnough(valueTimeStamps[endRunIdx], tsAtInsertPoint, timeDelta)) {
            endRunIdx++;
          }
          int numToCopy = endRunIdx - j;
          if (tsCount + numToCopy > ourTimeStamps.length) {
            // grow our timestamp array
            long[] tmp = new long[(tsCount + numToCopy) * 2];
            System.arraycopy(ourTimeStamps, 0, tmp, 0, tsCount);
            ourTimeStamps = tmp;
          }
          // make room for insert
          System.arraycopy(ourTimeStamps, ourIdx, ourTimeStamps, ourIdx + numToCopy,
              tsCount - ourIdx);
          // insert the elements
          if (numToCopy == 1) {
            ourTimeStamps[ourIdx] = valueTimeStamps[j];
          } else {
            System.arraycopy(valueTimeStamps, j, ourTimeStamps, ourIdx, numToCopy);
          }
          ourIdx += numToCopy;
          tsCount += numToCopy;
          // skip over all inserted elements
          j += numToCopy;
        }
      }
    }
    tsCount--;
    {
      int startIdx = 0;
      int endIdx = tsCount - 1;
      if (startTime != -1) {
//        Assert.assertTrue(ourTimeStamps[startIdx] >= startTime);
        Assert.isTrue(ourTimeStamps[startIdx] >= startTime);
      }
      if (endTime != -1) {
//        Assert.assertTrue(endIdx == startIdx - 1 || ourTimeStamps[endIdx] < endTime);
        Assert.isTrue(endIdx == startIdx - 1 || ourTimeStamps[endIdx] < endTime);
      }
      tsCount = (endIdx - startIdx) + 1;

      // shrink and trim our timestamp array
      long[] tmp = new long[tsCount];
      System.arraycopy(ourTimeStamps, startIdx, tmp, 0, tsCount);
      ourTimeStamps = tmp;
    }
    return ourTimeStamps;
  }

  public double[] getRawSnapshots() {
    return getRawSnapshots(getRawAbsoluteTimeStamps());
  }

  /**
   * Returns true if the timeStamp at curIdx is the one that ts is the closest to. We know that
   * timeStamps[curIdx-1], if it exists, was not the closest.
   */
  private static boolean isClosest(long ts, long[] timeStamps, int curIdx) {
    if (curIdx >= (timeStamps.length - 1)) {
      // curIdx is the last one so it must be the closest
      return true;
    }
    if (ts == timeStamps[curIdx]) {
      return true;
    }
    return closer(ts, timeStamps[curIdx], timeStamps[curIdx + 1]);
  }

  public boolean isTrimmedLeft() {
    for (int i = 0; i < this.values.length; i++) {
      if (this.values[i].isTrimmedLeft()) {
        return true;
      }
    }
    return false;
  }

  private double[] getRawSnapshots(long[] ourTimeStamps) {
    double[] result = new double[ourTimeStamps.length];
    if (result.length > 0) {
      for (int i = 0; i < values.length; i++) {
        long[] valueTimeStamps = values[i].getRawAbsoluteTimeStamps();
        double[] valueSnapshots = values[i].getRawSnapshots();
        double currentValue = 0.0;
        int curIdx = 0;
        if (values[i].isTrimmedLeft() && valueSnapshots.length > 0) {
          currentValue = valueSnapshots[0];
        }
        for (int j = 0; j < valueSnapshots.length; j++) {
          while (!isClosest(valueTimeStamps[j], ourTimeStamps, curIdx)) {
            if (descriptor.isCounter()) {
              result[curIdx] += currentValue;
            }

            curIdx++;
          }
          if (curIdx >= result.length) {
            // Add this to workaround bug 30288
            int samplesSkipped = valueSnapshots.length - j;
            StringBuffer msg = new StringBuffer(100);
            msg.append("WARNING: dropping last ");
            if (samplesSkipped == 1) {
              msg.append("sample because it");
            } else {
              msg.append(samplesSkipped).append(" samples because they");
            }
            msg.append(" could not fit in the merged result.");
            System.out.println(msg.toString());
            break;
          }
          currentValue = valueSnapshots[j];
          result[curIdx] += currentValue;
          curIdx++;
        }
        if (descriptor.isCounter()) {
          for (int j = curIdx; j < result.length; j++) {
            result[j] += currentValue;
          }
        }
      }
    }
    return result;
  }

  public double[] getSnapshots() {
    double[] result;
    if (filter != FILTER_NONE) {
      long[] timestamps = getRawAbsoluteTimeStamps();
      double[] snapshots = getRawSnapshots(timestamps);
      if (snapshots.length <= 1) {
        return new double[0];
      }
      result = new double[snapshots.length - 1];
      for (int i = 0; i < result.length; i++) {
        double valueDelta = snapshots[i + 1] - snapshots[i];
        if (filter == FILTER_PERSEC) {
          long timeDelta = timestamps[i + 1] - timestamps[i];
          result[i] = valueDelta / (timeDelta / 1000.0);
        } else {
          result[i] = valueDelta;
        }
      }
    } else {
      result = getRawSnapshots();
    }
    calcStats(result);
    return result;
  }
}
