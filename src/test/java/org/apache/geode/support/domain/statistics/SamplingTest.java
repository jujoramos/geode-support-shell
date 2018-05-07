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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class SamplingTest {

  @Test
  public void hasAnyStatisticTest() {
    Sampling emptySampling = new Sampling(mock(SamplingMetadata.class), new HashMap<>());
    assertThat(emptySampling.hasAnyStatistic()).isFalse();

    Category emptyCategory = new Category("Category", "Description");
    Map<String, Category> categoriesMap = new HashMap<>();
    categoriesMap.put(emptyCategory.getName(), emptyCategory);
    Sampling samplingWithEmptyCategory = new Sampling(mock(SamplingMetadata.class), categoriesMap);
    assertThat(samplingWithEmptyCategory.hasAnyStatistic()).isFalse();
  }

  @Test
  public void hasAnyNonEmptyStatisticTest() {
    // Empty Categories.
    Sampling emptySampling = new Sampling(mock(SamplingMetadata.class), new HashMap<>());
    assertThat(emptySampling.hasAnyNonEmptyStatistic()).isFalse();

    // Category With No Statistics.
    Category emptyCategory = new Category("Category", "Description");
    Map<String, Category> categoriesMap = new HashMap<>();
    categoriesMap.put(emptyCategory.getName(), emptyCategory);
    Sampling samplingWithEmptyCategory = new Sampling(mock(SamplingMetadata.class), categoriesMap);
    assertThat(samplingWithEmptyCategory.hasAnyNonEmptyStatistic()).isFalse();

    // Category With Empty Statistics.
    Category nonEmptyCategoryWithEmptyStatistic = new Category("Category", "Description");
    Statistic emptyStatistic = mock(Statistic.class);
    when(emptyStatistic.isEmpty()).thenReturn(true);
    nonEmptyCategoryWithEmptyStatistic.addStatistic(emptyStatistic);
    Map<String, Category> nonEmptyCategoriesMapWithEmptyStatistic = new HashMap<>();
    nonEmptyCategoriesMapWithEmptyStatistic.put(nonEmptyCategoryWithEmptyStatistic.getName(), nonEmptyCategoryWithEmptyStatistic);
    Sampling samplingWithNonEmptyCategoryAndEmptyStatistic = new Sampling(mock(SamplingMetadata.class), nonEmptyCategoriesMapWithEmptyStatistic);
    assertThat(samplingWithNonEmptyCategoryAndEmptyStatistic.hasAnyNonEmptyStatistic()).isFalse();

    // Category With Non Empty Statistic.
    Category nonEmptyCategoryWithNonZeroMaxStatistic = new Category("Category", "Description");
    Statistic nonZeroMaxValueStatistic = mock(Statistic.class);
    when(nonZeroMaxValueStatistic.isEmpty()).thenReturn(false);
    nonEmptyCategoryWithNonZeroMaxStatistic.addStatistic(nonZeroMaxValueStatistic);
    Map<String, Category> nonEmptyCategoriesMapWithNonZeroMaxStatistic = new HashMap<>();
    nonEmptyCategoriesMapWithNonZeroMaxStatistic.put(nonEmptyCategoryWithNonZeroMaxStatistic.getName(), nonEmptyCategoryWithNonZeroMaxStatistic);
    Sampling samplingWithNonEmptyCategoryAndNonZeroMaxStatistic = new Sampling(mock(SamplingMetadata.class), nonEmptyCategoriesMapWithNonZeroMaxStatistic);
    assertThat(samplingWithNonEmptyCategoryAndNonZeroMaxStatistic.hasAnyNonEmptyStatistic()).isTrue();

    // Several Categories With Several Statistics, only the last one non empty.
    Map<String, Category> categoryMap = new LinkedHashMap<>();
    Statistic nonZeroStatistic = mock(Statistic.class);
    when(nonZeroStatistic.getName()).thenReturn("NonEmpty");
    when(nonZeroStatistic.isEmpty()).thenReturn(false);

    for (int i = 0; i < 5; i ++) {
      Category category = new Category("Category_" + i, "Description_ " + i);

      for (int j = 0; j < 10; j ++) {
        Statistic statistic = mock(Statistic.class);
        when(statistic.getName()).thenReturn("Statistic_" + j);
        when(statistic.isEmpty()).thenReturn(true);
        category.addStatistic(statistic);
      }

      categoryMap.put(category.getName(), category);
    }

    categoryMap.get("Category_4").addStatistic(nonZeroStatistic);
    Sampling sampling = new Sampling(mock(SamplingMetadata.class), categoryMap);
    assertThat(sampling.hasAnyNonEmptyStatistic()).isTrue();
  }
}
