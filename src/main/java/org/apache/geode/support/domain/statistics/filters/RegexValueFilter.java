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
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 *
 */
public class RegexValueFilter extends AbstractValueFilter {
  private final Pattern typeIdPattern;
  private final Pattern instanceIdPattern;
  private final Pattern statisticIdPattern;
  private final Pattern archiveNamePattern;

  public RegexValueFilter(String typeId, String instanceId, String statisticId, String archiveNamePattern) {
    super(typeId, instanceId, statisticId, archiveNamePattern);

    if (StringUtils.isBlank(typeId)) {
      this.typeIdPattern = null;
    } else {
      this.typeIdPattern = Pattern.compile(typeId, Pattern.CASE_INSENSITIVE);
    }

    if (StringUtils.isBlank(instanceId)) {
      this.instanceIdPattern = null;
    } else {
      this.instanceIdPattern = Pattern.compile(instanceId, Pattern.CASE_INSENSITIVE);
    }

    if (StringUtils.isBlank(statisticId)) {
      this.statisticIdPattern = null;
    } else {
      this.statisticIdPattern = Pattern.compile(statisticId, Pattern.CASE_INSENSITIVE);
    }

    if (StringUtils.isBlank(archiveNamePattern)) {
      this.archiveNamePattern = null;
    } else {
      this.archiveNamePattern = Pattern.compile(archiveNamePattern, Pattern.CASE_INSENSITIVE);
    }
  }

  private boolean matches(Pattern pattern, String actual) {
    if (pattern == null) {
      return true;
    } else {
      return pattern.matcher(actual).matches();
    }
  }

  @Override
  public boolean archiveMatches(File archive) {
    if (this.archiveNamePattern == null) {
      return super.archiveMatches(archive);
    } else {
      return this.archiveNamePattern.matcher(archive.getName()).matches();
    }
  }

  @Override
  public boolean statMatches(String statName) {
    return matches(statisticIdPattern, statName);
  }

  @Override
  public boolean typeMatches(String typeName) {
    return matches(typeIdPattern, typeName);
  }

  @Override
  public boolean instanceMatches(String textId, long numericId) {
    boolean matches = matches(instanceIdPattern, textId);
    matches = matches || matches(instanceIdPattern, String.valueOf(numericId));

    return matches;
  }
}
