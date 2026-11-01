package org.itineradev.itinera.conformance.run;

import java.util.Map;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookRequest;

/**
 * One call of a scripted hook.
 *
 * @param policy the policy
 * @param hook the hook
 * @param received what it received for each request, absent as {@code null}
 */
public record HookCall(String policy, Hook hook, Map<HookRequest, Object> received) {}
