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
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

/**
 * Custom assertion to verify Table data.
 */
public class TableAssert extends AbstractAssert<TableAssert, Table> {

 private TableAssert(Table actual) {
    super(actual, TableAssert.class);
  }

  /**
   * Entry point for all assertions.
   */
  public static TableAssert assertThat(Table actual) {
    return new TableAssert(actual);
  }

  /**
   * Asserts that the table has the expected amount of rows.
   */
  public TableAssert rowCountIsEqualsTo(int rowCount) {
    isNotNull();
    org.assertj.core.api.Assertions.assertThat(actual.getModel().getRowCount()).isEqualTo(rowCount);

    return this;
  }

  /**
   * Asserts that the table has the expected amount of columns.
   */
  public TableAssert columnCountIsEqualsTo(int columnCount) {
    isNotNull();
    org.assertj.core.api.Assertions.assertThat(actual.getModel().getColumnCount()).isEqualTo(columnCount);

    return this;
  }

  /**
   * Returns a RowAssert object for the given row.
   */
  public RowAssert row(int row) {
    isNotNull();
    TableModel model = actual.getModel();
    int columnCount = model.getColumnCount();
    String[] actualRow = new String[columnCount];
    for (int i = 0; i < columnCount; i++) actualRow[i] = model.getValue(row, i).toString();

    return new RowAssert(actualRow);
  }
}
