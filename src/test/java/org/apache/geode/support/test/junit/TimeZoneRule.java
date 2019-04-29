package org.apache.geode.support.test.junit;

import java.time.ZoneId;
import java.util.TimeZone;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Changes TimeZone to the specified one before the test, restores the default one afterwards.
 */
public class TimeZoneRule implements TestRule {
  private final ZoneId zoneId;

  public TimeZoneRule(ZoneId zoneId) {
    this.zoneId = zoneId;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        TimeZone defaultTimeZone = TimeZone.getDefault();

        try {
          TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
          base.evaluate();
        } finally {
          TimeZone.setDefault(defaultTimeZone);
        }
      }
    };
  }
}
