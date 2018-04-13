/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * https://docs.oracle.com/javase/tutorial/displayCode.html?code=https://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
 */
package org.apache.geode.support;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public class TreeCopier implements FileVisitor<Path> {
  private final Path source;
  private final Path target;
  private final boolean preserve;

  public TreeCopier(Path source, Path target, boolean preserve) {
    this.source = source;
    this.target = target;
    this.preserve = preserve;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
    // before visiting entries in a directory we copy the directory (okay if directory already exists).
    CopyOption[] options = (preserve) ? new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES } : new CopyOption[0];
    Path newdir = target.resolve(source.relativize(dir));

    try {
      Files.copy(dir, newdir, options);
    } catch (FileAlreadyExistsException x) {
      // ignore
    } catch (IOException x) {
      System.err.format("Unable to create: %s: %s%n", newdir, x);
      return FileVisitResult.SKIP_SUBTREE;
    }

    return FileVisitResult.CONTINUE;
  }

  /**
   * Copy source file to target location. If {@code prompt} is true then
   * prompt user to overwrite target if it exists. The {@code preserve}
   * parameter determines if file attributes should be copied/preserved.
   */
  void copyFile(Path source, Path target, boolean preserve) {
    CopyOption[] options = (preserve) ? new CopyOption[] { StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING } : new CopyOption[] { StandardCopyOption.REPLACE_EXISTING };
    if (Files.notExists(target)) {
      try {
        Files.copy(source, target, options);
      } catch (IOException x) {
        System.err.format("Unable to copy: %s: %s%n", source, x);
      }
    }
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
    copyFile(file, target.resolve(source.relativize(file)), preserve);

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
    // fix up modification time of directory when done
    if (exc == null && preserve) {
      Path newdir = target.resolve(source.relativize(dir));
      try {
        FileTime time = Files.getLastModifiedTime(dir);
        Files.setLastModifiedTime(newdir, time);
      } catch (IOException x) {
        System.err.format("Unable to copy all attributes to: %s: %s%n", newdir, x);
      }
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFileFailed(Path file, IOException exc) {
    if (exc instanceof FileSystemLoopException) {
      System.err.println("cycle detected: " + file);
    } else {
      System.err.format("Unable to copy: %s: %s%n", file, exc);
    }

    return FileVisitResult.CONTINUE;
  }
}