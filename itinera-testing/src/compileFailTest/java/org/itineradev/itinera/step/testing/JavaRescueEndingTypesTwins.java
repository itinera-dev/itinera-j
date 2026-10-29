package org.itineradev.itinera.step.testing;

/** What {@link JavaRescueEndingTypesTest} refuses, written so that it compiles. */
final class JavaRescueEndingTypesTwins {
  RescueStepMock rescue =
      RescueStepMock.playing(RescueEnding.success(), RescueEnding.failure("stolen"));
}
