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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.geode.internal.statistics.StatArchiveReader;

/**
 * Represents the different types of statistics gathered by Geode/GemFire (VMStats, CachePerfStats, etc.).
 * Wrapper of {@link StatArchiveReader.ResourceType}.
 */
public class Category {
  private final String name;
  private final String description;
  private Map<String, Statistic> statistics;

  public Category(String name, String description) {
    Objects.requireNonNull(name, "Statistic name can not be null.");
    Objects.requireNonNull(description, "Statistic unit can not be null.");

    this.name = name;
    this.description = description;
    this.statistics = new HashMap<>();
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Map<String, Statistic> getStatistics() {
    return statistics;
  }

  public boolean hasStatistic(String name) {
    return this.statistics.containsKey(name);
  }

  public Statistic getStatistic(String name) {
   return this.statistics.get(name);
  }

  public void removeStatistic(String statisticId) {
    this.statistics.remove(statisticId);
  }

  public void addStatistic(Statistic statistic) {
    this.statistics.put(statistic.getName(), statistic);
  }

  public boolean isEmpty() {
    return this.statistics.isEmpty();
  }

  @Override
  public String toString() {
    return "Category{" +
        "name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", statistics=" + statistics +
        '}';
  }
}
