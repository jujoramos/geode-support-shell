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
package org.apache.geode.support.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Properties;

import org.apache.geode.support.domain.logs.LogMetadata;
import org.apache.geode.support.utils.FormatUtils;

/**
 * Class used in integration tests to store and assert the hardcoded contents of the logs files within the samples directory.
 *
 * TODO: This should be removed once the tool has the ability to generate statistics files on its own, without using already created files for testing.
 */
public final class LogsSampleDataUtils {
  public static final File rootFolder = new File(LogsSampleDataUtils.class.getResource("/samples/logs").getFile());
  public static final File parseableFolder = rootFolder.toPath().resolve("parseable").toFile();
  public static final File unparseableFolder = rootFolder.toPath().resolve("unparseable").toFile();
  public static final Path noHeaderLogPath = parseableFolder.toPath().resolve("noHeader.log");
  public static final Path member8XLogPath = parseableFolder.toPath().resolve("member_8X.log");
  public static final Path member9XLogPath = parseableFolder.toPath().resolve("member_9X.log");
  public static final Path unknownLogPath = unparseableFolder.toPath().resolve("unknownFormat.log");
  private static final LogMetadata noHeader_Metadata;
  private static final LogMetadata member8X_Metadata;
  private static final LogMetadata member9X_Metadata;

  static {
    Properties expectedProperties8x = new Properties();
    expectedProperties8x.setProperty("Locator.forceLocatorDMType", "true");
    expectedProperties8x.setProperty("awt.toolkit", "sun.lwawt.macosx.LWCToolkit");
    expectedProperties8x.setProperty("file.encoding", "UTF-8");
    expectedProperties8x.setProperty("file.encoding.pkg", "sun.io");
    expectedProperties8x.setProperty("file.separator", "/");
    expectedProperties8x.setProperty("ftp.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties8x.setProperty("gemfire.enable-cluster-configuration", "true");
    expectedProperties8x.setProperty("gemfire.launcher.registerSignalHandlers", "true");
    expectedProperties8x.setProperty("gemfire.load-cluster-configuration-from-dir", "false");
    expectedProperties8x.setProperty("gopherProxySet", "false");
    expectedProperties8x.setProperty("http.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties8x.setProperty("java.awt.graphicsenv", "sun.awt.CGraphicsEnvironment");
    expectedProperties8x.setProperty("java.awt.headless", "true");
    expectedProperties8x.setProperty("java.awt.printerjob", "sun.lwawt.macosx.CPrinterJob");
    expectedProperties8x.setProperty("java.class.version", "52.0");
    expectedProperties8x.setProperty("java.endorsed.dirs", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/endorsed");
    expectedProperties8x.setProperty("java.ext.dirs", "/Users/bwayne/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java");
    expectedProperties8x.setProperty("java.home", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre");
    expectedProperties8x.setProperty("java.io.tmpdir", "/var/folders/h5/lpqkmjfn7mv6fkgp9gdhf8dh0000gq/T/");
    expectedProperties8x.setProperty("java.runtime.name", "Java(TM) SE Runtime Environment");
    expectedProperties8x.setProperty("java.runtime.version", "1.8.0_152-b16");
    expectedProperties8x.setProperty("java.specification.name", "Java Platform API Specification");
    expectedProperties8x.setProperty("java.specification.vendor", "Oracle Corporation");
    expectedProperties8x.setProperty("java.specification.version", "1.8");
    expectedProperties8x.setProperty("java.vendor", "Oracle Corporation");
    expectedProperties8x.setProperty("java.vendor.url", "http://java.oracle.com/");
    expectedProperties8x.setProperty("java.vendor.url.bug", "http://bugreport.sun.com/bugreport/");
    expectedProperties8x.setProperty("java.version", "1.8.0_152");
    expectedProperties8x.setProperty("java.vm.info", "mixed mode");
    expectedProperties8x.setProperty("java.vm.name", "Java HotSpot(TM) 64-Bit Server VM");
    expectedProperties8x.setProperty("java.vm.specification.name", "Java Virtual Machine Specification");
    expectedProperties8x.setProperty("java.vm.specification.vendor", "Oracle Corporation");
    expectedProperties8x.setProperty("java.vm.specification.version", "1.8");
    expectedProperties8x.setProperty("java.vm.vendor", "Oracle Corporation");
    expectedProperties8x.setProperty("java.vm.version", "25.152-b16");
    expectedProperties8x.setProperty("jna.platform.library.path", "/usr/lib:/usr/lib");
    expectedProperties8x.setProperty("jnidispatch.path", "/var/folders/h5/lpqkmjfn7mv6fkgp9gdhf8dh0000gq/T/jna--1151997864/jna8782806641865284006.tmp");
    expectedProperties8x.setProperty("line.separator", "");
    expectedProperties8x.setProperty("log4j.configurationFile", "jar:file:/Users/bwayne/Applications/GemFire/8.2.0/Pivotal_GemFire_820_b17919_Linux/lib/gemfire.jar!/com/gemstone/gemfire/internal/logging/log4j/log4j2-default.xml");
    expectedProperties8x.setProperty("os.version", "10.13.6");
    expectedProperties8x.setProperty("path.separator", ":");
    expectedProperties8x.setProperty("socksNonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties8x.setProperty("sun.arch.data.model", "64");
    expectedProperties8x.setProperty("sun.boot.class.path", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/sunrsasign.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/classes");
    expectedProperties8x.setProperty("sun.boot.library.path", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib");
    expectedProperties8x.setProperty("sun.cpu.endian", "little");
    expectedProperties8x.setProperty("sun.cpu.isalist", "");
    expectedProperties8x.setProperty("sun.io.unicode.encoding", "UnicodeBig");
    expectedProperties8x.setProperty("sun.java.command", "com.gemstone.gemfire.distributed.LocatorLauncher start locator1 --port=10334");
    expectedProperties8x.setProperty("sun.java.launcher", "SUN_STANDARD");
    expectedProperties8x.setProperty("sun.jnu.encoding", "UTF-8");
    expectedProperties8x.setProperty("sun.management.compiler", "HotSpot 64-Bit Tiered Compilers");
    expectedProperties8x.setProperty("sun.os.patch.level", "unknown");
    expectedProperties8x.setProperty("sun.rmi.dgc.server.gcInterval", "9223372036854775806");
    expectedProperties8x.setProperty("user.country", "US");
    expectedProperties8x.setProperty("user.language", "en");
    expectedProperties8x.setProperty("user.timezone", "Europe/Dublin");

    Properties expectedProperties9x = new Properties();
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
    expectedProperties9x.setProperty("gopherProxySet", "false");
    expectedProperties9x.setProperty("http.nonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties9x.setProperty("java.awt.graphicsenv", "sun.awt.CGraphicsEnvironment");
    expectedProperties9x.setProperty("java.awt.headless", "true");
    expectedProperties9x.setProperty("java.awt.printerjob", "sun.lwawt.macosx.CPrinterJob");
    expectedProperties9x.setProperty("java.class.version", "52.0");
    expectedProperties9x.setProperty("java.endorsed.dirs", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/endorsed");
    expectedProperties9x.setProperty("java.ext.dirs", "/Users/bwayne/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java");
    expectedProperties9x.setProperty("java.home", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre");
    expectedProperties9x.setProperty("java.io.tmpdir", "/var/folders/h5/lpqkmjfn7mv6fkgp9gdhf8dh0000gq/T/");
    expectedProperties9x.setProperty("java.runtime.name", "Java(TM) SE Runtime Environment");
    expectedProperties9x.setProperty("java.runtime.version", "1.8.0_152-b16");
    expectedProperties9x.setProperty("java.specification.name", "Java Platform API Specification");
    expectedProperties9x.setProperty("java.specification.vendor", "Oracle Corporation");
    expectedProperties9x.setProperty("java.specification.version", "1.8");
    expectedProperties9x.setProperty("java.vendor", "Oracle Corporation");
    expectedProperties9x.setProperty("java.vendor.url", "http://java.oracle.com/");
    expectedProperties9x.setProperty("java.vendor.url.bug", "http://bugreport.sun.com/bugreport/");
    expectedProperties9x.setProperty("java.version", "1.8.0_152");
    expectedProperties9x.setProperty("java.vm.info", "mixed mode");
    expectedProperties9x.setProperty("java.vm.name", "Java HotSpot(TM) 64-Bit Server VM");
    expectedProperties9x.setProperty("java.vm.specification.name", "Java Virtual Machine Specification");
    expectedProperties9x.setProperty("java.vm.specification.vendor", "Oracle Corporation");
    expectedProperties9x.setProperty("java.vm.specification.version", "1.8");
    expectedProperties9x.setProperty("java.vm.vendor", "Oracle Corporation");
    expectedProperties9x.setProperty("java.vm.version", "25.152-b16");
    expectedProperties9x.setProperty("jna.platform.library.path", "/usr/lib:/usr/lib");
    expectedProperties9x.setProperty("jnidispatch.path", "/var/folders/h5/lpqkmjfn7mv6fkgp9gdhf8dh0000gq/T/jna--1151997864/jna7005621900414300440.tmp");
    expectedProperties9x.setProperty("line.separator", "");
    expectedProperties9x.setProperty("os.version", "10.13.4");
    expectedProperties9x.setProperty("path.separator", ":");
    expectedProperties9x.setProperty("socksNonProxyHosts", "local|*.local|169.254/16|*.169.254/16");
    expectedProperties9x.setProperty("sun.arch.data.model", "64");
    expectedProperties9x.setProperty("sun.boot.class.path", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/resources.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/rt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/sunrsasign.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jsse.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jce.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/charsets.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib/jfr.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/classes");
    expectedProperties9x.setProperty("sun.boot.library.path", "/Library/Java/JavaVirtualMachines/jdk1.8.0_152.jdk/Contents/Home/jre/lib");
    expectedProperties9x.setProperty("sun.cpu.endian", "little");
    expectedProperties9x.setProperty("sun.cpu.isalist", "");
    expectedProperties9x.setProperty("sun.io.unicode.encoding", "UnicodeBig");
    expectedProperties9x.setProperty("sun.java.command", "org.apache.geode.distributed.ServerLauncher start server1 --server-port=40404");
    expectedProperties9x.setProperty("sun.java.launcher", "SUN_STANDARD");
    expectedProperties9x.setProperty("sun.jnu.encoding", "UTF-8");
    expectedProperties9x.setProperty("sun.management.compiler", "HotSpot 64-Bit Tiered Compilers");
    expectedProperties9x.setProperty("sun.nio.ch.bugLevel", "");
    expectedProperties9x.setProperty("sun.os.patch.level", "unknown");
    expectedProperties9x.setProperty("sun.rmi.dgc.server.gcInterval", "9223372036854775806");
    expectedProperties9x.setProperty("user.country", "US");
    expectedProperties9x.setProperty("user.language", "en");
    expectedProperties9x.setProperty("user.timezone", "America/Buenos_Aires");

    String basePath = parseableFolder.getAbsolutePath() + File.separator;
    noHeader_Metadata = LogMetadata.of(basePath + "noHeader.log", null, 1536195800179L, 1536203534347L, null, null, null);
    member8X_Metadata = LogMetadata.of(basePath + "member_8X.log", ZoneId.of("Europe/Dublin"), 1535122364384L, 1535123277866L, "8.2.0", "x86_64 Mac OS X 10.13.6", expectedProperties8x);
    member9X_Metadata = LogMetadata.of(basePath + "member_9X.log", ZoneId.of("America/Buenos_Aires"), 1523953188658L, 1523953245610L, "9.4.0", "amd64 Linux 3.10.0-862.11.6.el7.x86_64", expectedProperties9x);
  }

  private static void assertFullMetadata(LogMetadata actualMetadata, LogMetadata expectedMetadata) {
    assertThat(actualMetadata.getFileName()).isEqualTo(expectedMetadata.getFileName());
    assertThat(actualMetadata.getStartTimeStamp()).isEqualTo(expectedMetadata.getStartTimeStamp());
    assertThat(actualMetadata.getFinishTimeStamp()).isEqualTo(expectedMetadata.getFinishTimeStamp());
    assertThat(actualMetadata.getProductVersion()).isEqualTo(expectedMetadata.getProductVersion());
    assertThat(actualMetadata.getOperatingSystem()).isEqualTo(expectedMetadata.getOperatingSystem());
    assertThat(actualMetadata.getSystemProperties()).isEqualTo(expectedMetadata.getSystemProperties());
  }

  private static void assertFullMetadata(LogMetadata metadata, Path basePath, ZoneId zoneId, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    ZoneId formatZoneId = zoneId != null ? zoneId : (metadata.getTimeZoneId() != null) ? metadata.getTimeZoneId() : ZoneId.systemDefault();
    Instant startInstant = Instant.ofEpochMilli(metadata.getStartTimeStamp());
    Instant finishInstant = Instant.ofEpochMilli(metadata.getFinishTimeStamp());
    ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formatZoneId);
    ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formatZoneId);

    String expectedFileName = FormatUtils.relativizePath(basePath, new File(metadata.getFileName()).toPath());
    String expectedProductVersion = FormatUtils.trimProductVersion(metadata.getProductVersion());
    assertThat(fileName).isEqualTo(expectedFileName);
    assertThat(productVersion).isEqualTo(expectedProductVersion);
    assertThat(operatingSystem).isEqualTo(metadata.getOperatingSystem());
    assertThat(timeZoneId).isEqualTo(metadata.getTimeZoneId().toString());
    assertThat(startTimeStamp).isEqualTo(startTime.format(FormatUtils.getDateTimeFormatter()));
    assertThat(finishTimeStamp).isEqualTo(finishTime.format(FormatUtils.getDateTimeFormatter()));
  }

  private static void assertIntervalOnly(LogMetadata actualMetadata, LogMetadata expectedMetadata) {
    assertThat(actualMetadata.getFileName()).isEqualTo(expectedMetadata.getFileName());
    assertThat(actualMetadata.getProductVersion()).isNull();
    assertThat(actualMetadata.getOperatingSystem()).isNull();
    assertThat(actualMetadata.getSystemProperties()).isNull();
    assertThat(actualMetadata.getStartTimeStamp()).isEqualTo(expectedMetadata.getStartTimeStamp());
    assertThat(actualMetadata.getFinishTimeStamp()).isEqualTo(expectedMetadata.getFinishTimeStamp());
  }

  private static void assertIntervalOnly(LogMetadata metadata, Path basePath, ZoneId zoneId, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    ZoneId formatZoneId = zoneId != null ? zoneId : ZoneId.systemDefault();
    Instant startInstant = Instant.ofEpochMilli(metadata.getStartTimeStamp());
    Instant finishInstant = Instant.ofEpochMilli(metadata.getFinishTimeStamp());
    ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, formatZoneId);
    ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, formatZoneId);

    String expectedFileName = FormatUtils.relativizePath(basePath, new File(metadata.getFileName()).toPath());
    assertThat(fileName).isEqualTo(expectedFileName);
    assertThat(productVersion).isEqualTo("");
    assertThat(operatingSystem).isEqualTo("");
    assertThat(timeZoneId).isEqualTo("");
    assertThat(startTimeStamp).isEqualTo(startTime.format(FormatUtils.getDateTimeFormatter()));
    assertThat(finishTimeStamp).isEqualTo(finishTime.format(FormatUtils.getDateTimeFormatter()));
  }

  public static void assertNoHeaderMetadata(LogMetadata metadata) {
    assertIntervalOnly(metadata, noHeader_Metadata);
  }

  public static void assertNoHeaderMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp) {
    assertIntervalOnly(noHeader_Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertMember8XMetadata(LogMetadata actualMetadata, boolean intervalOnly) {
    if (intervalOnly) assertIntervalOnly(actualMetadata, member8X_Metadata);
    else assertFullMetadata(actualMetadata, member8X_Metadata);
  }

  public static void assertMember8XMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp, boolean intervalOnly) {
    if (intervalOnly) assertIntervalOnly(member8X_Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
    else assertFullMetadata(member8X_Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }

  public static void assertMember9XMetadata(LogMetadata actualMetadata, boolean intervalOnly) {
    if (intervalOnly) assertIntervalOnly(actualMetadata, member9X_Metadata);
    else assertFullMetadata(actualMetadata, member9X_Metadata);
  }

  public static void assertMember9XMetadata(Path basePath, ZoneId formatTimeZone, String fileName, String productVersion, String operatingSystem, String timeZoneId, String startTimeStamp, String finishTimeStamp, boolean intervalOnly) {
    if (intervalOnly) assertIntervalOnly(member9X_Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
    else assertFullMetadata(member9X_Metadata, basePath, formatTimeZone, fileName, productVersion, operatingSystem, timeZoneId, startTimeStamp, finishTimeStamp);
  }
}
