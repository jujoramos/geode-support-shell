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

public interface StatSpec extends ValueFilter {
  /**
   * Causes all stats that matches this spec, in all archive files, to be combined into a single
   * global stat value.
   */
  int GLOBAL = 2;
  /**
   * Causes all stats that matches this spec, in each archive file, to be combined into a single
   * stat value for each file.
   */
  int FILE = 1;
  /**
   * No combination is done.
   */
  int NONE = 0;

  /**
   * Returns one of the following values: {@link #GLOBAL}, {@link #FILE}, {@link #NONE}.
   */
  int getCombineType();
}
