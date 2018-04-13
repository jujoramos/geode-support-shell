package org.apache.geode.support.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class FilesService {

  /**
   *
   * @param file
   * @throws IllegalArgumentException
   */
  public void assertFileExistence(Path file) throws IllegalArgumentException {
    if (!Files.exists(file)) {
      throw new IllegalArgumentException(String.format("File %s does not exist.", file.toAbsolutePath()));
    }
  }

  /**
   *
   * @param folder
   * @throws IllegalArgumentException
   */
  public void assertFolderExistence(Path folder) throws IllegalArgumentException {
    if (!Files.exists(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s does not exist.", folder.toAbsolutePath()));
    }

    if (!Files.isDirectory(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not a directory.", folder.toAbsolutePath()));
    }
  }

  /**
   *
   * @param file
   * @throws IllegalArgumentException
   */
  public void assertFileReadability(Path file) throws IllegalArgumentException {
    assertFileExistence(file);
    if (!Files.isReadable(file)) {
      throw new IllegalArgumentException(String.format("File %s is not readable.", file.toAbsolutePath()));
    }
  }

  /**
   *
   * @param folder
   * @throws IllegalArgumentException
   */
  public void assertFolderReadability(Path folder) throws IllegalArgumentException {
    assertFolderExistence(folder);
    if (!Files.isReadable(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not readable.", folder.toAbsolutePath()));
    }
  }

  /**
   *
   * @param folder
   * @throws IllegalArgumentException
   */
  public void assertFolderWritability(Path folder) throws IllegalArgumentException {
    assertFolderExistence(folder);
    if (!Files.isWritable(folder)) {
      throw new IllegalArgumentException(String.format("Folder %s is not writable.", folder.toAbsolutePath()));
    }
  }

  /**
   *
   * @param path1
   * @param path2
   * @param marker1
   * @param marker2
   * @throws IllegalArgumentException
   */
  public void assertPathsInequality(Path path1, Path path2, String marker1, String marker2) throws IllegalArgumentException {
    if (path1.equals(path2)) {
      throw new IllegalArgumentException(String.format("%s can't be the same as %s.", marker1, marker2));
    }
  }

  /**
   *
   * @param sourceFile
   * @param targetFolder
   * @throws IOException
   */
  public void moveFile(Path sourceFile, Path targetFolder) throws IOException {
    // Create Directory if it doesn't exist.
    if (!Files.exists(targetFolder)) {
      Files.createDirectories(targetFolder);
    }

    Files.move(sourceFile, targetFolder.resolve(sourceFile.getFileName()));
  }
}
