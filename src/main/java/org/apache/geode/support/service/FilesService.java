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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class FilesService {

  public void assertFileExistence(Path file) throws IllegalArgumentException {
    if (!Files.exists(file)) {
      throw new IllegalArgumentException(String.format("File %s does not exist.", file.toAbsolutePath()));
    }
  }

  public void assertFileReadability(Path file) throws IllegalArgumentException {
    assertFileExistence(file);
    if (!Files.isReadable(file)) {
      throw new IllegalArgumentException(String.format("File %s is not readable.", file.toAbsolutePath()));
    }
  }

  public void assertFileExecutability(Path file) throws IllegalArgumentException {
    assertFileExistence(file);
    if (!Files.isExecutable(file)) {
      throw new IllegalArgumentException(String.format("File %s is not executable.", file.toAbsolutePath()));
    }
  }

  void assertFolderExistence(Path folder) throws IllegalArgumentException {
    if (!Files.exists(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s does not exist.", folder.toAbsolutePath()));
    }

    if (!Files.isDirectory(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not a directory.", folder.toAbsolutePath()));
    }
  }

  public void assertFolderReadability(Path folder) throws IllegalArgumentException {
    assertFolderExistence(folder);
    if (!Files.isReadable(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not readable.", folder.toAbsolutePath()));
    }
  }

  void assertFolderWritability(Path folder) throws IllegalArgumentException {
    assertFolderExistence(folder);
    if (!Files.isWritable(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not writable.", folder.toAbsolutePath()));
    }
  }

  public void assertPathsInequality(Path path1, Path path2, String marker1, String marker2) throws IllegalArgumentException {
    if (path1.equals(path2)) {
      throw new IllegalArgumentException(String.format("%s can't be the same as %s.", marker1, marker2));
    }
  }

  public void createDirectories(Path folder) throws IOException {
    // Create Directory if it doesn't exist.
    if (!Files.exists(folder)) {
      Files.createDirectories(folder);
    }
  }

  void moveFile(Path sourceFile, Path targetFolder) throws IOException {
    createDirectories(targetFolder);
    Files.move(sourceFile, targetFolder.resolve(sourceFile.getFileName()), StandardCopyOption.REPLACE_EXISTING );
  }

  public void copyFile(Path sourceFile, Path targetFolder) throws IOException {
    createDirectories(targetFolder);
    Files.copy(sourceFile, targetFolder.resolve(sourceFile.getFileName()), StandardCopyOption.REPLACE_EXISTING );
  }
}
