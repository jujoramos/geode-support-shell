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
package org.apache.geode.support.command.logs;

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
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.command.ExportableCommand;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.service.TableExportService;
import org.apache.geode.support.utils.FormatUtils;

@ShellComponent
@ShellCommandGroup("Logs Commands")
public class ShowLogsMetadataCommand extends ExportableCommand {
  private LogsService logsService;

  @Autowired
  public ShowLogsMetadataCommand(FilesService filesService, TableExportService tableExportService, LogsService logsService) {
    super(filesService, tableExportService);
    this.logsService = logsService;
  }

  @ShellMethod(key = "show logs metadata", value = "Show general information about log files.")
  List<?> showLogsMetadata(
      @ShellOption(help = "Path to the log file, or directory to scan for log files.", value = "--path") File source,
      @ShellOption(help = "Whether to parse the full metadata (slower) or only the time covered by the log file (much faster).", value = "--intervalOnly", arity = 1, defaultValue = "false") boolean intervalOnly,
      @ShellOption(help = "Time Zone Id to use when showing results. If not set, the default from the local system will be used (or the one from the log file, if found and '--intervalOnly' is set as 'false').", value = "--timeZone", defaultValue = ShellOption.NULL) ZoneId zoneId,
      @ShellOption(help = EXPORT_OPTION_HELP, value = EXPORT_OPTION, defaultValue = ShellOption.NULL) File outputFile) {

    // Use paths from here.
    Path sourcePath = source.toPath();

    // Check permissions.
    filesService.assertFileReadability(sourcePath);

    ZoneId defaultZoneId = ZoneId.systemDefault();
    List<Object> commandResult = new ArrayList<>();
    List<ParsingResult<LogMetadata>> parsingResults = (intervalOnly) ? logsService.parseInterval(sourcePath) : logsService.parseMetadata(sourcePath);
    TableModelBuilder<String> resultsModelBuilder = new TableModelBuilder<>();
    addMetadataHeader(resultsModelBuilder, zoneId);

    if (parsingResults.isEmpty()) {
      commandResult.add("No log files found.");
    } else {
      parsingResults.sort(Comparator.comparing(ParsingResult::getFile));
      parsingResults.stream().filter(ParsingResult::isSuccess).forEach(
          parsingResult -> {
            String filePath = FormatUtils.relativizePath(sourcePath, parsingResult.getFile());

            if (parsingResult.isSuccess()) {
              LogMetadata logMetadata = parsingResult.getData();
              ZoneId logFileZoneId = logMetadata.getTimeZoneId();
              ZoneId formattingZoneId = zoneId != null ? zoneId : (logFileZoneId != null) ? logFileZoneId : defaultZoneId;

              // Show dates using local format, but original time zone if available.
              Instant startInstant = Instant.ofEpochMilli(logMetadata.getStartTimeStamp());
              Instant finishInstant = Instant.ofEpochMilli(logMetadata.getFinishTimeStamp());
              ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formattingZoneId);
              ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formattingZoneId);

              resultsModelBuilder.addRow()
                  .addValue(filePath)
                  .addValue(logMetadata.getProductVersion() != null ? FormatUtils.trimProductVersion(logMetadata.getProductVersion()) : "")
                  .addValue(logMetadata.getOperatingSystem() != null ? logMetadata.getOperatingSystem() : "")
                  .addValue((logFileZoneId != null) ? logFileZoneId.toString() : "")
                  .addValue(startTime.format(FormatUtils.getDateTimeFormatter()))
                  .addValue(finishTime.format(FormatUtils.getDateTimeFormatter()));
            }
          }
      );

      buildCommandResult(sourcePath, parsingResults, resultsModelBuilder, outputFile, commandResult);
    }

    return commandResult;
  }
}
