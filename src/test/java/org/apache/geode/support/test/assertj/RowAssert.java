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
package org.apache.geode.support.test.assertj;

import org.assertj.core.api.AbstractAssert;

/**
 * Custom assertion to verify row data.
 */
public class RowAssert extends AbstractAssert<RowAssert, String[]> {

  RowAssert(String[] actual) {
    super(actual, RowAssert.class);
  }

  /**
   * Entry point for all assertions.
   */
  public static RowAssert assertThat(String[] actual) {
    return new RowAssert(actual);
  }

  /**
   * Asserts that all cells within a row match the specified array.
   */
  public RowAssert isEqualTo(String... cells) {
    isNotNull();
    org.assertj.core.api.Assertions.assertThat(actual.length).isEqualTo(cells.length);
    for (int i = 0; i < actual.length; i++) org.assertj.core.api.Assertions.assertThat(actual[i]).isEqualTo(cells[i]);

    return this;
  }
}
