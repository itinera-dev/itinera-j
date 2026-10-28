package org.itineradev.itinera.workflow;

import org.itineradev.itinera.step.RescueOutcome;

/** What {@link JavaRescueTypesTest} refuses, written so that it compiles. */
final class JavaRescueTypesTwins {
  RescueStepBuilder<Sync> rescue() {
    return RescueStepDescriptor.builder(got -> () -> RescueOutcome.success());
  }
}
