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

import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.utils.FormatUtils;

// TODO: Spring doesn't know how to convert from String to Path. Add a custom converter and use Path instead of the old Sampling class.
public class AbstractStatisticsCommand<V>  {
  protected FilesService filesService;
  protected StatisticsService statisticsService;
  protected final BorderStyle borderStyle = BorderStyle.fancy_double;

  @Autowired
  public AbstractStatisticsCommand(FilesService filesService, StatisticsService statisticsService) {
    this.filesService = filesService;
    this.statisticsService = statisticsService;
  }

  /**
   * Builds the Errors Table, containing the errors that happened while parsing the statistics files.
   *
   * @param sourcePath The original sourcePath used to scan for statistics to parse.
   * @param parsingResults The list of parsing results returned by the service layer.
   * @return The errors Table, or null if no errors happened while parsing the files.
   */
  protected Table buildErrosTable(Path sourcePath, List<ParsingResult<V>> parsingResults) {
    Table errosTable = null;
    TableModelBuilder<String> errorsModelBuilder = new TableModelBuilder<String>().addRow().addValue("File Name").addValue("Error Description");

    parsingResults.stream()
        .filter(ParsingResult::isFailure)
        .forEach(parsingResult -> {
          Exception exception = parsingResult.getException();
          String filePath = FormatUtils.relativizePath(sourcePath, parsingResult.getFile());
          String errorMessage = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
          errorsModelBuilder.addRow()
              .addValue(filePath)
              .addValue(errorMessage);
        });

    TableBuilder errorsTableBuilder = new TableBuilder(errorsModelBuilder.build());
    if (errorsTableBuilder.getModel().getRowCount() > 1) errosTable = errorsTableBuilder.addFullBorder(borderStyle).build();

    return errosTable;
  }
}
