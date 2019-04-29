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
package org.apache.geode.support.service.logs.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Properties;

import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.test.junit.TimeZoneRule;
import org.apache.geode.support.test.mockito.MockUtils;

public class Log4JParserTest extends AbstractLogParserTest {
  private final String header8XSample = "  ---------------------------------------------------------------------------\n"
      + "\n"
      + "    Copyright (C) 1997-2015 Pivotal Software, Inc. All rights reserved. This\n"
      + "    product is protected by U.S. and international copyright and intellectual\n"
      + "    property laws. Pivotal products are covered by one or more patents listed\n"
      + "    at http://www.pivotal.io/patents.  Pivotal is a registered trademark\n"
      + "    of trademark of Pivotal Software, Inc. in the United States and/or other\n"
      + "    jurisdictions.  All other marks and names mentioned herein may be\n"
      + "    trademarks of their respective companies.\n"
      + "\n"
      + "  ---------------------------------------------------------------------------\n"
      + "  Java version:   8.2.0 build 17919 08/19/2015 14:58:51 PDT javac 1.7.0_79\n"
      + "  Native version: native code unavailable\n"
      + "  Source revision: a58a25d2be7eab751794e9cc78ff04a03bb09a65\n"
      + "  Source repository: gemfire82_dev\n"
      + "  Running on: /10.69.252.33, 8 cpu(s), x86_64 Mac OS X 10.13.6\n"
      + "  Process ID: 45837\n"
      + "  User: bwayne\n"
      + "  Current dir: /Users/bwayne/temp/tempTests/locator1\n"
      + "  Home dir: /Users/bwayne\n"
      + "  Command Line Parameters:\n"
      + "    -Dgemfire.enable-cluster-configuration=true\n"
      + "    -Dgemfire.load-cluster-configuration-from-dir=false\n"
      + "    -Dgemfire.launcher.registerSignalHandlers=true\n"
      + "    -Djava.awt.headless=true\n"
      + "    -Dsun.rmi.dgc.server.gcInterval=9223372036854775806\n"
      + "  Class Path:\n"
      + "    /Users/bwayne/Applications/GemFire/8.2.0/Pivotal_GemFire_820_b17919_Linux/lib/gemfire.jar\n"
      + "    /Users/bwayne/Applications/GemFire/8.2.0/Pivotal_GemFire_820_b17919_Linux/lib/locator-dependencies.jar\n"
      + "  Library Path:\n"
      + "    /Users/bwayne/Library/Java/Extensions\n"
      + "    /Library/Java/Extensions\n"
      + "    /Network/Library/Java/Extensions\n"
      + "    /System/Library/Java/Extensions\n"
      + "    /usr/lib/java\n"
      + "    .\n"
      + "  System Properties:\n"
      + "      Locator.forceLocatorDMType = true\n"
      + "      awt.toolkit = sun.lwawt.macosx.LWCToolkit\n"
      + "      file.encoding = UTF-8\n"
      + "      file.encoding.pkg = sun.io\n"
      + "      file.separator = /\n"
      + "      ftp.nonProxyHosts = local|*.local|169.254/16|*.169.254/16\n"
      + "      gemfire.enable-cluster-configuration = true\n"
      + "      gemfire.launcher.registerSignalHandlers = true\n"
      + "      gemfire.load-cluster-configuration-from-dir = false\n"
      + "      user.country = US\n"
      + "      user.language = en\n"
      + "      user.timezone = Europe/Dublin\n"
      + "  Log4J 2 Configuration:\n"
      + "      Setting log4j.configurationFile to specify log4j configuration file: 'jar:file:/Users/bwayne/Applications/GemFire/8.2.0/Pivotal_GemFire_820_b17919_Linux/lib/gemfire.jar!/com/gemstone/gemfire/internal/logging/log4j/log4j2-default.xml'\n"
      + "  ---------------------------------------------------------------------------";

  private final String header9XSample = "  ---------------------------------------------------------------------------\n"
      + "\n"
      + "    Licensed to the Apache Software Foundation (ASF) under one or more\n"
      + "    contributor license agreements.  See the NOTICE file distributed with this\n"
      + "    work for additional information regarding copyright ownership.\n"
      + "\n"
      + "    The ASF licenses this file to You under the Apache License, Version 2.0\n"
      + "    (the \"License\"); you may not use this file except in compliance with the\n"
      + "    License.  You may obtain a copy of the License at\n"
      + "\n"
      + "    http://www.apache.org/licenses/LICENSE-2.0\n"
      + "\n"
      + "    Unless required by applicable law or agreed to in writing, software\n"
      + "    distributed under the License is distributed on an \"AS IS\" BASIS, WITHOUT\n"
      + "    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the\n"
      + "    License for the specific language governing permissions and limitations\n"
      + "    under the License.\n"
      + "\n"
      + "  ---------------------------------------------------------------------------\n"
      + "  Build-Date: 2018-03-19 16:50:34 +0000\n"
      + "  Build-Id: root 13\n"
      + "  Build-Java-Version: 1.8.0_161\n"
      + "  Build-Platform: Linux 4.4.0-89-generic amd64\n"
      + "  GemFire-Source-Date: 2018-03-19 16:06:42 +0000\n"
      + "  GemFire-Source-Repository: support/9.4\n"
      + "  GemFire-Source-Revision: a79a89636981719f3153c9a38db87aa49a7f2026\n"
      + "  Product-Name: Pivotal GemFire\n"
      + "  Product-Version: 9.4.0\n"
      + "  Source-Date: 2018-03-16 20:43:10 +0000\n"
      + "  Source-Repository: support/9.4\n"
      + "  Source-Revision: f122abcb40ac9050680ed28b1e66ad73a60cba46\n"
      + "  Native version: native code unavailable\n"
      + "  Running on: /192.168.1.7, 8 cpu(s), amd64 Linux 3.10.0-862.11.6.el7.x86_64 \n"
      + "  Communications version: 80\n"
      + "  Process ID: 32310\n"
      + "  User: bwayne\n"
      + "  Current dir: /Users/bwayne/server1\n"
      + "  Home dir: /Users/bwayne\n"
      + "  Command Line Parameters:\n"
      + "    -Dgemfire.locators=localhost[10101]\n"
      + "    -Dgemfire.start-dev-rest-api=false\n"
      + "    -Dgemfire.use-cluster-configuration=true\n"
      + "    -Dgemfire.security-manager=io.pivotal.support.MySecurityManager\n"
      + "    -Dgemfire.security-username=superUser\n"
      + "    -Dgemfire.security-password=********\n"
      + "    -XX:OnOutOfMemoryError=kill -KILL %p\n"
      + "    -Dgemfire.launcher.registerSignalHandlers=true\n"
      + "    -Djava.awt.headless=true\n"
      + "    -Dsun.rmi.dgc.server.gcInterval=9223372036854775806\n"
      + "  Class Path:\n"
      + "    /Users/bwayne/Applications/GemFire/9.4.0/pivotal-gemfire-9.4.0/lib/geode-core-9.4.0.jar\n"
      + "    /Users/bwayne/security-manager/target/security-manager-1.0.0.jar\n"
      + "    /Users/bwayne/Applications/GemFire/9.4.0/pivotal-gemfire-9.4.0/lib/geode-dependencies.jar\n"
      + "  Library Path:\n"
      + "    /Users/bwayne/Library/Java/Extensions\n"
      + "    /Library/Java/Extensions\n"
      + "    /Network/Library/Java/Extensions\n"
      + "    /System/Library/Java/Extensions\n"
      + "    /usr/lib/java\n"
      + "    .\n"
      + "  System Properties:\n"
      + "      awt.toolkit = sun.lwawt.macosx.LWCToolkit\n"
      + "      file.encoding = UTF-8\n"
      + "      file.encoding.pkg = sun.io\n"
      + "      file.separator = /\n"
      + "      ftp.nonProxyHosts = local|*.local|169.254/16|*.169.254/16\n"
      + "      gemfire.launcher.registerSignalHandlers = true\n"
      + "      gemfire.locators = localhost[10101]\n"
      + "      gemfire.security-manager = io.pivotal.support.MySecurityManager\n"
      + "      gemfire.security-password = ********\n"
      + "      gemfire.security-username = superUser\n"
      + "      gemfire.start-dev-rest-api = false\n"
      + "      gemfire.use-cluster-configuration = true\n"
      + "      user.country = US\n"
      + "      user.language = en\n"
      + "      user.timezone = Europe/Dublin\n"
      + "  Log4J 2 Configuration:\n"
      + "      jar:file:/Users/bwayne/Applications/GemFire/9.4.0/pivotal-gemfire-9.4.0/lib/geode-core-9.4.0.jar!/log4j2.xml\n"
      + "  ---------------------------------------------------------------------------";

  private Properties expectedProperties8x;
  private Properties expectedProperties9x;

  @Rule
  public TimeZoneRule timeZoneRule = new TimeZoneRule(ZoneId.of("Europe/Dublin"));

  @Before
  public void setUp() {
    super.setUp();
    logParser = spy(new Log4jParser(filesService));

    expectedProperties8x = new Properties();
    expectedProperties8x.setProperty("Locator.forceLocatorDMType", "true");
    expectedProperties8x.setProperty("awt.toolkit", "sun.lwawt.macosx.LWCToolkit");
    expectedProperties8x.setProperty("file.encoding", "UTF-8");
    expectedProperties8x.setProperty("file.encoding.pkg", "sun.io");
    expectedProperties8x.setProperty("file.separator", "/");
    expectedProperties8x.setProperty("ftp.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties8x.setProperty("gemfire.enable-cluster-configuration", "true");
    expectedProperties8x.setProperty("gemfire.launcher.registerSignalHandlers", "true");
    expectedProperties8x.setProperty("gemfire.load-cluster-configuration-from-dir", "false");
    expectedProperties8x.setProperty("user.country", "US");
    expectedProperties8x.setProperty("user.language", "en");
    expectedProperties8x.setProperty("user.timezone", "Europe/Dublin");

    expectedProperties9x = new Properties();
    expectedProperties9x.setProperty("awt.toolkit", "sun.lwawt.macosx.LWCToolkit");
    expectedProperties9x.setProperty("file.encoding", "UTF-8");
    expectedProperties9x.setProperty("file.encoding.pkg", "sun.io");
    expectedProperties9x.setProperty("file.separator", "/");
    expectedProperties9x.setProperty("ftp.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties9x.setProperty("gemfire.launcher.registerSignalHandlers", "true");
    expectedProperties9x.setProperty("gemfire.locators", "localhost[10101]");
    expectedProperties9x.setProperty("gemfire.security-manager", "io.pivotal.support.MySecurityManager");
    expectedProperties9x.setProperty("gemfire.security-password", "********");
    expectedProperties9x.setProperty("gemfire.security-username", "superUser");
    expectedProperties9x.setProperty("gemfire.start-dev-rest-api", "false");
    expectedProperties9x.setProperty("gemfire.use-cluster-configuration", "true");
    expectedProperties9x.setProperty("user.country", "US");
    expectedProperties9x.setProperty("user.language", "en");
    expectedProperties9x.setProperty("user.timezone", "Europe/Dublin");
  }

  @Test
  public void buildMetadataWithIntervalShouldThrowExceptionIfLineCanNotBeParsed() {
    assertThatThrownBy(() -> logParser.buildMetadataWithIntervalOnly(mock(Path.class), "StartLine", "")).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");
    assertThatThrownBy(() -> logParser.buildMetadataWithIntervalOnly(mock(Path.class), "[info 2018/08/24 16:07:57.863 IST locator1 <main> tid=0xe] DistributionManager stopped in 111ms.", "FinishLine")).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");
  }

  @Test
  public void buildMetadataWithIntervalShouldReturnTheLogMetadataOnlyWithTheIntervalCovered() {
    String startLine = "[info 2018/04/17 15:19:48.658 IST server1 <main> tid=0x1] Startup Configuration:";
    String finishLine = "[info 2018/04/17 15:20:45.610 IST server1 <pool-3-thread-1> tid=0x4e] Marking DistributionManager 192.168.1.7(server1:32310)<v1>:1025 as closed.";

    LogMetadata logMetadata = logParser.buildMetadataWithIntervalOnly(MockUtils.mockPath("fileName", false), startLine, finishLine);
    assertThat(logMetadata).isNotNull();
    assertThat(logMetadata.getFileName()).isEqualTo("fileName");
    assertThat(logMetadata.getStartTimeStamp()).isEqualTo(1523974788658L);
    assertThat(logMetadata.getFinishTimeStamp()).isEqualTo(1523974845610L);
    assertThat(logMetadata.getSystemProperties()).isNull();
    assertThat(logMetadata.getProductVersion()).isNull();
    assertThat(logMetadata.getOperatingSystem()).isNull();
  }

  @Test
  public void parseProductVersionShouldWorkCorrectly() {
    Log4jParser log4jParser = (Log4jParser) logParser;

    assertThat(log4jParser.parseProductVersion("")).isNull();
    assertThat(log4jParser.parseProductVersion(header8XSample)).isEqualTo("8.2.0");
    assertThat(log4jParser.parseProductVersion(header9XSample)).isEqualTo("9.4.0");
  }

  @Test
  public void parseOperatingSystemShouldWorkCorrectly() {
    Log4jParser log4jParser = (Log4jParser) logParser;

    assertThat(log4jParser.parseOperatingSystem("")).isNull();
    assertThat(log4jParser.parseOperatingSystem(header8XSample)).isEqualTo("x86_64 Mac OS X 10.13.6");
    assertThat(log4jParser.parseOperatingSystem(header9XSample)).isEqualTo("amd64 Linux 3.10.0-862.11.6.el7.x86_64");
  }

  @Test
  public void parseSystemPropertiesShouldWorkCorrectly() throws IOException {
    Log4jParser log4jParser = (Log4jParser) logParser;

    assertThat(log4jParser.parseSystemProperties("")).isNotNull().isEmpty();
    assertThat(log4jParser.parseSystemProperties(header8XSample)).isEqualTo(expectedProperties8x);
    assertThat(log4jParser.parseSystemProperties(header9XSample)).isEqualTo(expectedProperties9x);
  }

  @Test
  public void buildMetadataShouldPropagateExceptionsThrownByBuildMetadataWithIntervalOnly() {
    assertThatThrownBy(() -> logParser.buildMetadata(mock(Path.class), "StartLine", "")).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");
    assertThatThrownBy(() -> logParser.buildMetadata(mock(Path.class), "[info 2018/08/24 16:07:57.863 IST locator1 <main> tid=0xe] DistributionManager stopped in 111ms.", "FinishLine")).isInstanceOf(IllegalArgumentException.class).hasMessage("Log format not recognized.");
  }

  @Test
  public void buildMetadataShouldReturnOnlyTheIntervalCoveredIfNoExtraMetadataIsFound() throws IOException {
    doReturn(Collections.emptyList()).when((Log4jParser) logParser).parseFile(any());
    String startLine = "[info 2018/04/17 15:19:48.658 IST server1 <main> tid=0x1] Startup Configuration:";
    String finishLine = "[info 2018/04/17 15:20:45.610 IST server1 <pool-3-thread-1> tid=0x4e] Marking DistributionManager 192.168.1.7(server1:32310)<v1>:1025 as closed.";

    LogMetadata logMetadata = logParser.buildMetadata(MockUtils.mockPath("fileName", false), startLine, finishLine);
    assertThat(logMetadata).isNotNull();
    assertThat(logMetadata.getFileName()).isEqualTo("fileName");
    assertThat(logMetadata.getStartTimeStamp()).isEqualTo(1523974788658L);
    assertThat(logMetadata.getFinishTimeStamp()).isEqualTo(1523974845610L);
    assertThat(logMetadata.getSystemProperties()).isNull();
    assertThat(logMetadata.getProductVersion()).isNull();
    assertThat(logMetadata.getOperatingSystem()).isNull();
  }

  @Test
  public void buildMetadataShouldReturnFullLogMetadata() throws IOException {
    String startLine = "[info 2018/04/17 15:19:48.658 IST server1 <main> tid=0x1] Startup Configuration:";
    String finishLine = "[info 2018/04/17 15:20:45.610 IST server1 <pool-3-thread-1> tid=0x4e] Marking DistributionManager 192.168.1.7(server1:32310)<v1>:1025 as closed.";

    LoggingEvent mockLoggingEvent8X = mock(LoggingEvent.class);
    when(mockLoggingEvent8X.getMessage()).thenReturn(header8XSample);
    doReturn(Collections.singletonList(mockLoggingEvent8X)).when((Log4jParser) logParser).parseFile(any());
    LogMetadata logMetadata8X = logParser.buildMetadata(MockUtils.mockPath("fileName", false), startLine, finishLine);
    assertThat(logMetadata8X).isNotNull();
    assertThat(logMetadata8X.getFileName()).isEqualTo("fileName");
    assertThat(logMetadata8X.getStartTimeStamp()).isEqualTo(1523974788658L);
    assertThat(logMetadata8X.getFinishTimeStamp()).isEqualTo(1523974845610L);
    assertThat(logMetadata8X.getSystemProperties()).isEqualTo(expectedProperties8x);
    assertThat(logMetadata8X.getProductVersion()).isEqualTo("8.2.0");
    assertThat(logMetadata8X.getOperatingSystem()).isEqualTo("x86_64 Mac OS X 10.13.6");

    LoggingEvent mockLoggingEvent9X = mock(LoggingEvent.class);
    when(mockLoggingEvent9X.getMessage()).thenReturn(header9XSample);
    doReturn(Collections.singletonList(mockLoggingEvent9X)).when((Log4jParser) logParser).parseFile(any());
    LogMetadata logMetadata9X = logParser.buildMetadata(MockUtils.mockPath("fileName", false), startLine, finishLine);
    assertThat(logMetadata9X).isNotNull();
    assertThat(logMetadata9X.getFileName()).isEqualTo("fileName");
    assertThat(logMetadata9X.getStartTimeStamp()).isEqualTo(1523974788658L);
    assertThat(logMetadata9X.getFinishTimeStamp()).isEqualTo(1523974845610L);
    assertThat(logMetadata9X.getSystemProperties()).isEqualTo(expectedProperties9x);
    assertThat(logMetadata9X.getProductVersion()).isEqualTo("9.4.0");
    assertThat(logMetadata9X.getOperatingSystem()).isEqualTo("amd64 Linux 3.10.0-862.11.6.el7.x86_64");
  }
}
