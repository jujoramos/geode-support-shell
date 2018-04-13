package org.apache.geode.support.utils;

import java.nio.file.Path;
import java.time.ZoneId;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.Locale;

public class FormatUtils {
  private static final DateTimeFormatter defaultDateTimeFormatter;

  static {
    defaultDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormatterBuilder
        .getLocalizedDateTimePattern(FormatStyle.MEDIUM, FormatStyle.MEDIUM, IsoChronology.INSTANCE, Locale
            .getDefault()));
  }

  public static DateTimeFormatter getDateTimeFormatter() {
    return defaultDateTimeFormatter;
  }

  /**
   * Check whether file2 path is contained within file1 path, in which case
   * the repetitive part of the path is removed.
   * If the path from both files are the same, then no trim is done.
   *
   * @param file1
   * @param file2
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
   *
   * @param productVersion
   * @return
   */
  public static String trimProductVersion(String productVersion) {
    // Ignore buildId and sourceDate. See GemFireStatSampler#getProductDescription().
    if (productVersion.indexOf("#") != -1) {
      return productVersion.substring(0, productVersion.indexOf("#")).trim();
    } else {
      return productVersion.trim();
    }
  }

  /**
   *
   * @param zoneId
   * @return
   */
  public static String formatTimeZoneId(ZoneId zoneId) {
    return zoneId != null ? "[" + zoneId.toString() + "]" : "";
  }
}
