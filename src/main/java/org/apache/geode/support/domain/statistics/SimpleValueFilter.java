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

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import org.apache.geode.internal.statistics.ValueFilter;

/**
 *
 */
public class SimpleValueFilter implements ValueFilter {
  private final String typeId;
  private final String instanceId;
  private final String statisticId;
  private final Pattern typeIdPattern;
  private final Pattern instanceIdPattern;
  private final Pattern statisticIdPattern;
  private final Pattern archiveNamePattern;

  /**
   *
   * @param typeId
   * @param instanceId
   * @param statisticId
   */
  public SimpleValueFilter(String typeId, String instanceId, String statisticId, String archiveNamePattern) {
    this.typeId = typeId;
    this.instanceId = instanceId;
    this.statisticId = statisticId;

    if (StringUtils.isBlank(typeId)) {
      this.typeIdPattern = null;
    } else {
      this.typeIdPattern = Pattern.compile(".*" + typeId, Pattern.CASE_INSENSITIVE);
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

  public String getTypeId() {
    return typeId;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public String getStatisticId() {
    return statisticId;
  }

  @Override
  public boolean archiveMatches(File archive) {
    if (this.archiveNamePattern == null) {
      return ValueFilter.super.archiveMatches(archive);
    } else {
      Matcher m = this.archiveNamePattern.matcher(archive.getName());
      return m.matches();
    }
  }

  @Override
  public boolean statMatches(String statName) {
    if (this.statisticIdPattern == null) {
      return true;
    } else {
      Matcher m = this.statisticIdPattern.matcher(statName);
      return m.matches();
    }
  }

  @Override
  public boolean typeMatches(String typeName) {
    if (this.typeIdPattern == null) {
      return true;
    } else {
      Matcher m = this.typeIdPattern.matcher(typeName);
      return m.matches();
    }
  }

  @Override
  public boolean instanceMatches(String textId, long numericId) {
    if (this.instanceIdPattern == null) {
      return true;
    } else {
      Matcher m = this.instanceIdPattern.matcher(textId);
      if (m.matches()) {
        return true;
      }

      m = this.instanceIdPattern.matcher(String.valueOf(numericId));
      return m.matches();
    }
  }

  @Override
  public String toString() {
    return "SimpleValueFilter[typeId=" + this.typeId + ", instanceId=" + this.instanceId + " statisticId=" + this.statisticId + "]";
  }
}
