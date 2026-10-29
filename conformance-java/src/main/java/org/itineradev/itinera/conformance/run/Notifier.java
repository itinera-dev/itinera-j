package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.policy.Role;

/** The role the cases call {@code notifier}, whose operations the scenario scripts. */
@Role
interface Notifier {
  /** Performs {@code operation} of the role. */
  void perform(String operation);
}
