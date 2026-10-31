package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.conformance.model.ModelException;

/** The interface of each role the cases name. */
final class Roles {
  private static final Map<String, Class<Notifier>> ROLES = Map.of("notifier", Notifier.class);

  private Roles() {}

  /** The interface of the role the cases call {@code name}. */
  static Class<Notifier> roleOf(String name) {
    var role = ROLES.get(name);
    if (role == null) {
      throw new ModelException(name + " is not a role of the cases");
    }
    return role;
  }
}
