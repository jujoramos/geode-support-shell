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

import java.util.Iterator;
import java.util.Map;

/**
 * Represents a single statistic sampling.
 */
public class Sampling {
  private final SamplingMetadata metadata;
  private final Map<String, Category> categories;

  /**
   *
   * @param metadata
   */
  public Sampling(SamplingMetadata metadata, Map<String, Category> categories) {
    this.metadata = metadata;
    this.categories = categories;
  }

  /**
   *
   * @return
   */
  public SamplingMetadata getMetadata() {
    return metadata;
  }

  /**
   *
   * @return
   */
  public Map<String, Category> getCategories() {
    return categories;
  }

  /**
   *
   * @param name
   * @return
   */
  public boolean hasCategory(String name) {
    return this.categories.containsKey(name);
  }

  /**
   *
   * @param name
   * @return
   */
  public Category getCategory(String name) {
    return this.categories.get(name);
  }

  /**
   * Checks whether the sampling has any data.
   *
   * @return true if there's at least one category with one statistic, false otherwise.
   */
  public boolean hasAnyStatistic() {
    if (this.categories.isEmpty()) return false;

    boolean empty = true;
    Iterator<Category> categoryIterator = this.categories.values().iterator();
    while (categoryIterator.hasNext() && empty) {
      empty = empty && categoryIterator.next().isEmpty();
    }

    return !empty;
  }

  /**
   * Checks whether the sampling has any data.
   *
   * @return true if there's at least one category with at least one statistic with at least one non zero value, false otherwise.
   */
  public boolean hasAnyNonEmptyStatistic() {
    if (!hasAnyStatistic()) return false;

    boolean empty = true;
    Iterator<Category> categoryIterator = this.categories.values().iterator();
    while (categoryIterator.hasNext() && empty) {
      Category category = categoryIterator.next();

      Iterator<Statistic> statisticIterator = category.getStatistics().values().iterator();
      while (statisticIterator.hasNext() && empty) {
        Statistic statistic = statisticIterator.next();
        empty = empty && statistic.isEmpty();
      }
    }

    return !empty;
  }

  /**
   *
   * @return
   */
  @Override
  public String toString() {
    return "Sampling{" +
        "metadata=" + metadata +
        ", categories=" + categories +
        '}';
  }
}
