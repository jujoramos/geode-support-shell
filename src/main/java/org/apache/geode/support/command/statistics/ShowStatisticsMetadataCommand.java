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

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.command.AbstractCommand;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.SamplingMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.utils.FormatUtils;

@ShellComponent
@ShellCommandGroup("Statistics Commands")
public class ShowStatisticsMetadataCommand extends AbstractCommand {
  private StatisticsService statisticsService;

  @Autowired
  public ShowStatisticsMetadataCommand(FilesService filesService, StatisticsService statisticsService) {
    super(filesService);
    this.statisticsService = statisticsService;
  }

  @ShellMethod(key = "show statistics metadata", value = "Show general information about statistics files.")
  List<?> showStatisticsMetadata(
      @ShellOption(help = "Path to statistics file, or directory to scan for statistics files.", value = "--path") File source,
      @ShellOption(help = "Time Zone Id to use when showing results. If not set, the default from the statistics file will be used.", value = "--timeZone", defaultValue = ShellOption.NULL) ZoneId zoneId) {

    // Use paths from here.
    Path sourcePath = source.toPath();

    // Check permissions.
    filesService.assertFileReadability(sourcePath);

    List<Object> commandResult = new ArrayList<>();
    List<ParsingResult<SamplingMetadata>> parsingResults = statisticsService.parseMetadata(sourcePath);
    TableModelBuilder<String> resultsModelBuilder = new TableModelBuilder<>();
    addMetadataHeader(resultsModelBuilder, zoneId);

    if (parsingResults.isEmpty()) {
      commandResult.add("No statistics files found.");
    } else {
      parsingResults.sort(Comparator.comparing(ParsingResult::getFile));
      parsingResults.stream().filter(ParsingResult::isSuccess).forEach(
          parsingResult -> {
            String filePath = FormatUtils.relativizePath(sourcePath, parsingResult.getFile());

            if (parsingResult.isSuccess()) {
              SamplingMetadata metadataFile = parsingResult.getData();
              ZoneId formattingZoneId = zoneId != null ? zoneId : metadataFile.getTimeZoneId();

              // Show dates using local format, but original time zone.
              Instant startInstant = Instant.ofEpochMilli(metadataFile.getStartTimeStamp());
              Instant finishInstant = Instant.ofEpochMilli(metadataFile.getFinishTimeStamp());
              ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formattingZoneId);
              ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formattingZoneId);

              resultsModelBuilder.addRow()
                  .addValue(filePath)
                  .addValue(FormatUtils.trimProductVersion(metadataFile.getProductVersion()))
                  .addValue(metadataFile.getOperatingSystem())
                  .addValue(metadataFile.getTimeZoneId().toString())
                  .addValue(startTime.format(FormatUtils.getDateTimeFormatter()))
                  .addValue(finishTime.format(FormatUtils.getDateTimeFormatter()));
            }
          }
      );

      TableBuilder resultsTableBuilder = new TableBuilder(resultsModelBuilder.build());
      if (resultsTableBuilder.getModel().getRowCount() > 1) commandResult.add(resultsTableBuilder.addFullBorder(borderStyle).build());
      @SuppressWarnings("unchecked") Table errorsTable = buildErrorsTable(sourcePath, parsingResults);
      if (errorsTable != null) commandResult.add(errorsTable);
    }

    return commandResult;
  }
}
