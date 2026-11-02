package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.StepAction;
import org.itineradev.itinera.conformance.model.Values;
import org.itineradev.itinera.step.StepReporter;

/** How a scripted step does what its script says, with the handles it received. */
final class Acting {
  private Acting() {}

  /**
   * Does what {@code action} says with the contributor and the step reporter {@code received}
   * holds, catching what a call on the reporter throws when the step {@code ignoresEmitFailures}.
   */
  static void perform(StepAction action, Received received, boolean ignoresEmitFailures) {
    var reporter = received.reporter();
    if (action instanceof StepAction.Contributes contributes) {
      received.contributor().contribute(contributes.key(), contributes.value());
    } else if (action instanceof StepAction.Emits emits) {
      reporting(ignoresEmitFailures, () -> emit(reporter, emits));
    } else if (action instanceof StepAction.SetsGlobalMetadata sets) {
      reporting(ignoresEmitFailures, () -> reporter.setGlobalMetadata(sets.key(), sets.value()));
    } else {
      var changes = (StepAction.ContributesThenChanges) action;
      var contributed = Values.value(changes.written());
      received.contributor().contribute(changes.key(), contributed);
      Scripted.changeInPlace(contributed, changes.changed());
    }
  }

  /** Makes {@code call}, catching every exception it throws when {@code ignoring} its failures. */
  private static void reporting(boolean ignoring, Runnable call) {
    try {
      call.run();
    } catch (RuntimeException failure) {
      if (!ignoring) {
        throw failure;
      }
    }
  }

  private static void emit(StepReporter reporter, StepAction.Emits emits) {
    switch (emits.kind()) {
      case STEP_INFO -> reporter.info(emits.message(), emits.data());
      case STEP_WARNING -> reporter.warning(emits.message(), emits.data());
      case STEP_ERROR -> reporter.error(emits.message(), emits.data());
    }
  }
}
