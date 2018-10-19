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
package org.apache.geode.support.utils;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;

public class FormatUtils {
  private static final DateTimeFormatter defaultDateTimeFormatter;
  private static final ThreadLocal<NumberFormat>
      defaultNumberFormatter =
      ThreadLocal.withInitial(() -> new DecimalFormat("#0.00"));

  static {
    defaultDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormatterBuilder
        .getLocalizedDateTimePattern(FormatStyle.MEDIUM, FormatStyle.MEDIUM, IsoChronology.INSTANCE,
            Locale
                .getDefault()));
  }

  public static NumberFormat getNumberFormatter() {
    return defaultNumberFormatter.get();
  }

  public static DateTimeFormatter getDateTimeFormatter() {
    return defaultDateTimeFormatter;
  }

  /**
   * Check whether file2 path is contained within file1 path, in which case
   * the repetitive part of the path is removed.
   * If the path from both files are the same, then no trim is done.
   * @param file1 Path to file1.
   * @param file2 Path to file2.
   * @return Trimmed path from file2, or the original file2 path if both paths are the same.
   */
  public static String relativizePath(Path file1, Path file2) {
    String resultPath;
    String file1Path = file1.toAbsolutePath().toString();
    String file2Path = file2.toAbsolutePath().toString();

    if (file2Path.contains(file1Path) && !file2Path.equals(file1Path)) {
      resultPath = file2Path.substring(file1Path.length());
    } else {
      resultPath = file2Path;
    }

    return resultPath;
  }

  /**
   * Removes, from the product version parsed from the statistics files, the build information and
   * date information.
   * @param productVersion Original Product Version Parsed from the statistics files.
   * @return The Product Version, without the build information.
   */
  public static String trimProductVersion(String productVersion) {
    // Ignore buildId and sourceDate. See GemFireStatSampler#getProductDescription().
    if (productVersion.contains("#")) {
      return productVersion.substring(0, productVersion.indexOf("#")).trim();
    } else {
      return productVersion.trim();
    }
  }

  /**
   * Adds `[` and `]` to the zoneId.
   * @param zoneId Original zoneId.
   * @return [zoneId].
   */
  public static String formatTimeZoneId(ZoneId zoneId) {
    return zoneId != null ? "[" + zoneId.toString() + "]" : "";
  }
}
