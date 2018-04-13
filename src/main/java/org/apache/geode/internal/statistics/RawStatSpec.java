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

/**
 * Wraps an instance of StatSpec but alwasy returns a combine type of NONE.
 */
class RawStatSpec implements StatSpec {
  private final StatSpec spec;

  RawStatSpec(StatSpec wrappedSpec) {
    this.spec = wrappedSpec;
  }

  public int getCombineType() {
    return StatSpec.NONE;
  }

  public boolean typeMatches(String typeName) {
    return spec.typeMatches(typeName);
  }

  public boolean statMatches(String statName) {
    return spec.statMatches(statName);
  }

  public boolean instanceMatches(String textId, long numericId) {
    return spec.instanceMatches(textId, numericId);
  }

  public boolean archiveMatches(File archive) {
    return spec.archiveMatches(archive);
  }
}
