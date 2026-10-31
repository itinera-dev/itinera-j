package org.itineradev.itinera.conformance.run;

import java.util.Set;
import org.itineradev.itinera.conformance.model.RoleOperation;

/**
 * The workflow class of every scenario, which declares nothing: the builder puts each scenario's
 * workflow together on it. It provides every role of the cases, whose operations the witness
 * counts, failing those among the failing operations.
 */
final class Scenario implements Notifier {
  private final Witness witness;
  private final Set<RoleOperation> failing;

  Scenario(Witness witness, Set<RoleOperation> failing) {
    this.witness = witness;
    this.failing = Set.copyOf(failing);
  }

  @Override
  public void perform(String operation) {
    witness.roleCalled("notifier", operation);
    if (failing.contains(new RoleOperation("notifier", operation))) {
      throw new IllegalStateException(
          "the operation " + operation + " of the role notifier failed");
    }
  }
}
