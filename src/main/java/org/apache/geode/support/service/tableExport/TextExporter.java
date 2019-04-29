package org.apache.geode.support.service.tableExport;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.shell.table.Table;

class TextExporter extends AbstractExporter {

  @Override
  void export(Path path, Table table) throws IOException {
    String renderedModel  = table.render(Integer.MAX_VALUE);

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      writer.write(renderedModel);
      writer.flush();
    }
  }
}
