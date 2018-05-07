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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import junitparams.JUnitParamsRunner;
import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.shell.table.Table;
import org.springframework.shell.table.TableModel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.util.ReflectionUtils;

import org.apache.geode.support.test.SampleDataUtils;
import org.apache.geode.support.test.VsdHome;

/**
 * Currently there's no easy way of doing proper integration tests with spring-boot + spring-shell.
 * See https://github.com/spring-projects/spring-shell/issues/204.
 */
@ActiveProfiles("test")
@RunWith(JUnitParamsRunner.class)
@SpringBootTest(properties = {
    ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT_ENABLED + "=false",
    InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
})
public class StartVisualStatisticsDisplayCommandIntegrationTest {
  @Autowired
  public Shell shell;

  @Autowired
  public StartVisualStatisticsDisplayCommand startVsdCommand;

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  private VsdHome vsdHome = new VsdHome();

  /**
   *
   */
  @After
  public void destroyLaunchedProcesses() {
    startVsdCommand.launchedProcesses.stream().forEach(processWrapper -> {
      if (processWrapper.process.isAlive()) {
        processWrapper.process.destroyForcibly();

        try {
          processWrapper.process.waitFor();
        } catch (InterruptedException interruptedException) {
          throw new RuntimeException(interruptedException);
        }
      }
    });

    startVsdCommand.launchedProcesses.clear();
  }

  @Test
  public void shellIntegrationTest() throws IOException, InterruptedException {
    vsdHome.exists();

    shell.run(new InputProvider() {
      private boolean invoked = false;

      @Override
      public Input readInput() {
        if (!invoked) {
          invoked = true;
          String command = "start vsd"
              + " --vsdHome " + vsdHome.getVsdHome()
              + " --path " + SampleDataUtils.rootFolder.getAbsolutePath()
              + " --decompressionFolder " + temporaryFolder.getRoot()
              + " --timeZone America/Buenos_Aires";

          return () -> command;
        } else {
          return () -> "exit";
        }
      }
    });

    Optional<StartVisualStatisticsDisplayCommand.ProcessWrapper> vsdProcessWrapper = startVsdCommand.launchedProcesses.stream().findFirst();
    assertThat(vsdProcessWrapper.isPresent());
    assertThat(vsdProcessWrapper.get().success).isTrue();
    assertThat(vsdProcessWrapper.get().process.isAlive());
    assertThat(vsdProcessWrapper.get().environment).isNotNull();
    assertThat(vsdProcessWrapper.get().commandLine.size()).isEqualTo(9);
    assertThat(vsdProcessWrapper.get().environment.containsKey("TZ")).isTrue();
    assertThat(vsdProcessWrapper.get().environment.get("TZ")).isEqualTo("America/Buenos_Aires");
    assertThat(Files.list(temporaryFolder.getRoot().toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldGracefullyIntegrateWithSpringShell() {
    MethodTarget methodTarget = shell.listCommands().get("start vsd");

    assertThat(methodTarget).isNotNull();
    assertThat(methodTarget.getAvailability().isAvailable()).isTrue();
    assertThat(methodTarget.getGroup()).isEqualTo("Statistics Commands");
    assertThat(methodTarget.getHelp()).isEqualTo("Start Visual Statistics Display Tool (VSD).");
    assertThat(methodTarget.getMethod()).isEqualTo(ReflectionUtils.findMethod(StartVisualStatisticsDisplayCommand.class, "startVisualStatisticsDisplayTool", File.class, File.class, File.class, ZoneId.class));
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenVsdHomeCanNotBeResolved() {
    String command = "start vsd";
    String currentVsdHome = startVsdCommand.defaultVsdHome;

    try {
      startVsdCommand.defaultVsdHome = null;
      Object commandResult = shell.evaluate(() -> command);
      assertThat(commandResult).isNotNull();
      assertThat(commandResult).isInstanceOf(IllegalStateException.class);
      assertThat(((IllegalStateException) commandResult).getMessage()).isEqualTo("Visual Statistics Display Tool (VSD) can not be found.");
      assertThat(startVsdCommand.launchedProcesses.isEmpty()).isTrue();
    } finally {
      startVsdCommand.defaultVsdHome = currentVsdHome;
    }
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenVsdScriptDoesNotExistOrIsNotExecutable() throws IOException {
    File binFolder = temporaryFolder.newFolder("apps", "vsd", "bin");
    File rootFolder = binFolder.getParentFile();
    Path executablePath = binFolder.toPath().resolve(SystemUtils.IS_OS_WINDOWS ? "vsd.bat" : "vsd");

    String command = "start vsd --vsdHome " + rootFolder.getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult)).hasMessageMatching("^File (.*) does not exist.$");
    assertThat(startVsdCommand.launchedProcesses.isEmpty()).isTrue();

    Files.createFile(executablePath);
    commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult)).hasMessageMatching("^File (.*) is not executable.$");
    assertThat(startVsdCommand.launchedProcesses.isEmpty()).isTrue();
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldThrowExceptionWhenSourcePathDoesNotExist() {
    vsdHome.exists();
    String command = "start vsd --vsdHome " + vsdHome.getVsdHome() + " --path /temp/mock";

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(IllegalArgumentException.class);
    assertThat(((IllegalArgumentException) commandResult).getMessage()).isEqualTo("File /temp/mock does not exist.");
    assertThat(startVsdCommand.launchedProcesses.isEmpty()).isTrue();
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldLaunchVsdProcessEvenWhenNoFilesAreFound() throws IOException {
    vsdHome.exists();
    File emptyFolder = temporaryFolder.newFolder("emptyFolder");
    String command = "start vsd --vsdHome " + vsdHome.getVsdHome() + " --path " + emptyFolder.getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    Optional<StartVisualStatisticsDisplayCommand.ProcessWrapper> vsdProcessWrapper = startVsdCommand.launchedProcesses.stream().findFirst();
    assertThat(vsdProcessWrapper.isPresent());
    assertThat(vsdProcessWrapper.get().success).isTrue();
    assertThat(vsdProcessWrapper.get().process.isAlive());
    assertThat(vsdProcessWrapper.get().commandLine.size()).isEqualTo(1);
    assertThat(vsdProcessWrapper.get().environment.containsKey("TZ")).isFalse();
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldSetTimeZoneEnvironmentVariable() {
    vsdHome.exists();
    String command = "start vsd --vsdHome " + vsdHome.getVsdHome() + " --timeZone " + ZoneId.systemDefault().toString();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    Optional<StartVisualStatisticsDisplayCommand.ProcessWrapper> vsdProcessWrapper = startVsdCommand.launchedProcesses.stream().findFirst();
    assertThat(vsdProcessWrapper.isPresent());
    assertThat(vsdProcessWrapper.get().success).isTrue();
    assertThat(vsdProcessWrapper.get().process.isAlive());
    assertThat(vsdProcessWrapper.get().environment).isNotNull();
    assertThat(vsdProcessWrapper.get().commandLine.size()).isEqualTo(1);
    assertThat(vsdProcessWrapper.get().environment.containsKey("TZ")).isTrue();
    assertThat(vsdProcessWrapper.get().environment.get("TZ")).isEqualTo(ZoneId.systemDefault().toString());
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldDecompressFilesAndLoadThemIntoVsd() throws IOException {
    vsdHome.exists();
    File decompressionFolder = temporaryFolder.newFolder("decompressed");
    String command = "start vsd"
        + " --vsdHome " + vsdHome.getVsdHome()
        + " --path " + SampleDataUtils.uncorruptedFolder.getAbsolutePath()
        + " --decompressionFolder " + decompressionFolder.getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<String> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(1);
    String resultString = resultList.get(0);
    assertThat(resultString).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    Optional<StartVisualStatisticsDisplayCommand.ProcessWrapper> vsdProcessWrapper = startVsdCommand.launchedProcesses.stream().findFirst();
    assertThat(vsdProcessWrapper.isPresent());
    assertThat(vsdProcessWrapper.get().success).isTrue();
    assertThat(vsdProcessWrapper.get().process.isAlive());
    assertThat(vsdProcessWrapper.get().environment).isNotNull();
    assertThat(vsdProcessWrapper.get().commandLine.size()).isEqualTo(8);
    assertThat(vsdProcessWrapper.get().environment.containsKey("TZ")).isFalse();
    assertThat(Files.list(decompressionFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
  }

  @Test
  public void startVisualStatisticsDisplayToolShouldReturnErrorsTableButLaunchVsdAnywayWhenDecompressionFailsForSomeFiles() throws IOException {
    vsdHome.exists();
    Path basePath = SampleDataUtils.rootFolder.toPath();
    File decompressionFolder = temporaryFolder.newFolder("decompressed");

    String command = "start vsd"
        + " --vsdHome " + vsdHome.getVsdHome()
        + " --path " + basePath.toString()
        + " --decompressionFolder " + decompressionFolder.getAbsolutePath();

    Object commandResult = shell.evaluate(() -> command);
    assertThat(commandResult).isNotNull();
    assertThat(commandResult).isInstanceOf(List.class);
    List<Object> resultList = (List) commandResult;
    assertThat(resultList.size()).isEqualTo(2);

    // Errors Table Should Come First.
    assertThat(resultList.get(0)).isInstanceOf(Table.class);
    TableModel errorsTable = ((Table) resultList.get(0)).getModel();
    int errorsRowCount = errorsTable.getRowCount();
    int errorsColumnCount = errorsTable.getColumnCount();
    assertThat(errorsRowCount).isEqualTo(2);
    assertThat(errorsColumnCount).isEqualTo(2);
    assertThat(errorsTable.getValue(0, 0)).isEqualTo("File Name");
    assertThat(errorsTable.getValue(0, 1)).isEqualTo("Error Description");
    assertThat(errorsTable.getValue(1, 0)).isEqualTo(SampleDataUtils.SampleType.UNPARSEABLE_COMPRESSED.getRelativeFilePath(basePath));
    assertThat(errorsTable.getValue(1, 1)).isEqualTo("Not in GZIP format");

    // Vsd Launch Status
    assertThat(resultList.get(1)).isInstanceOf(String.class);
    String resultString = (String) resultList.get(1);
    assertThat(resultString).isEqualTo("Visual Statistics Display Tool (VSD) successfully started.");
    Optional<StartVisualStatisticsDisplayCommand.ProcessWrapper> vsdProcessWrapper = startVsdCommand.launchedProcesses.stream().findFirst();
    assertThat(vsdProcessWrapper.isPresent());
    assertThat(vsdProcessWrapper.get().success).isTrue();
    assertThat(vsdProcessWrapper.get().process.isAlive());
    assertThat(vsdProcessWrapper.get().environment).isNotNull();
    assertThat(vsdProcessWrapper.get().commandLine.size()).isEqualTo(9);
    assertThat(vsdProcessWrapper.get().environment.containsKey("TZ")).isFalse();
    assertThat(Files.list(decompressionFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.corruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(2);
    assertThat(Files.list(SampleDataUtils.uncorruptedFolder.toPath()).filter(path -> Files.isRegularFile(path)).count()).isEqualTo(7);
  }
}
