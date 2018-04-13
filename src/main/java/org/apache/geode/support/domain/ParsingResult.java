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
