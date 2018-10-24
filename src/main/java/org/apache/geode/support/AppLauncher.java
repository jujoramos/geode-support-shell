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
package org.apache.geode.support;

import java.io.IOException;
import java.nio.file.Paths;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.shell.jline.JLineShellAutoConfiguration;
import org.springframework.shell.jline.PromptProvider;

/**
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = { "org.apache.geode.support.service", "org.apache.geode.support.command.statistics", "org.apache.geode.support.command.logs" })
public class AppLauncher {

  public static void main(String args[]) {
    SpringApplication.run(AppLauncher.class, args);
  }

  @Bean
  public PromptProvider promptProvider() {
    return () -> new AttributedString("geode-support-shell>", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
  }

  /**
   * Same as Spring-Shell default, but uses a custom filename.
   * @see JLineShellAutoConfiguration
   *
   */
  @Configuration
  public static class HistoryConfiguration {
    private final static String DISABLED = "disabled";
    @Autowired private History history;

    @Bean
    public History history(LineReader lineReader, @Value("${app.history.file}") String historyPath) {
      if (!DISABLED.equals(historyPath)) {
        lineReader.setVariable(LineReader.HISTORY_FILE, Paths.get(historyPath));
      }

      return new DefaultHistory(lineReader);
    }

    @EventListener
    public void onContextClosedEvent(ContextClosedEvent event) throws IOException {
      history.save();
    }
  }
}
