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
package org.apache.geode.support.command.statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;

import org.apache.geode.support.command.AbstractStatisticsCommand;
import org.apache.geode.support.service.FilesService;
import org.apache.geode.support.service.StatisticsService;
import org.apache.geode.support.utils.FormatUtils;

@ShellComponent
@ShellCommandGroup("Statistics Commands")
public class StartVisualStatisticsDisplayCommand extends AbstractStatisticsCommand {
  public static final String VSD_HOME_KEY = "VSD_HOME";
  private static final Logger logger = LoggerFactory.getLogger(StartVisualStatisticsDisplayCommand.class);
  private StatisticsService statisticsService;
  String defaultVsdHome;
  List<ProcessWrapper> launchedProcesses;

  /**
   *
   */
  static class DaemonStreamGobbler extends Thread {
    final InputStream inputStream;
    final Consumer<String> consumer;

    DaemonStreamGobbler(String threadName, InputStream inputStream, Consumer<String> consumer) {
      super(threadName);
      this.setDaemon(true);
      this.consumer = consumer;
      this.inputStream = inputStream;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }
  }

  /**
   *
   */
  static class ProcessWrapper {
    final boolean success;
    final Process process;
    final List<String> commandLine;
    final Map<String, String> environment;

    ProcessWrapper(boolean started, Process process, Map<String, String> environment, List<String> commandLine) {
      this.success = started;
      this.process = process;
      this.environment = environment;
      this.commandLine = commandLine;
    }
  }

  @Value("${app.vsd.home}")
  public void setDefaultVsdHome(String defaultVsdHome) {
    this.defaultVsdHome = StringUtils.isBlank(defaultVsdHome) ? System.getenv(VSD_HOME_KEY) : defaultVsdHome;
  }

  @Autowired
  public StartVisualStatisticsDisplayCommand(FilesService filesService, StatisticsService statisticsService) {
    super(filesService);
    this.statisticsService = statisticsService;
    this.launchedProcesses = new CopyOnWriteArrayList<>();
  }

  /**
   * Checks whether the operating system is Windows.
   *
   * @return true if the operating system on which the application is running is Windows, false otherwise.
   */
  boolean isWindows() {
    return SystemUtils.IS_OS_WINDOWS;
  }

  /**
   * Resolves the path for the VSD executable, based on the command argument, application parameter or system property, in that order of precedence.
   *
   * @param vsdHome Vsd Home Path specified as the command argument.
   * @return The VSD executable path.
   */
  Path resolveVsdExecutablePath(String vsdHome) {
    Path vsdRootPath;

    if (vsdHome != null) {
      vsdRootPath = Paths.get(vsdHome);
    } else if (!StringUtils.isBlank(defaultVsdHome)) {
      vsdRootPath = Paths.get(defaultVsdHome);
    } else {
      throw new IllegalStateException("Visual Statistics Display Tool (VSD) can not be found.");
    }

    filesService.assertFolderReadability(vsdRootPath);
    Path vsdExecutablePath = vsdRootPath.resolve(Paths.get("bin", isWindows() ? "vsd.bat" : "vsd"));
    filesService.assertFileExecutability(vsdExecutablePath);

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("VSD Executable Path: %s", vsdExecutablePath.toAbsolutePath().toString()));
    }

    return vsdExecutablePath;
  }

  /**
   *
   * @return Predicate to evaluate whether a given path is a regular statistics file.
   */
  private static Predicate<Path> isRegularStatisticsFile() {
    return compressedPath -> Files.isRegularFile(compressedPath) && compressedPath.toString().endsWith(".gfs");
  }

  /**
   *
   * @return Predicate to evaluate whether a given path is a compressed statistics file.
   */
  private static Predicate<Path> isCompressedStatisticsFile() {
    return compressedPath -> Files.isRegularFile(compressedPath) && compressedPath.toString().endsWith(".gz");
  }

  /**
   * Decompress a compressed statistics file to a regular statistics file.
   * The result is stored into the specified folder, and the name of the decompressed file is the same as the original but with ".gfs" extension.
   *
   * @param compressedPath Path to the compressed statistics file.
   * @param targetFolder Folder where decompressed files should be put.
   * @return The path for the decompressed file, which is the same as the original one but with ".gfs" extension.
   * @throws IOException Whenever a problem occurs while decompressing or storing the decompressed bytes into the new file.
   */
  Path decompress(Path compressedPath, Path targetFolder) throws IOException {
    String decompressedName = compressedPath.getFileName().toString().replace(".gz", ".gfs");
    Path decompressedPath = targetFolder.resolve(decompressedName);
    statisticsService.decompress(compressedPath, decompressedPath);

    return decompressedPath;
  }

  /**
   * Builds command line to execute, based on the VSD executable path and the statistics files to load.
   *
   * @param vsdExecutablePath Path to the executable vsd script.
   * @param regularStatisticsFiles List of original statistics files that should be loaded by VSD.
   * @param decompressedStatisticsFiles List of decompressed statistics files that should be loaded by VSD.
   * @return Command line to be executed.
   */
  List<String> buildCommandLine(Path vsdExecutablePath, List<Path> regularStatisticsFiles, List<Path> decompressedStatisticsFiles) {
    List<String> commandLine = new ArrayList<>();
    commandLine.add(vsdExecutablePath.toString());
    commandLine.addAll(regularStatisticsFiles.stream().map(Path::toString).collect(Collectors.toList()));
    commandLine.addAll(decompressedStatisticsFiles.stream().map(Path::toString).collect(Collectors.toList()));

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Command Line to Execute: %s", commandLine));
    }

    return commandLine;
  }

  /**
   * Launches the process specified by the {@code commandLine} and starts two daemon threads to consume its standard error and standard output streams.
   *
   * @param commandLine The command line to execute.
   * @param environment Environment variables to set in the process to execute.
   * @return {@code ProcessWrapper} with the launched process and the error/output stream readers.
   *
   * TODO: spring-shell doesn't support Java 9, which has a much better API to deal with processes. See https://github.com/spring-projects/spring-shell/issues/214.
   */
  ProcessWrapper launchProcess(List<String> commandLine, Map<String, String> environment) {
    // Build VSD Process.
    ProcessBuilder processBuilder = new ProcessBuilder(commandLine).redirectErrorStream(false);
    environment.forEach((key, value) -> processBuilder.environment().put(key, value));

    if (logger.isDebugEnabled()) logger.debug(String.format("Launching process using %s with environment=%s...", commandLine, environment.toString()));

    try {
      Process process = processBuilder.start();
      StringBuilder errorReader = new StringBuilder();

      Consumer<String> errorConsumer = (string) -> {
        errorReader.append(string);
        logger.error(string);
      };

      // Read process stdout.
      DaemonStreamGobbler outputStreamReader = new DaemonStreamGobbler("Daemon_OutputStreamReader", process.getInputStream(), logger::info);

      // Store and read process stderr.
      DaemonStreamGobbler errorStreamReader = new DaemonStreamGobbler("Daemon_ErrorStreamReader", process.getErrorStream(), errorConsumer);

      try {
        errorStreamReader.start();
        outputStreamReader.start();

        errorStreamReader.join(5000);
      } catch (InterruptedException interruptedException) {
        // Shouldn't happen.
        logger.warn("Process Reader Interrupted.", interruptedException);
      }

      if (logger.isDebugEnabled()) logger.debug(String.format("Launching process using %s with environment=%s... Done!.", commandLine, environment.toString()));

      return new ProcessWrapper((errorReader.length() == 0) && (process.isAlive()), process, environment, commandLine);
    } catch (IOException ioException) {
      String errorMessage = String.format("There was an error while launching process using %s with environment=%s.", commandLine, environment);
      logger.error(errorMessage, ioException);

      return new ProcessWrapper(false, null, environment, commandLine);
    }
  }

  @ShellMethod(key = "start vsd", value = "Start Visual Statistics Display Tool (VSD).")
  List<?> startVisualStatisticsDisplayTool(
      @ShellOption(help = "Path to directory to scan for statistics files.", value = "--path", defaultValue = ShellOption.NULL) File source,
      @ShellOption(help = "Path to the Visual Statistics Display Tool Installation Directory.", value = "--vsdHome", defaultValue = ShellOption.NULL) File vsdHome,
      @ShellOption(help = "Path to the folder where decompressed files should be located. If none is specified, compressed files are left alone and won't be loaded into VSD.", value = "--decompressionFolder", defaultValue = ShellOption.NULL) File decompressionFolder,
      @ShellOption(help = "Time Zone to set as system environment variable. This will be used by the Visual Statistics Display Tool (VSD) when showing data. If not set, none is used.", value = "--timeZone", defaultValue = ShellOption.NULL) ZoneId timeZone) {

    List<Object> commandResult = new ArrayList<>();
    List<Path> statisticsFiles = new ArrayList<>();
    List<Path> compressedStatisticsFiles = new ArrayList<>();
    List<Path> decompressedStatisticsFiles = new ArrayList<>();

    // Resolve VSD Local Path.
    Path vsdExecutablePath = resolveVsdExecutablePath(vsdHome != null ? vsdHome.toPath().toString() : null);

    // If specified, the sourcePath should exists, no matter whether it's a folder or a regular file.
    if (source != null) {
      Path sourcePath = source.toPath();
      filesService.assertFileExistence(sourcePath);

      // Obtain the regular and compressed files from the source folder.
      try {
        Files.walk(sourcePath)
            .forEach(currentPath -> {
              if (isRegularStatisticsFile().test(currentPath)) statisticsFiles.add(currentPath);
              if (isCompressedStatisticsFile().test(currentPath)) compressedStatisticsFiles.add(currentPath);
            });
      } catch (IOException ioException) {
        // Shouldn't happen.
        String errorMessage = String.format("There was an error while iterating through the source folder %s.", sourcePath.toString());
        logger.error(errorMessage, ioException);
        commandResult.add(errorMessage);

        return commandResult;
      }

      // Decompression of .gz files, only if decompression folder has been specified and there are compressed files to decompress.
      if ((decompressionFolder != null) && (!compressedStatisticsFiles.isEmpty())) {
        Path decompressedPath = decompressionFolder.toPath();
        TableModelBuilder<String> errorsModelBuilder = new TableModelBuilder<String>().addRow().addValue("File Name").addValue("Error Description");

        try {
          // Create target folder.
          filesService.createDirectories(decompressedPath);

          // Decompress files and fill new list with the decompressed paths.
          compressedStatisticsFiles.forEach(compressedPath -> {
            String filePath = FormatUtils.relativizePath(sourcePath, compressedPath);

            try {
              decompressedStatisticsFiles.add(decompress(compressedPath, decompressedPath));
            } catch (IOException ioException) {
              errorsModelBuilder.addRow()
                  .addValue(filePath)
                  .addValue(ioException.getMessage());
            }
          });

          TableBuilder errorsTableBuilder = new TableBuilder(errorsModelBuilder.build());
          if (errorsTableBuilder.getModel().getRowCount() > 1) commandResult.add(errorsTableBuilder.addFullBorder(borderStyle).build());
        } catch (IOException ioException) {
          String errorMessage = String.format("Decompression folder %s couldn't be created. Compressed files will be ignored.", decompressedPath.toString());
          logger.warn(errorMessage);
          commandResult.add(errorMessage);
        }
      }
    }

    // Build executable command line.
    List<String> commandLine = buildCommandLine(vsdExecutablePath, statisticsFiles, decompressedStatisticsFiles);
    Map<String, String> environment = new HashMap<>();
    if (timeZone != null) environment.put("TZ", timeZone.toString());

    ProcessWrapper vsdProcessWrapper = launchProcess(commandLine, environment);
    launchedProcesses.add(vsdProcessWrapper);
    commandResult.add(vsdProcessWrapper.success ? "Visual Statistics Display Tool (VSD) successfully started." : "There was an error while starting the VSD process, please check logs for details.");

    return commandResult;
  }
}
