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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class IntervalTest {
  private ZoneId systemZoneId = ZoneId.systemDefault();
  private Instant noon, oneHourBeforeNoon, oneHourAfterNoon;
  private Interval noonInterval, todayInterval, thisMonthInterval;
  private ZonedDateTime startOfToday, finishOfToday, startOfMonth, finishOfMonth;

  @Before
  public void setUp() {
    startOfToday = ZonedDateTime.now().with(LocalTime.MIN);
    finishOfToday = ZonedDateTime.now().with(LocalTime.MAX);
    noon = ZonedDateTime.now().with(LocalTime.NOON).toInstant();
    oneHourAfterNoon = noon.plus(1, ChronoUnit.HOURS);
    oneHourBeforeNoon = noon.minus(1, ChronoUnit.HOURS);
    startOfMonth = ZonedDateTime.now().with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
    finishOfMonth = ZonedDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

    todayInterval = Interval.of(startOfToday, finishOfToday);
    thisMonthInterval = Interval.of(startOfMonth, finishOfMonth);
    noonInterval = Interval.of(systemZoneId, oneHourBeforeNoon, oneHourAfterNoon);
  }

  @Test
  public void ofStartTimeFinishTimeTest() {
    assertThatThrownBy(() -> Interval.of(null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("StartTime can't be null");

    assertThatThrownBy(() -> Interval.of(startOfToday, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("FinishTime can't be null");

    assertThatThrownBy(() -> Interval.of(finishOfToday, startOfToday))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("StartTime should be a point of time before FinishTime.");

    assertThatThrownBy(() -> Interval.of(startOfToday.withZoneSameInstant(ZoneId.of("America/Chicago")), finishOfToday.withZoneSameInstant(ZoneId.of("Europe/Rome"))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("StartTime and FinishTime should belong to the same ZoneId.");

    Interval interval = Interval.of(startOfToday, finishOfToday);
    assertThat(interval.getStart()).isEqualTo(startOfToday);
    assertThat(interval.getFinish()).isEqualTo(finishOfToday);
  }

  @Test
  public void ofZoneIdStartInstantFinishInstantTest() {
    assertThatThrownBy(() -> Interval.of(null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("ZoneId can't be null");

    assertThatThrownBy(() -> Interval.of(systemZoneId, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("StartInstant can't be null");

    assertThatThrownBy(() -> Interval.of(systemZoneId, oneHourBeforeNoon, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("FinishInstant can't be null");

    assertThatThrownBy(() -> Interval.of(systemZoneId, oneHourAfterNoon, oneHourBeforeNoon))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageMatching("StartInstant should be a point of time before FinishInstant.");

    Interval interval = Interval.of(systemZoneId, oneHourBeforeNoon, oneHourAfterNoon);
    assertThat(interval.getStart()).isEqualTo(ZonedDateTime.ofInstant(oneHourBeforeNoon, systemZoneId));
    assertThat(interval.getFinish()).isEqualTo(ZonedDateTime.ofInstant(oneHourAfterNoon, systemZoneId));
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai", "America/Chicago" })
  public void withZoneSameIntervalTest(String timeZoneId) {
    assertThatThrownBy(() -> noonInterval.withZoneSameInterval(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("ZoneId can't be null");

    ZoneId newZoneId = StringUtils.isBlank(timeZoneId) ? systemZoneId : ZoneId.of(timeZoneId);

    Interval newNoonInterval = noonInterval.withZoneSameInterval(newZoneId);
    assertThat(noonInterval.getStart().toInstant()).isEqualTo(newNoonInterval.getStart().toInstant());
    assertThat(noonInterval.getFinish().toInstant()).isEqualTo(newNoonInterval.getFinish().toInstant());

    Interval newTodayInterval = todayInterval.withZoneSameInterval(newZoneId);
    assertThat(todayInterval.getStart().toInstant()).isEqualTo(newTodayInterval.getStart().toInstant());
    assertThat(todayInterval.getFinish().toInstant()).isEqualTo(newTodayInterval.getFinish().toInstant());

    Interval newMonthInterval = thisMonthInterval.withZoneSameInterval(newZoneId);
    assertThat(thisMonthInterval.getStart().toInstant()).isEqualTo(newMonthInterval.getStart().toInstant());
    assertThat(thisMonthInterval.getFinish().toInstant()).isEqualTo(newMonthInterval.getFinish().toInstant());
  }

  @Test
  public void containsShouldThrownExceptionWhenZonedDateTimeParameterIsNull() {
    ZonedDateTime zonedDateTime = null;

    assertThatThrownBy(() -> noonInterval.contains(zonedDateTime))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("DateTime can't be null.");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai", "America/Chicago" })
  public void containsShouldWorkCorrectlyWhenUsingDifferentTimeZones(String timeZoneId) {
    ZoneId filterZoneId = StringUtils.isBlank(timeZoneId) ? systemZoneId : ZoneId.of(timeZoneId);

    assertThat(todayInterval.contains(ZonedDateTime.ofInstant(noon, filterZoneId))).isTrue();
    assertThat(todayInterval.contains(ZonedDateTime.ofInstant(oneHourBeforeNoon, filterZoneId))).isTrue();
    assertThat(todayInterval.contains(ZonedDateTime.ofInstant(oneHourAfterNoon, filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(startOfToday)).isTrue();
    assertThat(thisMonthInterval.contains(finishOfToday)).isTrue();
    assertThat(thisMonthInterval.contains(startOfMonth)).isTrue();
    assertThat(thisMonthInterval.contains(finishOfMonth)).isTrue();
    assertThat(thisMonthInterval.contains(ZonedDateTime.ofInstant(noon, filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(ZonedDateTime.ofInstant(oneHourBeforeNoon, filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(ZonedDateTime.ofInstant(oneHourAfterNoon, filterZoneId))).isTrue();
    assertThat(noonInterval.contains(ZonedDateTime.ofInstant(noon, filterZoneId))).isTrue();
    assertThat(noonInterval.contains(ZonedDateTime.ofInstant(oneHourBeforeNoon, filterZoneId))).isTrue();
    assertThat(noonInterval.contains(ZonedDateTime.ofInstant(oneHourAfterNoon, filterZoneId))).isTrue();

    assertThat(todayInterval.contains(startOfToday.minusSeconds(1).withZoneSameInstant(filterZoneId))).isFalse();
    assertThat(todayInterval.contains(finishOfToday.plusSeconds(1).withZoneSameInstant(filterZoneId))).isFalse();
    assertThat(thisMonthInterval.contains(startOfMonth.minusSeconds(1).withZoneSameInstant(filterZoneId))).isFalse();
    assertThat(thisMonthInterval.contains(finishOfMonth.plusSeconds(1).withZoneSameInstant(filterZoneId))).isFalse();
    assertThat(noonInterval
        .contains(ZonedDateTime.ofInstant(oneHourBeforeNoon.minusSeconds(1), filterZoneId))).isFalse();
    assertThat(
        noonInterval.contains(ZonedDateTime.ofInstant(oneHourAfterNoon.plusSeconds(1), filterZoneId))).isFalse();


  }

  @Test
  public void containsShouldThrownExceptionWhenIntervalParameterIsNull() {
    Interval interval = null;

    assertThatThrownBy(() -> todayInterval.contains(interval))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("Interval can't be null.");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai", "America/Chicago" })
  public void containsIntervalShouldWorkCorrectlyWhenUsingDifferentTimeZones(String timeZoneId) {
    ZoneId filterZoneId = StringUtils.isBlank(timeZoneId) ? systemZoneId : ZoneId.of(timeZoneId);

    // Minutes Within Hours
    assertThat(noonInterval.contains(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(noonInterval.contains(Interval.of(filterZoneId, oneHourBeforeNoon, noon))).isTrue();
    assertThat(noonInterval.contains(Interval.of(filterZoneId, noon, oneHourAfterNoon))).isTrue();
    assertThat(noonInterval.contains(Interval.of(filterZoneId, oneHourBeforeNoon.plusSeconds(1), oneHourAfterNoon.minusSeconds(1)))).isTrue();
    assertThat(noonInterval.contains(todayInterval.withZoneSameInterval(filterZoneId))).isFalse();
    assertThat(noonInterval.contains(thisMonthInterval.withZoneSameInterval(filterZoneId))).isFalse();
    assertThat(noonInterval.contains(Interval.of(filterZoneId, oneHourBeforeNoon.minusSeconds(1), oneHourAfterNoon.plusSeconds(1)))).isFalse();

    // Hours Within a Day
    assertThat(todayInterval.contains(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(todayInterval.contains(todayInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, oneHourBeforeNoon, oneHourAfterNoon))).isTrue();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, noon, finishOfToday.toInstant()))).isTrue();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, startOfToday.toInstant(), noon))).isTrue();
    assertThat(todayInterval.contains(thisMonthInterval.withZoneSameInterval(filterZoneId))).isFalse();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, startOfToday.minusSeconds(1).toInstant(), noon))).isFalse();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, noon, finishOfToday.plusSeconds(1).toInstant()))).isFalse();
    assertThat(todayInterval.contains(Interval.of(filterZoneId, startOfToday.minusSeconds(1).toInstant(), finishOfToday.plusSeconds(1).toInstant()))).isFalse();

    // Days Within a Month
    assertThat(thisMonthInterval.contains(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(todayInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(thisMonthInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, oneHourBeforeNoon, oneHourAfterNoon))).isTrue();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, startOfToday.toInstant(), finishOfToday.toInstant()))).isTrue();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, startOfMonth.plusSeconds(1).toInstant(), finishOfMonth.minusSeconds(1).toInstant()))).isTrue();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, startOfMonth.minusSeconds(1).toInstant(), noon))).isFalse();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, noon, finishOfMonth.plusSeconds(1).toInstant()))).isFalse();
    assertThat(thisMonthInterval.contains(Interval.of(filterZoneId, startOfMonth.minusSeconds(1).toInstant(), finishOfMonth.plusSeconds(1).toInstant()))).isFalse();
  }

  @Test
  public void overlapsShouldThrownExceptionWhenParameterIsNull() {
    Interval dateTimeInterval = Interval.of(ZoneId.systemDefault(), Instant.now(), Instant.now());

    assertThatThrownBy(() -> dateTimeInterval.overlaps(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageMatching("Interval can't be null.");
  }

  @Test
  @Parameters({ "", "Australia/Sydney", "America/Argentina/Buenos_Aires", "Asia/Shanghai", "America/Chicago" })
  public void overlapsShouldWorkCorrectlyWhenUsingDifferentTimeZones(String timeZoneId) {
    ZoneId filterZoneId = StringUtils.isBlank(timeZoneId) ? systemZoneId : ZoneId.of(timeZoneId);

    // Interval of Hours
    assertThat(noonInterval.overlaps(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(noonInterval.overlaps(todayInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(noonInterval.overlaps(thisMonthInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, oneHourBeforeNoon.minusSeconds(3600), noon))).isTrue();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, oneHourBeforeNoon.minusSeconds(3600), oneHourBeforeNoon))).isTrue();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, oneHourAfterNoon, oneHourAfterNoon.plusSeconds(3600)))).isTrue();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, oneHourAfterNoon.plusSeconds(1), finishOfToday.toInstant()))).isFalse();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, startOfToday.toInstant(), oneHourBeforeNoon.minusSeconds(1)))).isFalse();
    assertThat(noonInterval.overlaps(Interval.of(filterZoneId, startOfToday.minusDays(1).toInstant(), finishOfToday.minusDays(1).toInstant()))).isFalse();

    // Entire Day as The Interval
    assertThat(todayInterval.overlaps(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(todayInterval.overlaps(todayInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(todayInterval.overlaps(thisMonthInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, startOfToday.toInstant(), noon))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, startOfMonth.toInstant(), noon))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, noon, finishOfToday.toInstant()))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, noon, finishOfMonth.toInstant()))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, noon.minus(1, ChronoUnit.DAYS), noon))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, noon, noon.plus(1, ChronoUnit.DAYS)))).isTrue();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, startOfMonth.toInstant(), startOfToday.minusSeconds(1).toInstant()))).isFalse();
    assertThat(todayInterval.overlaps(Interval.of(filterZoneId, finishOfToday.plusSeconds(1).toInstant(), finishOfMonth.toInstant()))).isFalse();

    // Entire Month as The Interval
    assertThat(thisMonthInterval.overlaps(noonInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.overlaps(todayInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.overlaps(thisMonthInterval.withZoneSameInterval(filterZoneId))).isTrue();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, startOfToday.minusMonths(1).toInstant(), noon))).isTrue();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, noon, finishOfToday.plusMonths(1).toInstant()))).isTrue();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, startOfMonth.minusDays(1).toInstant(), startOfToday.toInstant()))).isTrue();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, finishOfToday.toInstant(), finishOfMonth.plusDays(1).toInstant()))).isTrue();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, startOfMonth.minusYears(1).toInstant(), startOfMonth.minusSeconds(1).toInstant()))).isFalse();
    assertThat(thisMonthInterval.overlaps(Interval.of(filterZoneId, finishOfMonth.plusSeconds(1).toInstant(), finishOfMonth.plusYears(1).toInstant()))).isFalse();
  }
}
