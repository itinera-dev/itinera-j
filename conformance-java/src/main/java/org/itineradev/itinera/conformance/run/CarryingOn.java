package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.List;

/**
 * The calls made on a reporter lent to a step or a hook run alone, in order. A call that is
 * interrupted is recorded so, and the step or hook carries on, as a test would let it.
 */
final class CarryingOn {
  private final List<Call> made = new ArrayList<>();

  /** Every call made, in order. */
  List<Call> calls() {
    return List.copyOf(made);
  }

  /**
   * Makes {@code call}, the call of {@code method} with {@code key}, recording it and carrying on.
   */
  void carriedOn(String method, String key, Runnable call) {
    boolean interrupted;
    try {
      call.run();
      interrupted = false;
    } catch (RuntimeException interruption) {
      interrupted = true;
    }
    made.add(new Call(method, key, interrupted));
  }
}
