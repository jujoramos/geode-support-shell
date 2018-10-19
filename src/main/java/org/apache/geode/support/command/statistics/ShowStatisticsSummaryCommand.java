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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModel;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.Category;
import org.apache.geode.support.domain.statistics.Sampling;
import org.apache.geode.support.domain.statistics.Statistic;
import org.apache.geode.support.domain.statistics.filters.RegexValueFilter;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.utils.FormatUtils;

@ShellComponent
@ShellCommandGroup("Statistics Commands")
public class ShowStatisticsSummaryCommand extends AbstractStatisticsCommand {

  /**
   * Representation about how the results should be grouped.
   */
  protected enum GroupCriteria {
    Sampling,
    Statistic
  }

  @Autowired
  public ShowStatisticsSummaryCommand(FilesService filesService, StatisticsService statisticsService) {
    super(filesService, statisticsService);
  }

  /**
   * Conditionally adds a row to the result table.
   *
   * @param modelBuilder The Table Model where the row should be added.
   * @param includeEmptyStatistics Whether to add the row if maximum and minimum values are 0.
   * @param name Name to add as the fist column in the row.
   * @param statistic Statistic.
   */
  private void addResultRow(TableModelBuilder<String> modelBuilder, boolean includeEmptyStatistics, String name, Statistic statistic) {
    if (statistic != null) {
      double min = statistic.getMinimum();
      double max = statistic.getMaximum();
      if ((!includeEmptyStatistics) && (statistic.isEmpty())) return;

      modelBuilder
          .addRow()
          .addValue("└──" + name)
          .addValue(FormatUtils.getNumberFormatter().format(min))
          .addValue(FormatUtils.getNumberFormatter().format(max))
          .addValue(FormatUtils.getNumberFormatter().format(statistic.getAverage()))
          .addValue(FormatUtils.getNumberFormatter().format(statistic.getLastValue()))
          .addValue(FormatUtils.getNumberFormatter().format(statistic.getStandardDeviation()));
    }
  }

  /**
   * Builds the result table grouping the statistical data by file Id.
   * Useful when the user wants to compare the statistical data for a single file all together.
   *
   * @param sourcePath Original path from where the samplings were parsed.
   * @param includeEmptyStatistics Whether to add the statistics for which both maximum and minimum values are 0.
   * @param filter Filter to use when showing results (none, per second or per sample).
   * @param parsingResults The parsed samplings.
   * @return A Table with the statistical data, grouped by file Id:
   *
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |/path/to/fileN                                  |Minimum|Maximum|Average|Last Value|Standard Deviation|
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──CategoryId[InstanceId].statisticId           |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──CategoryId[InstanceId].statisticId           |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──CategoryId[InstanceId].statisticId           |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──CategoryId[InstanceId].statisticId           |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   */
  Table buildTableGroupedBySampling(Path sourcePath, boolean includeEmptyStatistics, Statistic.Filter filter, List<ParsingResult<Sampling>> parsingResults) {
    Table result = null;
    parsingResults.sort(Comparator.comparing(ParsingResult::getFile));
    TableModelBuilder<String> resultsModelBuilder = new TableModelBuilder<>();

    parsingResults.stream()
        .filter(ParsingResult::isSuccess)
        .forEach(parsingResult -> {
          Sampling sampling = parsingResult.getData();
          Map<String, Category> categoryMap = sampling.getCategories();
          String filePath = FormatUtils.relativizePath(sourcePath, parsingResult.getFile());

          // Continue only if there's data to show.
          if ((sampling.hasAnyStatistic()) && (sampling.hasAnyNonEmptyStatistic() || includeEmptyStatistics)) {
            resultsModelBuilder.addRow().addValue(filePath).addValue("Minimum").addValue("Maximum").addValue("Average").addValue("Last Value").addValue("Standard Deviation");

            // Data Rows
             categoryMap.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(categoryEntry ->
               categoryEntry.getValue().getStatistics().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(statisticEntry -> {
                 String statName = categoryEntry.getKey().concat(".").concat(statisticEntry.getKey());
                 Statistic statistic = statisticEntry.getValue();
                 statistic.setFilter(filter);
                 addResultRow(resultsModelBuilder, includeEmptyStatistics, statName, statistic);
                })
             );
          }
        });

    TableModel resultModel = resultsModelBuilder.build();
    if (resultModel.getRowCount() > 0) {
      TableBuilder resultsTableBuilder = new TableBuilder(resultModel);
      result = resultsTableBuilder.addFullBorder(borderStyle).build();
    }

    return result;
  }

  /**
   * Builds the result table grouping the statistical data by statistic Id.
   * Useful when the user wants to compare the statistical data among files.
   *
   * @param sourcePath Original path from where the samplings were parsed.
   * @param includeEmptyStatistics Whether to add the statistics for which both maximum and minimum values are 0.
   * @param filter Filter to use when showing results (none, per second or per sample).
   * @param parsingResults The parsed samplings.
   * @return A Table with the statistical data, grouped by statistic Id:
   *
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |CategoryId[InstanceId].statisticId              |Minimum|Maximum|Average|Last Value|Standard Deviation|
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──/path/to/file1                               |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──/path/to/file2                               |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──/path/to/file3                               |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   * |└──/path/to/fileN                               |value  |value  |value  |value     |value             |
   * +------------------------------------------------+-------+-------+-------+----------+------------------+
   */
  Table buildTableGroupedByStatistic(Path sourcePath, boolean includeEmptyStatistics, Statistic.Filter filter, List<ParsingResult<Sampling>> parsingResults) {
    Table result = null;
    Set<String> statistics = new TreeSet<>();
    Map<String, Map<String, Statistic>> fileToStatisticMap = new TreeMap<>();
    TableModelBuilder<String> resultsModelBuilder = new TableModelBuilder<>();

    // Build Partial Results
    parsingResults.stream()
        .filter(ParsingResult::isSuccess)
        .forEach(parsingResult -> {
          Sampling sampling = parsingResult.getData();
          Map<String, Category> categoryMap = sampling.getCategories();
          String filePath = FormatUtils.relativizePath(sourcePath, parsingResult.getFile());

          // Continue only if there's data to show.
          if ((sampling.hasAnyStatistic()) && (sampling.hasAnyNonEmptyStatistic() || includeEmptyStatistics)) {
            // Data Rows
            categoryMap.forEach((key, value)  ->
              value.getStatistics().forEach((statisticKey, statistic) -> {
                String statName = key.concat(".").concat(statisticKey);
                statistic.setFilter(filter);

                // Check again to avoid empty statistics if flag if set as 'false'.
                if (!statistic.isEmpty() || includeEmptyStatistics) {
                  Map<String, Statistic> statisticMap = fileToStatisticMap.get(filePath);
                  if (statisticMap == null) statisticMap = new HashMap<>();

                  statistics.add(statName);
                  statisticMap.put(statName, statistic);
                  fileToStatisticMap.put(filePath, statisticMap);
                }
              })
            );
          }
        });

    // Build Results Table
    statistics.forEach(statName -> {
      // Add intermediate header for the Stat Name
      resultsModelBuilder.addRow().addValue(statName).addValue("Minimum").addValue("Maximum").addValue("Average").addValue("Last Value").addValue("Standard Deviation");

      fileToStatisticMap.forEach((filePath, value) -> {
        Statistic statistic = value.get(statName);
        addResultRow(resultsModelBuilder, includeEmptyStatistics, filePath, statistic);
      });
    });

    TableModel resultModel = resultsModelBuilder.build();
    if (resultModel.getRowCount() > 0) {
      TableBuilder resultsTableBuilder = new TableBuilder(resultModel);
      result = resultsTableBuilder.addFullBorder(borderStyle).build();
    }

    return result;
  }

  @ShellMethod(key = "show statistics summary", value = "Shows Minimum, Maximum, Average, Last Value and Standard Deviation values for a (set of) defined statistics.")
  List<?> showStatisticsSummary(
      @ShellOption(help = "Path to statistics file, or directory to scan for statistics files.", value = "--path") File source,
      @ShellOption(help = "Whether to group results by Sampling or Statistic.", value = "--groupBy", defaultValue = "Sampling") GroupCriteria groupCriteria,
      @ShellOption(help = "Filter to use (none, per second or per sample) when showing results.", value = "--filter", defaultValue = "None") Statistic.Filter statFilter,
      @ShellOption(help = "Whether to include statistics for which all sample values are 0.", value = "--showEmptyStatistics", arity = 1, defaultValue = "false") boolean showEmptyStatistics,
      @ShellOption(help = "Category of the statistic to search for (VMStats, IndexStats, etc.). Can be a regular expression.", value = "--category", defaultValue = ShellOption.NULL) String categoryId,
      @ShellOption(help = "Instance of the statistic to search for (region name, function name, etc.). Can be a regular expression.", value = "--instance", defaultValue = ShellOption.NULL) String instanceId,
      @ShellOption(help = "Name of the statistic to search for (replyWaitsInProgress, delayDuration, etc.). Can be a regular expression.", value = "--statistic", defaultValue = ShellOption.NULL) String statisticId) {

    // Limit the output, showing everything would be overkilling.
    if ((StringUtils.isBlank(categoryId)) && (StringUtils.isBlank(instanceId)) && (StringUtils.isBlank(statisticId))) {
      throw new IllegalArgumentException(String.format("Either '%s', '%s' or '%s' parameter should be specified.", "--category", "--instance", "--statistic"));
    }

    // Use paths from here.
    Path sourcePath = source.toPath();

    // Check file permissions.
    filesService.assertFileReadability(sourcePath);

    // Validations done, start with the command execution.
    List<Object> commandResult = new ArrayList<>();
    List<ParsingResult<Sampling>> parsingResults = statisticsService.parseSampling(sourcePath, Collections.singletonList(new RegexValueFilter(categoryId, instanceId, statisticId, null)));

    if (parsingResults.isEmpty()) {
      commandResult.add("No statistics files found.");
    } else {
      Table resultsTable = GroupCriteria.Sampling.equals(groupCriteria) ? buildTableGroupedBySampling(sourcePath, showEmptyStatistics, statFilter, parsingResults) : buildTableGroupedByStatistic(sourcePath, showEmptyStatistics, statFilter, parsingResults);
      if (resultsTable != null) commandResult.add(resultsTable);
      Table errorsTable = buildErrorsTable(sourcePath, parsingResults);
      if (errorsTable != null) commandResult.add(errorsTable);
      if (commandResult.isEmpty()) commandResult.add("No matching results found.");
    }

    return commandResult;
  }
}
