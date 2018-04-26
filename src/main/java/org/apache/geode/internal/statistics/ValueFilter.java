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

import org.apache.geode.support.domain.marker.GeodeExtension;

/**
 * Specifies what data from a statistic archive will be of interest to the reader. This is used
 * when loading a statistic archive file to reduce the memory footprint. Only statistic data that
 * matches all four will be selected for loading.
 */
public interface ValueFilter {

  /**
   * Returns true if the specified archive file matches this spec. Any archives whose name does
   * not match this spec will not be selected for loading by this spec.
   */
  @GeodeExtension(reason = "Default behavior is to match statistics files (whether they are compressed or not), so it makes sense to have this here.")
  default boolean archiveMatches(File archive) {
    return (archive.getName().endsWith(".gz") || archive.getName().endsWith(".gfs"));
  }

  /**
   * Returns true if the specified type name matches this spec. Any types whose name does not
   * match this spec will not be selected for loading by this spec.
   */
  boolean typeMatches(String typeName);

  /**
   * Returns true if the specified statistic name matches this spec. Any stats whose name does not
   * match this spec will not be selected for loading by this spec.
   */
  boolean statMatches(String statName);

  /**
   * Returns true if the specified instance matches this spec. Any instance whose text id and
   * numeric id do not match this spec will not be selected for loading by this spec.
   */
  boolean instanceMatches(String textId, long numericId);
}
