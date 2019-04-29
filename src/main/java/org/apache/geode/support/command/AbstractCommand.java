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
package org.apache.geode.support.command;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.domain.Interval;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.utils.FormatUtils;

// TODO: Spring doesn't know how to convert from String to Path. Add a custom converter and use Path instead of the old Sampling class.
public abstract class AbstractCommand<V>  {
  protected FilesService filesService;
  protected final BorderStyle borderStyle = BorderStyle.fancy_double;

  @Autowired
  public AbstractCommand(FilesService filesService) {
    this.filesService = filesService;
  }

  protected void addMetadataHeader(TableModelBuilder<String> tableModelBuilder, ZoneId zoneId) {
    String zoneIdDesc = FormatUtils.formatTimeZoneId(zoneId);
    tableModelBuilder.addRow()
        .addValue("File Name")
        .addValue("Product Version")
        .addValue("Operating System")
        .addValue("Time Zone")
        .addValue("Start Time" + zoneIdDesc)
        .addValue("Finish Time" + zoneIdDesc);
  }

  /**
   * Builds the Results Table.
   *
   * @param resultsModelBuilder The actual data to insert into the table.
   * @return The results Table, or null if no there is no data available.
   */
  protected Table buildResultsTable(TableModelBuilder<String> resultsModelBuilder) {
    Table resultsTable = null;
    TableBuilder resultsTableBuilder = new TableBuilder(resultsModelBuilder.build());
    if (resultsTableBuilder.getModel().getRowCount() > 1) resultsTable = resultsTableBuilder.addFullBorder(borderStyle).build();

    return resultsTable;
  }

  /**
   * Builds the Errors Table, containing the errors that happened while parsing the files.
   *
   * @param sourcePath The original sourcePath used to scan for statistics to parse.
   * @param parsingResults The list of parsing results returned by the service layer.
   * @return The errors Table, or null if no errors happened while parsing the files.
   */
  protected Table buildErrorsTable(Path sourcePath, List<ParsingResult<V>> parsingResults) {
    Table errorsTable = null;
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
    if (errorsTableBuilder.getModel().getRowCount() > 1) errorsTable = errorsTableBuilder.addFullBorder(borderStyle).build();

    return errorsTable;
  }

  protected boolean intervalMatchesFilter(Interval interval, Integer year, Integer month, Integer day, Integer hour, Integer minute, Integer second) {
    boolean matches;
    ZoneId filterZoneId = interval.getZoneId();
    LocalDate dateFilter = LocalDate.of(year, month, day);
    Optional<Integer> hourWrapper = Optional.ofNullable(hour);
    Optional<Integer> minuteWrapper = Optional.ofNullable(minute);
    Optional<Integer> secondsWrapper = Optional.ofNullable(second);

    // Specific point within the time line.
    if (hourWrapper.isPresent() && minuteWrapper.isPresent()) {
      LocalTime timeFilter = LocalTime.of(hourWrapper.get(), minuteWrapper.get(), secondsWrapper.orElse(0));
      ZonedDateTime filterDateTime = ZonedDateTime.of(dateFilter, timeFilter, filterZoneId);
      matches = interval.contains(filterDateTime);
    } else {
      // Interval within the time line.
      ZonedDateTime startOfFilterInterval = ZonedDateTime.of(dateFilter, LocalTime.of(hourWrapper.orElse(0), minuteWrapper.orElse(0), secondsWrapper.orElse(0)), filterZoneId);
      ZonedDateTime finishOfFilterInterval = ZonedDateTime.of(dateFilter, LocalTime.of(hourWrapper.orElse(23), minuteWrapper.orElse(59), secondsWrapper.orElse(59)), filterZoneId);
      matches = interval.overlaps(Interval.of(startOfFilterInterval, finishOfFilterInterval));
    }

    return matches;
  }
}
