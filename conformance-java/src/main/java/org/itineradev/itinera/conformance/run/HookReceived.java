package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.policy.HookReporter;

/**
 * What a scripted hook was given for one call.
 *
 * @param requested what it requested, by request, absent as {@code null}
 * @param contributor its contributor
 * @param reporter its hook reporter
 */
record HookReceived(
    Map<HookRequest, Object> requested, Contributor contributor, HookReporter reporter) {
  /** The step data it received under {@code key}. */
  Object stepData(String key) {
    return requested.entrySet().stream()
        .filter(entry -> isStepData(entry.getKey(), key))
        .findFirst()
        .map(Map.Entry::getValue)
        .orElse(null);
  }

  /** The role it received as {@code role}. */
  Notifier role(String role) {
    if (requested.get(new HookRequest.Role(role)) instanceof Notifier notifier) {
      return notifier;
    }
    throw new ModelException("the hook calls the role " + role + " it did not request");
  }

  private static boolean isStepData(HookRequest request, String key) {
    return request instanceof HookRequest.StepData data && data.key().equals(key);
  }
}
