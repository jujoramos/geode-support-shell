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

import java.util.Date;

import org.apache.geode.support.domain.marker.GeodeReplacement;

@GeodeReplacement(methods = "setFilter", changes = "Made public.")
abstract class AbstractValue implements StatValue {
  protected StatArchiveReader.StatDescriptor descriptor;
  protected int filter;

  protected long startTime = -1;
  protected long endTime = -1;

  protected boolean statsValid = false;
  protected int size;
  protected double min;
  protected double max;
  protected double avg;
  protected double stddev;
  protected double mostRecent;

  public void calcStats() {
    if (!statsValid) {
      getSnapshots();
    }
  }

  public int getSnapshotsSize() {
    calcStats();
    return this.size;
  }

  public double getSnapshotsMinimum() {
    calcStats();
    return this.min;
  }

  public double getSnapshotsMaximum() {
    calcStats();
    return this.max;
  }

  public double getSnapshotsAverage() {
    calcStats();
    return this.avg;
  }

  public double getSnapshotsStandardDeviation() {
    calcStats();
    return this.stddev;
  }

  public double getSnapshotsMostRecent() {
    calcStats();
    return this.mostRecent;
  }

  public StatArchiveReader.StatDescriptor getDescriptor() {
    return this.descriptor;
  }

  public int getFilter() {
    return this.filter;
  }

  @GeodeReplacement(changes = "Replaced LocalizedStrings.")
  public void setFilter(int filter) {
    if (filter != this.filter) {
      if (filter != FILTER_NONE && filter != FILTER_PERSEC && filter != FILTER_PERSAMPLE) {
//          throw new IllegalArgumentException(
//              LocalizedStrings.StatArchiveReader_FILTER_VALUE_0_MUST_BE_1_2_OR_3.toLocalizedString(
//                  new Object[] {Integer.valueOf(filter), Integer.valueOf(FILTER_NONE),
//                      Integer.valueOf(FILTER_PERSEC), Integer.valueOf(FILTER_PERSAMPLE)}));

        throw new IllegalArgumentException(String.format("Filter value \"%s\" must be %s, %s, or %s.", Integer.valueOf(filter), Integer.valueOf(FILTER_NONE), Integer.valueOf(FILTER_PERSEC), Integer.valueOf(FILTER_PERSAMPLE)));
      }
      this.filter = filter;
      this.statsValid = false;
    }
  }

  /**
   * Calculates each stat given the result of calling getSnapshots
   */
  protected void calcStats(double[] values) {
    if (statsValid) {
      return;
    }
    size = values.length;
    if (size == 0) {
      min = 0.0;
      max = 0.0;
      avg = 0.0;
      stddev = 0.0;
      mostRecent = 0.0;
    } else {
      min = values[0];
      max = values[0];
      mostRecent = values[values.length - 1];
      double total = values[0];
      for (int i = 1; i < size; i++) {
        total += values[i];
        if (values[i] < min) {
          min = values[i];
        } else if (values[i] > max) {
          max = values[i];
        }
      }
      avg = total / size;
      stddev = 0.0;
      if (size > 1) {
        for (int i = 0; i < size; i++) {
          double dv = values[i] - avg;
          stddev += (dv * dv);
        }
        stddev /= (size - 1);
        stddev = Math.sqrt(stddev);
      }
    }
    statsValid = true;
  }

  /**
   * Returns a string representation of this object.
   */
  @Override
  public String toString() {
    calcStats();
    StringBuffer result = new StringBuffer();
    result.append(getDescriptor().getName());
    String units = getDescriptor().getUnits();
    if (units != null && units.length() > 0) {
      result.append(' ').append(units);
    }
    if (filter == FILTER_PERSEC) {
      result.append("/sec");
    } else if (filter == FILTER_PERSAMPLE) {
      result.append("/sample");
    }
    result.append(": samples=").append(getSnapshotsSize());
    if (startTime != -1) {
      result.append(" startTime=\"").append(new Date(startTime)).append("\"");
    }
    if (endTime != -1) {
      result.append(" endTime=\"").append(new Date(endTime)).append("\"");
    }
    result.append(" min=").append(StatArchiveReader.nf.format(min));
    result.append(" max=").append(StatArchiveReader.nf.format(max));
    result.append(" average=").append(StatArchiveReader.nf.format(avg));
    result.append(" stddev=").append(StatArchiveReader.nf.format(stddev));
    result.append(" last=") // for bug 42532
        .append(StatArchiveReader.nf.format(mostRecent));
    return result.toString();
  }
}
