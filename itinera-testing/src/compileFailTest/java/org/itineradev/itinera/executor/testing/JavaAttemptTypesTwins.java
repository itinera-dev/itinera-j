package org.itineradev.itinera.executor.testing;

/** What {@link JavaAttemptTypesTest} refuses, written so that it compiles. */
final class JavaAttemptTypesTwins {
  AttemptScript<StepScript> chosen(Script script) {
    return script.step("charge").onAttempt(2);
  }

  AttemptRecord read(StepRecord record) {
    return record.attempt(2);
  }
}
