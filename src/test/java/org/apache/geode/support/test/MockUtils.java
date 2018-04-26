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
package org.apache.geode.support.test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MockUtils {

  public static Path mockPath(String absolutePath, boolean directory) {
    Path mockedPath = mock(Path.class);
    File mockedFile = mock(File.class);
    when(mockedFile.toPath()).thenReturn(mockedPath);
    when(mockedPath.toFile()).thenReturn(mockedFile);
    when(mockedPath.toString()).thenReturn(absolutePath);
    when(mockedPath.toAbsolutePath()).thenReturn(mock(Path.class));
    when(mockedPath.toAbsolutePath().toString()).thenReturn(absolutePath);

    if (directory) {
      when(mockedPath.resolve(anyString())).thenAnswer(i -> mockPath(absolutePath + "/" + i.getArguments()[0], false));
    } else {
      String fileName = absolutePath;

      if (absolutePath.lastIndexOf("/") != absolutePath.length()) {
        fileName = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
      }

      when(mockedFile.getName()).thenReturn(fileName);
      when(mockedPath.getFileName()).thenReturn(mock(Path.class));
      when(mockedPath.getFileName().toString()).thenReturn(fileName);
    }

    return mockedPath;
  }

  public static long mockTimeStamp(int year, int month, int dayOfMonth, int hour, int minutes, int seconds, ZoneId zoneId) {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minutes, seconds, 0, zoneId);

    return zonedDateTime.toInstant().toEpochMilli();
  }
}
