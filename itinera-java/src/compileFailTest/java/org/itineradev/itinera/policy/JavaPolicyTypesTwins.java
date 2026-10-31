package org.itineradev.itinera.policy;

import java.util.List;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.workflow.WorkflowDescriptor;

/** What {@link JavaPolicyTypesTest} refuses, written so that it compiles. */
final class JavaPolicyTypesTwins {
  @Role
  interface Messages {
    void tell(String message);
  }

  static final class Corner implements Messages {
    public void tell(String message) {}
  }

  static final class Audit {}

  void declare() {
    var audit = StepPolicyDescriptor.builder("audit", Audit::new);
    audit.onStepFailure((policy, got) -> OnFailure.keepDefault());
    audit.onStepSuccess(List.of(Requests.journeyId()), (policy, got) -> OnSuccess.keepDefault());
    WorkflowDescriptor.builder(Corner.class, "corner").role(Messages.class);
  }
}
