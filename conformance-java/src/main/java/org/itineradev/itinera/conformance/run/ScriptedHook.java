package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.Hook;
import org.itineradev.itinera.conformance.model.HookAction;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.HookScript;
import org.itineradev.itinera.conformance.model.ValueType;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.policy.HookReporter;
import org.itineradev.itinera.request.ContributorKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.HookKind;
import org.itineradev.itinera.request.HookReporterKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.java.JavaRequests;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.Sync;

/**
 * A hook of a policy as its script says, whose requests are of kind {@code K}: what it requests, in
 * the order written, then its contributor and its hook reporter, and what it does before it ends.
 */
final class ScriptedHook<K extends HookKind<Sync>> {
  private final String policy;
  private final Hook hook;
  private final HookScript script;
  private final Map<HookRequest, Request<? super K, ?>> requests = new LinkedHashMap<>();
  private final Request<ContributorKind, Contributor> contributor = Requests.contributor();
  private final Request<HookReporterKind<Sync>, HookReporter> reporter = Requests.hookReporter();

  ScriptedHook(String policy, Hook hook, HookScript script) {
    this.policy = policy;
    this.hook = hook;
    this.script = script;
    for (var request : script.requests()) {
      requests.put(request, ofKind(requestOf(request)));
    }
  }

  /** Everything the hook requests. */
  List<Request<? super K, ?>> declared() {
    var declared = new ArrayList<Request<? super K, ?>>(requests.values());
    declared.add(contributor);
    declared.add(reporter);
    return declared;
  }

  /** What the hook does before it ends, in the order written. */
  List<HookAction> actions() {
    return script.actions();
  }

  /** What {@code got} gives the hook for one call, which {@code witness} records. */
  HookReceived receivedFrom(Got<K> got, Witness witness) {
    var requested = new LinkedHashMap<HookRequest, Object>();
    for (var request : requests.entrySet()) {
      requested.put(request.getKey(), Providing.answerOf(got.get(request.getValue())));
    }
    var received = Collections.unmodifiableMap(requested);
    witness.hookCalled(new HookCall(policy, hook, received));
    return new HookReceived(received, got.get(contributor), got.get(reporter));
  }

  /** {@code request}, for a hook whose requests are of kind {@code K}, which the case trusts. */
  @SuppressWarnings("unchecked")
  private static <K> Request<? super K, ?> ofKind(Request<?, ?> request) {
    return (Request<? super K, ?>) request;
  }

  private static Request<?, ?> requestOf(HookRequest request) {
    if (request instanceof HookRequest.StepData data) {
      return data.optional()
          ? JavaRequests.optionalFromStep(data.key(), tokenOf(data.type()))
          : Requests.fromStep(data.key(), tokenOf(data.type()));
    }
    if (request instanceof HookRequest.WorkflowData data) {
      return data.optional()
          ? JavaRequests.optionalFromWorkflow(data.key(), tokenOf(data.type()))
          : Requests.fromWorkflow(data.key(), tokenOf(data.type()));
    }
    if (request instanceof HookRequest.FailureReason reason) {
      return reason.optional() ? JavaRequests.optionalReason() : Requests.reason();
    }
    if (request instanceof HookRequest.Role role) {
      return Requests.role(Roles.roleOf(role.role()));
    }
    return factOf(request);
  }

  private static Request<?, ?> factOf(HookRequest request) {
    if (request instanceof HookRequest.Error) {
      return Requests.error();
    }
    if (request instanceof HookRequest.FailureCause) {
      return Requests.failureCause();
    }
    if (request instanceof HookRequest.RetryCause) {
      return Requests.retryCause();
    }
    if (request instanceof HookRequest.StepName) {
      return Requests.stepName();
    }
    if (request instanceof HookRequest.Attempt) {
      return Requests.attempt();
    }
    return Requests.journeyId();
  }

  private static TypeToken<?> tokenOf(ValueType type) {
    return TypeToken.of(type.type());
  }
}
