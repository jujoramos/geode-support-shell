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
package org.apache.geode.support.domain.statistics.filters;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class SimpleValueFilter extends AbstractValueFilter {

  public SimpleValueFilter(String typeId, String instanceId, String statisticId, String archiveName) {
    super(typeId, instanceId, statisticId, archiveName);
  }

  private boolean matches(String pattern, String actual) {
    if (StringUtils.isBlank(pattern)) {
      return true;
    } else {
      return pattern.equalsIgnoreCase(actual);
    }
  }

  @Override
  public boolean archiveMatches(File archive) {
    if (StringUtils.isBlank(archiveName)) {
      return super.archiveMatches(archive);
    }

    return matches(archiveName, archive.getName());
  }

  @Override
  public boolean statMatches(String statName) {
    return matches(statisticId, statName);
  }

  @Override
  public boolean typeMatches(String typeName) {
    return matches(typeId, typeName);
  }

  @Override
  public boolean instanceMatches(String textId, long numericId) {
    boolean matches = matches(instanceId, textId);
    matches = matches || matches(instanceId, String.valueOf(numericId));

    return matches;
  }
}
