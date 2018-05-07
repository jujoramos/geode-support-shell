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
package org.apache.geode.support.domain;

import java.nio.file.Path;

public class ParsingResult<V> {
  private final V data;
  private final Path file;
  private final boolean success;
  private final Exception exception;

  public boolean isSuccess() {
    return success;
  }

  public boolean isFailure() {
    return !success;
  }

  public Path getFile() {
    return file;
  }

  public V getData() {
    if (!success)
      throw new IllegalArgumentException("Parsing failed, no data available.");

    return data;
  }

  public Exception getException() {
    if (success)
      throw new IllegalArgumentException("Parsing succeeded, no data available.");

    return exception;
  }

  public ParsingResult(Path file, V data) {
    this.file = file;
    this.data = data;
    this.success = true;
    this.exception = null;
  }

  public ParsingResult(Path file, Exception exception) {
    this.file = file;
    this.data = null;
    this.success = false;
    this.exception = exception;
  }
}
