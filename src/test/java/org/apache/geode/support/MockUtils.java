package org.apache.geode.support;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MockUtils {
  
  public static Path mockPath(String absolutePath) {
    Path mockedPath = mock(Path.class);
    File mockedFile = mock(File.class);
    when(mockedFile.toPath()).thenReturn(mockedPath);
    when(mockedPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedPath.toAbsolutePath().toString()).thenReturn(absolutePath);

    return mockedPath;
  }

  public static long mockTimeStamp(int year, int month, int dayOfMonth, int hour, int minutes, int seconds, ZoneId zoneId) {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minutes, seconds, 0, zoneId);

    return zonedDateTime.toInstant().toEpochMilli();
  }
}
