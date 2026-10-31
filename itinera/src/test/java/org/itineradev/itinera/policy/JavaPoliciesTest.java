package org.itineradev.itinera.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.ContributorKind;
import org.itineradev.itinera.request.FromStepKind;
import org.itineradev.itinera.request.JourneyIdKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.RoleKind;
import org.itineradev.itinera.request.StepHookKind;
import org.itineradev.itinera.request.WorkflowHookKind;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaPoliciesTest {
  @Role
  interface Messages {
    void tell(String message);
  }

  static final class Shop implements Messages {
    final List<String> told = new ArrayList<>();

    @Override
    public void tell(String message) {
      told.add(message);
    }
  }

  static final class Audit {
    OnSuccess charged(long amount, Contributor contributor) {
      contributor.contribute("audit", "charged " + amount);
      return amount > 1000 ? OnSuccess.failWorkflow("over the limit") : OnSuccess.keepDefault();
    }
  }

  static final Request<FromStepKind, Long> CHARGED =
      Requests.fromStep("charged", TypeToken.of(Long.class));
  static final Request<ContributorKind, Contributor> CONTRIBUTOR = Requests.contributor();
  static final Request<JourneyIdKind, JourneyId> JOURNEY = Requests.journeyId();
  static final Request<RoleKind, Messages> MESSAGES = Requests.role(Messages.class);

  @Test
  void javaBuildsPoliciesWithLambdasAndAttachesThem() {
    StepPolicyDescriptor<Audit, Sync> audit =
        StepPolicyDescriptor.builder("audit", Audit::new)
            .onStepSuccess(
                List.of(CHARGED, CONTRIBUTOR),
                (policy, got) -> policy.charged(got.get(CHARGED), got.get(CONTRIBUTOR)))
            .build();
    WorkflowPolicyDescriptor<Object, Sync> notify =
        WorkflowPolicyDescriptor.builder("notify", Object::new)
            .onWorkflowSuccess(
                List.of(JOURNEY, MESSAGES),
                (policy, got) -> got.get(MESSAGES).tell("order " + got.get(JOURNEY) + " is paid"))
            .build();
    WorkflowDescriptor<Shop, Sync> shop =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(StepDescriptor.builder("charge", got -> Outcome::success).policy(audit).build())
            .role(Messages.class)
            .policy(notify)
            .build();
    assertSame(audit, shop.getSteps().get(0).getPolicies().get(0));
    assertEquals(List.of(notify), shop.getPolicies());
    assertEquals(List.of(Messages.class), shop.getRoles());
  }

  @Test
  void aJavaHookReadsEachRequestAsItsOwnType() {
    StepPolicyDescriptor<Audit, Sync> audit =
        StepPolicyDescriptor.builder("audit", Audit::new)
            .onStepSuccess(
                List.of(CHARGED, CONTRIBUTOR),
                (policy, got) -> policy.charged(got.get(CHARGED), got.get(CONTRIBUTOR)))
            .build();
    List<String> contributed = new ArrayList<>();
    SyncHook<Audit, StepHookKind<Sync>, OnSuccess> hook =
        (SyncHook<Audit, StepHookKind<Sync>, OnSuccess>) audit.getOnStepSuccess().getCall();
    OnSuccess decided =
        hook.call(
            audit.getFactory().build(),
            new Answers<>(
                Map.of(
                    CHARGED,
                    4000L,
                    CONTRIBUTOR,
                    (Contributor) (key, value) -> contributed.add(key))));
    assertEquals(new Lifecycle.FailWorkflow(new Reason("over the limit")), decided);
    assertEquals(List.of("audit"), contributed);
  }

  @Test
  void javaSwitchesOverWhatAHookReturnedThroughItsProperties() {
    assertEquals(
        List.of("default", "declined"),
        List.of(OnFailure.keepDefault(), OnFailure.failWorkflow("declined")).stream()
            .map(JavaPoliciesTest::said)
            .toList());
  }

  private static String said(OnFailure decided) {
    return decided.getKeepsDefault() ? "default" : decided.getFailsWorkflow().code();
  }

  @Test
  void aJavaWorkflowHookCallsItsRole() {
    Shop shop = new Shop();
    SyncWorkflowHook<Object, WorkflowHookKind<Sync>> notify =
        (policy, got) -> got.get(MESSAGES).tell("paid");
    notify.call(new Object(), new Answers<>(Map.of(MESSAGES, shop)));
    assertEquals(List.of("paid"), shop.told);
  }
}
