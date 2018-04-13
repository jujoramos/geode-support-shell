package org.apache.geode.support.service;

import java.nio.file.Path;
import java.util.List;

import org.apache.geode.support.domain.ParsingResult;
import org.apache.geode.support.domain.statistics.StatisticFileMetadata;

public interface StatisticsService {

  /**
   * @param path A statistics file, or a directory containing statistics files to scan.
   * @return List of ParsingResult instances, containing the parsed metadata and/or the error occurred while trying to read the file.
   */
  List<ParsingResult<StatisticFileMetadata>> parseMetadata(Path path);
}
