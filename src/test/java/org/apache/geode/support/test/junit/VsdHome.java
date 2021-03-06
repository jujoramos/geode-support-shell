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
package org.apache.geode.support.test.junit;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;

import org.apache.geode.support.command.statistics.StartVisualStatisticsDisplayCommand;

/**
 * Helper class to check whether to run or ignore a test that depends on the external VSD tool.
 */
public class VsdHome {
  private String vsdHome;

  public VsdHome() {
    vsdHome = System.getenv().get(StartVisualStatisticsDisplayCommand.VSD_HOME_KEY);
  }

  public String getVsdHome() {
    return vsdHome;
  }

  public void exists() {
    Assume.assumeTrue("VSD_HOME should be set to run this test.", StringUtils.isNoneEmpty(vsdHome) && Files.exists(Paths.get(vsdHome)));
  }
}
