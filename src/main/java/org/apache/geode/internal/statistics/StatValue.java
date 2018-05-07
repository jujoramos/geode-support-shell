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

import org.apache.geode.support.domain.marker.GeodeReplacement;

@GeodeReplacement(changes = "Made public.")
public interface StatValue {
  /**
   * {@link StatValue} filter that causes the statistic values to be unfiltered.
   * This causes the raw values written to the archive to be used.
   * <p>
   * This is the default filter for non-counter statistics. To determine if a statistic is not a
   * counter use {@link StatArchiveReader.StatDescriptor#isCounter}.
   */
  int FILTER_NONE = 0;

  /**
   * {@link StatValue} filter that causes the statistic values to be filtered to
   * reflect how often they change per second. Since the difference between two samples is used to
   * calculate the value this causes the {@link StatValue} to have one less
   * sample than {@link #FILTER_NONE}. The instance time stamp that does not have a per second
   * value is the instance's first time stamp
   * {@link StatArchiveReader.ResourceInst#getFirstTimeMillis}.
   * <p>
   * This is the default filter for counter statistics. To determine if a statistic is a counter
   * use {@link StatArchiveReader.StatDescriptor#isCounter}.
   */
  int FILTER_PERSEC = 1;

  /**
   * {@link StatValue} filter that causes the statistic values to be filtered to
   * reflect how much they changed between sample periods. Since the difference between two
   * samples is used to calculate the value this causes the {@link StatValue} to
   * have one less sample than {@link #FILTER_NONE}. The instance time stamp that does not have a
   * per second value is the instance's first time stamp
   * {@link StatArchiveReader.ResourceInst#getFirstTimeMillis}.
   */
  int FILTER_PERSAMPLE = 2;

  /**
   * Creates and returns a trimmed version of this stat value. Any samples taken before
   * <code>startTime</code> and after <code>endTime</code> are discarded from the resulting value.
   * Set a time parameter to <code>-1</code> to not trim that side.
   */
  StatValue createTrimmed(long startTime, long endTime);

  /**
   * Returns true if value has data that has been trimmed off it by a start timestamp.
   */
  boolean isTrimmedLeft();

  /**
   * Gets the {@link StatArchiveReader.ResourceType type} of the resources that this value belongs
   * to.
   */
  StatArchiveReader.ResourceType getType();

  /**
   * Gets the {@link StatArchiveReader.ResourceInst resources} that this value belongs to.
   */
  StatArchiveReader.ResourceInst[] getResources();

  /**
   * Returns an array of timestamps for each unfiltered snapshot in this value. Each returned time
   * stamp is the number of millis since midnight, Jan 1, 1970 UTC.
   */
  long[] getRawAbsoluteTimeStamps();

  /**
   * Returns an array of timestamps for each unfiltered snapshot in this value. Each returned time
   * stamp is the number of millis since midnight, Jan 1, 1970 UTC. The resolution is seconds.
   */
  long[] getRawAbsoluteTimeStampsWithSecondRes();

  /**
   * Returns an array of doubles containing the unfiltered value of this statistic for each point
   * in time that it was sampled.
   */
  double[] getRawSnapshots();

  /**
   * Returns an array of doubles containing the filtered value of this statistic for each point in
   * time that it was sampled.
   */
  double[] getSnapshots();

  /**
   * Returns the number of samples taken of this statistic's value.
   */
  int getSnapshotsSize();

  /**
   * Returns the smallest of all the samples taken of this statistic's value.
   */
  double getSnapshotsMinimum();

  /**
   * Returns the largest of all the samples taken of this statistic's value.
   */
  double getSnapshotsMaximum();

  /**
   * Returns the average of all the samples taken of this statistic's value.
   */
  double getSnapshotsAverage();

  /**
   * Returns the standard deviation of all the samples taken of this statistic's value.
   */
  double getSnapshotsStandardDeviation();

  /**
   * Returns the most recent value of all the samples taken of this statistic's value.
   */
  double getSnapshotsMostRecent();

  /**
   * Returns true if sample whose value was different from previous values has been added to this
   * StatValue since the last time this method was called.
   */
  boolean hasValueChanged();

  /**
   * Returns the current filter used to calculate this statistic's values. It will be one of these
   * values:
   * <ul>
   * <li>{@link #FILTER_NONE}
   * <li>{@link #FILTER_PERSAMPLE}
   * <li>{@link #FILTER_PERSEC}
   * </ul>
   */
  int getFilter();

  /**
   * Sets the current filter used to calculate this statistic's values. The default filter is
   * {@link #FILTER_NONE} unless the statistic is a counter,
   * {@link StatArchiveReader.StatDescriptor#isCounter}, in which case its {@link #FILTER_PERSEC}.
   *
   * @param filter It must be one of these values:
   *        <ul>
   *        <li>{@link #FILTER_NONE}
   *        <li>{@link #FILTER_PERSAMPLE}
   *        <li>{@link #FILTER_PERSEC}
   *        </ul>
   * @throws IllegalArgumentException if <code>filter</code> is not a valid filter constant.
   */
  void setFilter(int filter);

  /**
   * Returns a description of this statistic.
   */
  StatArchiveReader.StatDescriptor getDescriptor();
}
