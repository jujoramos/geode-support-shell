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
import java.util.Objects;

import org.apache.geode.internal.statistics.ValueFilter;

/**
 *
 */
public abstract class AbstractValueFilter implements ValueFilter {
  final String typeId;
  final String instanceId;
  final String statisticId;
  final String archiveName;

  AbstractValueFilter(String typeId, String instanceId, String statisticId, String archiveName) {
    this.typeId = typeId;
    this.instanceId = instanceId;
    this.statisticId = statisticId;
    this.archiveName = archiveName;
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

  private String getArchiveName() {
    return archiveName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractValueFilter)) {
      return false;
    }
    AbstractValueFilter that = (AbstractValueFilter) o;
    return Objects.equals(getTypeId(), that.getTypeId()) &&
        Objects.equals(getInstanceId(), that.getInstanceId()) &&
        Objects.equals(getStatisticId(), that.getStatisticId()) &&
        Objects.equals(getArchiveName(), that.getArchiveName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTypeId(), getInstanceId(), getStatisticId(), getArchiveName());
  }

  @Override
  public boolean archiveMatches(File archive) {
    return (archive.getName().endsWith(".gz") || archive.getName().endsWith(".gfs"));
  }

  @Override
  public String toString() {
    return "AbstractValueFilter{" +
        "typeId='" + typeId + '\'' +
        ", instanceId='" + instanceId + '\'' +
        ", statisticId='" + statisticId + '\'' +
        ", archiveName='" + archiveName + '\'' +
        '}';
  }
}
