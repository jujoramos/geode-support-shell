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
package org.apache.geode.support.test.mockito;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;

import org.apache.geode.internal.statistics.StatArchiveReader;
import org.apache.geode.internal.statistics.StatValue;

public class MockUtils {

  /**
   *
   * @param absolutePath
   * @param directory
   * @return
   */
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

  /**
   *
   * @param year
   * @param month
   * @param dayOfMonth
   * @param hour
   * @param minutes
   * @param seconds
   * @param zoneId
   * @return
   */
  public static long mockTimeStamp(int year, int month, int dayOfMonth, int hour, int minutes, int seconds, ZoneId zoneId) {
    ZonedDateTime zonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minutes, seconds, 0, zoneId);

    return zonedDateTime.toInstant().toEpochMilli();
  }

  /**
   *
   * @param name
   * @param description
   * @return
   */
  public static StatArchiveReader.ResourceType mockResourceType(String name, String description) {
    StatArchiveReader.ResourceType resourceType = mock(StatArchiveReader.ResourceType.class);
    when(resourceType.getName()).thenReturn(name);
    when(resourceType.getDescription()).thenReturn(description);

    return resourceType;
  }

  /**
   *
   * @param name
   * @param description
   * @param isCounter
   * @param units
   * @return
   */
  public static StatValue mockStatValue(String name, String description, boolean isCounter, String units) {
    StatValue statValue = mock(StatValue.class);
    StatArchiveReader.StatDescriptor statDescriptor = mock(StatArchiveReader.StatDescriptor.class);
    when(statDescriptor.getName()).thenReturn(name);
    when(statDescriptor.getDescription()).thenReturn(description);
    when(statDescriptor.isCounter()).thenReturn(isCounter);
    when(statDescriptor.getUnits()).thenReturn(units);
    when(statValue.getDescriptor()).thenReturn(statDescriptor);

    return statValue;
  }

  /**
   *
   * @param name
   * @param description
   * @param isCounter
   * @param units
   * @param min
   * @param max
   * @param average
   * @param lastValue
   * @param standardDeviation
   * @return
   */
  public static StatValue mockStatValue(String name, String description, boolean isCounter, String units, double min, double max, double average, double lastValue, double standardDeviation) {
    StatValue statValue = mockStatValue(name, description, isCounter, units);
    when(statValue.getSnapshotsMinimum()).thenReturn(min);
    when(statValue.getSnapshotsMaximum()).thenReturn(max);
    when(statValue.getSnapshotsAverage()).thenReturn(average);
    when(statValue.getSnapshotsMostRecent()).thenReturn(lastValue);
    when(statValue.getSnapshotsStandardDeviation()).thenReturn(standardDeviation);
    when(statValue.getSnapshots()).thenReturn(new double[] { min, max, average, lastValue, standardDeviation});

    return statValue;
  }

  /**
   *
   * @param isLoaded
   * @param resourceType
   * @param values
   * @return
   */
  public static StatArchiveReader.ResourceInst mockResourceInstance(boolean isLoaded, StatArchiveReader.ResourceType resourceType, StatValue[] values) {
    StatArchiveReader.ResourceInst resourceInstance = mock(StatArchiveReader.ResourceInst.class);
    when(resourceInstance.isLoaded()).thenReturn(isLoaded);
    when(resourceInstance.getType()).thenReturn(resourceType);
    when(resourceInstance.getStatValues()).thenReturn(values);

    return resourceInstance;
  }

  /**
   *
   * @param table
   */
  public static void printTable(Table table) {
    TableModel resultTableModel = table.getModel();
    int rowCount = resultTableModel.getRowCount();
    int columnCount = resultTableModel.getColumnCount();

    for (int i = 0; i < rowCount; i++) {
      for (int j = 0; j < columnCount; j++) System.out.print(resultTableModel.getValue(i, j) + " ");
      System.out.println();
    }

    System.out.println();
  }
}
