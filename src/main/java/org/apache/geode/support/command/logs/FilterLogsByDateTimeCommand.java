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
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.command.AbstractCommand;
import org.apache.geode.support.domain.Interval;
import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.LogsService;
import org.apache.geode.support.utils.FormatUtils;

@ShellComponent
@ShellCommandGroup("Logs Commands")
public class FilterLogsByDateTimeCommand extends AbstractCommand {
  private LogsService logsService;

  @Autowired
  public FilterLogsByDateTimeCommand(FilesService filesService, LogsService logsService) {
    super(filesService);
    this.logsService = logsService;
  }

  @ShellMethod(key = "filter logs by date-time", value = "Scan the log files contained within the source folder and copy them to different folders, depending on whether they match the specified date time or not.")
  List<?> filterLogsByDateTime(
      @ShellOption(help = "Year to look for within the log samples.", value = "--year") @Min(2010) Integer year,
      @ShellOption(help = "Month [1 - 12] to look for within the log samples.", value = "--month") @Min(1) @Max(12) Integer month,
      @ShellOption(help = "Day of Month [1 - 31] to look for within the log samples.", value = "--day") @Min(1) @Max(31) Integer day,
      @ShellOption(help = "Hour of day [00 - 23] to look for within the log samples.", value = "--hour", defaultValue = ShellOption.NULL) @Min(0) @Max(23) Integer hour,
      @ShellOption(help = "Minute of Hour [00 - 59] to look for within the log samples.", value = "--minute", defaultValue = ShellOption.NULL) @Min(0) @Max(59) Integer minute,
      @ShellOption(help = "Second of minute [00 - 59] to look for within the log samples.", value = "--second", defaultValue = ShellOption.NULL) @Min(0) @Max(59) Integer second,
      @ShellOption(help = "Directory to scan for logs.", value = "--sourceFolder") File sourceFolder,
      @ShellOption(help = "Directory where matching files should be copied to.", value = "--matchingFolder") File matchingTargetFolder,
      @ShellOption(help = "Directory where non matching files should be copied to.", value = "--nonMatchingFolder") File nonMatchingTargetFolder,
      @ShellOption(help = "Time Zone Id to use when filtering.", value = "--timeZone") ZoneId zoneId) {

    // Use paths from here.
    Path sourceFolderPath = sourceFolder.toPath();
    Path matchingFolderPath = matchingTargetFolder.toPath();
    Path nonMatchingFolderPath = nonMatchingTargetFolder.toPath();

    // Check permissions.
    filesService.assertFolderReadability(sourceFolderPath);

    // Check constraints.
    filesService.assertPathsInequality(sourceFolderPath, matchingFolderPath, "sourceFolder", "matchingFolder");
    filesService.assertPathsInequality(sourceFolderPath, nonMatchingFolderPath, "sourceFolder", "nonMatchingFolder");
    filesService.assertPathsInequality(matchingFolderPath, nonMatchingFolderPath, "matchingFolder", "nonMatchingFolder");

    // Parse logs metadata.
    List<Object> commandResult = new ArrayList<>();
    List<ParsingResult<LogMetadata>> parsingResults = logsService.parseInterval(sourceFolder.toPath());
    TableModelBuilder<String> resultModelBuilder = new TableModelBuilder<>();
    resultModelBuilder.addRow().addValue("File Name").addValue("Matches");
    TableModelBuilder<String> errorsModelBuilder = new TableModelBuilder<>();
    errorsModelBuilder.addRow().addValue("File Name").addValue("Error Description");

    // Process Results.
    if (parsingResults.isEmpty()) {
      commandResult.add("No log files found.");
    } else {
      parsingResults.sort(Comparator.comparing(ParsingResult::getFile));

      parsingResults.forEach(
          parsingResult -> {
            boolean matches;
            String filePath = FormatUtils.relativizePath(sourceFolder.toPath(), parsingResult.getFile());

            // Proceed if parsing succeeded.
            if (parsingResult.isSuccess()) {
              LogMetadata metadataFile = parsingResult.getData();
              Instant startInstant = Instant.ofEpochMilli(metadataFile.getStartTimeStamp());
              Instant finishInstant = Instant.ofEpochMilli(metadataFile.getFinishTimeStamp());
              Interval fileInterval = Interval.of(zoneId, startInstant, finishInstant);
              matches = intervalMatchesFilter(fileInterval, year, month, day, hour, minute, second);

              // Add row to results table no matter what.
              resultModelBuilder.addRow()
                  .addValue(filePath)
                  .addValue(String.valueOf(matches));

              // Try to move the file, add the row to the errors table if needed.
              try {
                filesService.copyFile(parsingResult.getFile(), matches ? matchingFolderPath : nonMatchingFolderPath);
              } catch (IOException ioException) {
                errorsModelBuilder.addRow()
                    .addValue(filePath)
                    .addValue(ioException.getMessage());
              }
            } else {
              Exception exception = parsingResult.getException();
              String errorMessage = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
              errorsModelBuilder.addRow()
                  .addValue(filePath)
                  .addValue(errorMessage);
            }
          }
      );

      TableBuilder resultsTableBuilder = new TableBuilder(resultModelBuilder.build());
      if (resultsTableBuilder.getModel().getRowCount() > 1) {
        commandResult.add(resultsTableBuilder.addFullBorder(borderStyle).build());
      }

      TableBuilder errorsTableBuilder = new TableBuilder(errorsModelBuilder.build());
      if (errorsTableBuilder.getModel().getRowCount() > 1) {
        commandResult.add(errorsTableBuilder.addFullBorder(borderStyle).build());
      }
    }

    return commandResult;
  }
}
