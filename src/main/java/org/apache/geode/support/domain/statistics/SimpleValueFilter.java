package org.apache.geode.support.domain.statistics;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

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

  /**
   *
   * @param typeId
   * @param instanceId
   * @param statisticId
   */
  public SimpleValueFilter(String typeId, String instanceId, String statisticId) {
    this.typeId = typeId;
    this.instanceId = instanceId;
    this.statisticId = statisticId;

    if (StringUtils.isEmpty(typeId)) {
      this.typeIdPattern = null;
    } else {
      this.typeIdPattern = Pattern.compile(".*" + typeId, Pattern.CASE_INSENSITIVE);
    }

    if (StringUtils.isEmpty(instanceId)) {
      this.instanceIdPattern = null;
    } else {
      this.instanceIdPattern = Pattern.compile(instanceId, Pattern.CASE_INSENSITIVE);
    }

    if (StringUtils.isEmpty(statisticId)) {
      this.statisticIdPattern = null;
    } else {
      this.statisticIdPattern = Pattern.compile(statisticId, Pattern.CASE_INSENSITIVE);
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
    return (archive.getName().endsWith(".gz") || archive.getName().endsWith(".gfs"));
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
