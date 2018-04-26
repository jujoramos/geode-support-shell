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
package org.apache.geode.support.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Files.class, FilesService.class })
public class FilesServiceTest {
  @Mock private Path mockedFile;
  @Mock private Path mockedFolder;
  private FilesService filesService;

  @Before
  public void setUp() {
    filesService = new FilesService();
    PowerMockito.mockStatic(Files.class);
  }

  @Test
  public void assertFileExistenceTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFileExistence(mockedFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^File (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFileExistence(mockedFile)).doesNotThrowAnyException();
  }

  @Test
  public void assertFileReadabilityTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFileReadability(mockedFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^File (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isReadable(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFileReadability(mockedFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^File (.*) is not readable.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isReadable(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFileReadability(mockedFile)).doesNotThrowAnyException();
  }

  @Test
  public void assertFileExecutabilityTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFileExecutability(mockedFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^File (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isExecutable(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFileExecutability(mockedFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^File (.*) is not executable.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isExecutable(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFileExecutability(mockedFile)).doesNotThrowAnyException();
  }

  @Test
  public void assertFolderExistenceTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderExistence(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderExistence(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) is not a directory.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFolderExistence(mockedFolder)).doesNotThrowAnyException();
  }

  @Test
  public void assertFolderReadabilityTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderReadability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderReadability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) is not a directory.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(true);
    when(Files.isReadable(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderReadability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) is not readable.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(true);
    when(Files.isReadable(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFolderReadability(mockedFolder))
        .doesNotThrowAnyException();
  }

  @Test
  public void assertFolderWritabilityTest() {
    when(Files.exists(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderWritability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) does not exist.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderWritability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) is not a directory.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(true);
    when(Files.isWritable(any())).thenReturn(false);
    assertThatThrownBy(() -> filesService.assertFolderWritability(mockedFolder))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^Folder (.*) is not writable.$");

    when(Files.exists(any())).thenReturn(true);
    when(Files.isDirectory(any())).thenReturn(true);
    when(Files.isWritable(any())).thenReturn(true);
    assertThatCode(() -> filesService.assertFolderWritability(mockedFolder))
        .doesNotThrowAnyException();
  }

  @Test
  public void assertPathsInequalityTest() {
    when(mockedFile.equals(mockedFolder)).thenReturn(true);
    assertThatThrownBy(() -> filesService.assertPathsInequality(mockedFile, mockedFolder, "mockedFile", "mockedFolder"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("^mockedFile can't be the same as mockedFolder.$");

    assertThatCode(() -> filesService.assertPathsInequality(mockedFolder, mockedFile, "mockedFolder", "mockedFile"))
        .doesNotThrowAnyException();
  }

  @Test
  public void createDirectoriesTest() throws Exception {
    when(Files.exists(mockedFolder)).thenReturn(true);
    assertThatCode(() -> filesService.createDirectories(mockedFolder)).doesNotThrowAnyException();

    when(Files.exists(mockedFolder)).thenReturn(false);
    assertThatCode(() -> filesService.createDirectories(mockedFolder)).doesNotThrowAnyException();

    when(Files.exists(mockedFolder)).thenReturn(false);
    when(Files.createDirectories(mockedFolder)).thenThrow(new IOException("Mocked IOException When Creating Folder."));
    assertThatThrownBy(() -> filesService.createDirectories(mockedFolder))
        .isInstanceOf(IOException.class)
        .hasMessage("Mocked IOException When Creating Folder.");
  }

  @Test
  public void copyFileTest() throws Exception {
    when(Files.exists(mockedFolder)).thenReturn(false);
    when(Files.createDirectories(mockedFolder)).thenThrow(new IOException("Mocked IOException When Creating Folder."));
    assertThatThrownBy(() -> filesService.copyFile(mockedFile, mockedFolder))
        .isInstanceOf(IOException.class)
        .hasMessage("Mocked IOException When Creating Folder.");

    when(Files.exists(mockedFolder)).thenReturn(true);
    when(Files.copy(any(Path.class), any(), any())).thenThrow(new IOException("Mocked IOException When Moving File."));
    assertThatThrownBy(() -> filesService.copyFile(mockedFile, mockedFolder))
        .isInstanceOf(IOException.class)
        .hasMessage("Mocked IOException When Moving File.");
  }

  @Test
  public void moveFileTest() throws Exception {
    when(Files.exists(mockedFolder)).thenReturn(false);
    when(Files.createDirectories(mockedFolder)).thenThrow(new IOException("Mocked IOException When Creating Folder."));
    assertThatThrownBy(() -> filesService.moveFile(mockedFile, mockedFolder))
        .isInstanceOf(IOException.class)
        .hasMessage("Mocked IOException When Creating Folder.");

    when(Files.exists(mockedFolder)).thenReturn(true);
    when(Files.move(any(), any(), any())).thenThrow(new IOException("Mocked IOException When Moving File."));
    assertThatThrownBy(() -> filesService.moveFile(mockedFile, mockedFolder))
        .isInstanceOf(IOException.class)
        .hasMessage("Mocked IOException When Moving File.");
  }
}
