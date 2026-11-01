package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.HookAction;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.policy.HookReporter;

/** How a scripted hook does what its script says, with what it received. */
final class HookActing {
  private HookActing() {}

  /**
   * Does what {@code action} says with what {@code received} holds, on the policy {@code played},
   * reaching kept handles through {@code witness}.
   */
  static void perform(HookAction action, HookReceived received, Played<?> played, Witness witness) {
    var reporter = received.reporter();
    if (action instanceof HookAction.Contributes contributes) {
      received.contributor().contribute(contributes.key(), contributes.value());
    } else if (action instanceof HookAction.CountsCalls counts) {
      received.contributor().contribute(counts.key(), played.counted());
    } else if (action instanceof HookAction.ChangesStepData changes) {
      Scripted.changeInPlace(received.stepData(changes.key()), changes.value());
    } else if (action instanceof HookAction.Emits emits) {
      emit(reporter, emits);
    } else if (action instanceof HookAction.SetsGlobalMetadata sets) {
      reporter.setGlobalMetadata(sets.key(), sets.value());
    } else if (action instanceof HookAction.CallsRole calls) {
      received.role(calls.role()).perform(calls.operation());
    } else {
      useKept((HookAction.UsesKeptHandles) action, witness);
    }
  }

  private static void useKept(HookAction.UsesKeptHandles uses, Witness witness) {
    var kept = keptBy(uses.step(), witness);
    ignoringFailures(() -> kept.contributor().contribute(uses.key(), uses.value()));
    ignoringFailures(() -> kept.reporter().info(uses.message()));
  }

  /** What the last attempt of {@code step} received, whose handles it kept. */
  private static Received keptBy(String step, Witness witness) {
    var built = witness.built(step);
    if (built.isEmpty()) {
      throw new ModelException("\"" + step + "\" was never built, so it kept no handles");
    }
    return built.get(built.size() - 1).received();
  }

  /** Makes {@code call} on a handle whose attempt has ended, which throws, and carries on. */
  private static void ignoringFailures(Runnable call) {
    try {
      call.run();
    } catch (IllegalStateException ended) {
    }
  }

  private static void emit(HookReporter reporter, HookAction.Emits emits) {
    switch (emits.kind()) {
      case JOURNEY_INFO -> reporter.info(emits.message());
      case JOURNEY_WARNING -> reporter.warning(emits.message());
      case JOURNEY_ERROR -> reporter.error(emits.message());
    }
  }
}
