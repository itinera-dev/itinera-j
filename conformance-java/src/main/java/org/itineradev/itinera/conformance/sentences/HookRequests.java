package org.itineradev.itinera.conformance.sentences;

import java.util.function.Predicate;
import org.itineradev.itinera.conformance.model.HookRequest;

/** Which of a hook's requests a sentence reads what it received for. */
final class HookRequests {
  private HookRequests() {}

  static Predicate<HookRequest> stepData(String key) {
    return request -> request instanceof HookRequest.StepData data && data.key().equals(key);
  }

  static Predicate<HookRequest> workflowData(String key) {
    return request -> request instanceof HookRequest.WorkflowData data && data.key().equals(key);
  }

  static boolean isStepName(HookRequest request) {
    return request instanceof HookRequest.StepName;
  }

  static boolean isAttempt(HookRequest request) {
    return request instanceof HookRequest.Attempt;
  }

  static boolean isJourneyId(HookRequest request) {
    return request instanceof HookRequest.JourneyId;
  }

  static boolean isError(HookRequest request) {
    return request instanceof HookRequest.Error;
  }

  static boolean isReason(HookRequest request) {
    return request instanceof HookRequest.FailureReason;
  }

  static boolean isCause(HookRequest request) {
    return request instanceof HookRequest.FailureCause || request instanceof HookRequest.RetryCause;
  }
}
