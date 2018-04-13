package org.apache.geode.support.domain.marker;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method from the original Geode should be improved.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD})
public @interface GeodeImprovement {

  String reason() default  "";
}
