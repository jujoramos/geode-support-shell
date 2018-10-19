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

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.internal.statistics.StatValue;

/**
 * Represents a specific statistic with all sampled values.
 * Wrapper of {@link StatArchiveReader.StatDescriptor} and {@link StatValue}.
 */
public class Statistic {
  private final String name;
  private final String units;
  private final boolean counter;
  private final String description;
  private final StatValue sampling;

  public enum Filter {
    None(StatValue.FILTER_NONE),
    Second(StatValue.FILTER_PERSEC),
    Sample(StatValue.FILTER_PERSAMPLE);

    private int value;

    Filter(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public Statistic(StatValue sampling) {
    Objects.requireNonNull(sampling, "Backing StatValue can not be null.");
    Objects.requireNonNull(sampling.getDescriptor(), "Statistic Descriptor name can not be null.");
    Objects.requireNonNull(sampling.getDescriptor().getName(), "Statistic Descriptor name can not be null.");
    Objects.requireNonNull(sampling.getDescriptor().getDescription(), "Statistic Descriptor unit can not be null.");
    Objects.requireNonNull(sampling.getDescriptor().getUnits(), "Statistic Descriptor description can not be null.");

    this.sampling = sampling;
    this.name = sampling.getDescriptor().getName();
    this.units = sampling.getDescriptor().getUnits();
    this.counter = sampling.getDescriptor().isCounter();
    this.description = sampling.getDescriptor().getDescription();
  }

  public void setFilter(Filter filter) {
    this.sampling.setFilter(filter.getValue());
  }

  public String getName() {
    return name;
  }

  public String getUnits() {
    return units;
  }

  public boolean isCounter() {
    return counter;
  }

  public String getDescription() {
    return description;
  }

  public double getMinimum() {
    return sampling.getSnapshotsMinimum();
  }

  public double getMaximum() {
    return sampling.getSnapshotsMaximum();
  }

  public double getAverage() {
    return sampling.getSnapshotsAverage();
  }

  public double getStandardDeviation() {
    return sampling.getSnapshotsStandardDeviation();
  }

  public double getLastValue() {
    return sampling.getSnapshotsMostRecent();
  }

  public boolean isEmpty() {
    Set<Double> uniqueValues = Arrays.stream(sampling.getSnapshots()).boxed().collect(Collectors.toSet());

    return ((uniqueValues.size() == 1) && uniqueValues.contains(0.0));
  }

  @Override
  public String toString() {
    return "Statistic{" +
        "name='" + name + '\'' +
        ", units='" + units + '\'' +
        ", counter=" + counter +
        ", description='" + description + '\'' +
        ", sampling=" + sampling +
        '}';
  }
}
