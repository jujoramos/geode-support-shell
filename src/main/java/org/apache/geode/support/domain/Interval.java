package org.apache.geode.support.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

public class Interval {
  private final ZoneId zoneId;
  private final ZonedDateTime startTime;
  private final ZonedDateTime finishTime;

  public ZonedDateTime getStart() {
    return startTime;
  }

  public ZonedDateTime getFinish() {
    return finishTime;
  }

  private Interval(ZoneId zoneId, ZonedDateTime startTime, ZonedDateTime finishTime) {
    this.zoneId = zoneId;
    this.startTime = startTime;
    this.finishTime = finishTime;
  }

  public Interval withZoneSameInterval(ZoneId zoneId) {
    Objects.requireNonNull(zoneId, "ZoneId can't be null");
    if (zoneId.equals(this.zoneId)) return this;

    ZonedDateTime newStartTime = startTime.withZoneSameInstant(zoneId);
    ZonedDateTime newFinishTime = finishTime.withZoneSameInstant(zoneId);

    return new Interval(zoneId, newStartTime, newFinishTime);
  }

  public static Interval of(ZonedDateTime startTime, ZonedDateTime finishTime) {
    Objects.requireNonNull(startTime, "StartTime can't be null");
    Objects.requireNonNull(finishTime, "FinishTime can't be null");

    if (startTime.isAfter(finishTime))
      throw new IllegalArgumentException("StartTime should be a point of time before FinishTime.");

    if (!startTime.getZone().equals(finishTime.getZone()))
      throw new IllegalArgumentException("StartTime and FinishTime should belong to the same ZoneId.");

    return new Interval(startTime.getZone(), startTime, finishTime);
  }

  public static Interval of(ZoneId zoneId, Instant startInstant, Instant finishInstant) {
    Objects.requireNonNull(zoneId, "ZoneId can't be null");
    Objects.requireNonNull(startInstant, "StartInstant can't be null");
    Objects.requireNonNull(finishInstant, "FinishInstant can't be null");

    if (startInstant.isAfter(finishInstant))
      throw new IllegalArgumentException("StartInstant should be a point of time before FinishInstant.");

    ZonedDateTime startTime = ZonedDateTime.ofInstant(startInstant, zoneId);
    ZonedDateTime finishTime = ZonedDateTime.ofInstant(finishInstant, zoneId);

    return new Interval(zoneId, startTime, finishTime);
  }

  public boolean contains(ZonedDateTime dateTime) {
    Objects.requireNonNull(dateTime, "DateTime can't be null.");
    ZonedDateTime filterDateTime = dateTime;

    // Use the same ZoneId to execute the comparision.
    if (!zoneId.equals(dateTime.getZone())) {
      filterDateTime = dateTime.withZoneSameInstant(startTime.getZone());
    }

    return filterDateTime.compareTo(startTime) >= 0 && filterDateTime.compareTo(finishTime) <= 0;
  }

  public boolean contains(Interval interval) {
    Objects.requireNonNull(interval, "Interval can't be null.");
    Interval other = interval;

    // Use the same ZoneId to execute the comparision.
    if (!zoneId.equals(interval.zoneId)) {
      other = new Interval(zoneId, interval.startTime.withZoneSameInstant(zoneId), interval.finishTime.withZoneSameInstant(zoneId));
    }

    return other.startTime.compareTo(startTime) >= 0 && other.finishTime.compareTo(finishTime) <= 0;
  }

  public boolean overlaps(Interval interval) {
    Objects.requireNonNull(interval, "Interval can't be null.");
    Interval other = interval;

    // Use the same ZoneId to execute the comparision.
    if (!zoneId.equals(interval.zoneId)) {
      other = new Interval(zoneId, interval.startTime.withZoneSameInstant(zoneId), interval.finishTime.withZoneSameInstant(zoneId));
    }

    return contains(other) ||
        (startTime.compareTo(other.finishTime) <= 0 && other.startTime.compareTo(finishTime) <= 0);
  }
}
