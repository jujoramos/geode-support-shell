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
package org.apache.geode.support.command.statistics;

import org.springframework.beans.factory.annotation.Autowired;

import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;

// TODO: Spring doesn't know how to convert from String to Path. Add a custom converter and use Path instead of the old File class.
public class AbstractStatisticsCommand {
  protected FilesService filesService;
  protected StatisticsService statisticsService;

  @Autowired
  public AbstractStatisticsCommand(FilesService filesService, StatisticsService statisticsService) {
    this.filesService = filesService;
    this.statisticsService = statisticsService;
  }
}
