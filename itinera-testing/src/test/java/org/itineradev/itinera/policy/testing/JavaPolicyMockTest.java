package org.itineradev.itinera.policy.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.itineradev.itinera.event.PolicyCouldNotBeBuilt;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyResult;
import org.itineradev.itinera.policy.OnFailure;
import org.itineradev.itinera.policy.PolicyName;
import org.itineradev.itinera.policy.StepPolicyDescriptor;
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.step.testing.Ending;
import org.itineradev.itinera.step.testing.StepMock;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowBuilder;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaPolicyMockTest {
  static final class Shop {}

  @Test
  void aStepPolicyMockEndsItsHooksAsItsScriptSaysInAJavaWorkflow() throws Refusal {
    var script = StepPolicyScript.keepingDefault().onStepSuccess(HookEnding.failWorkflow("fraud"));
    var audit =
        StepPolicyDescriptor.builder("audit", script)
            .onStepSuccess((policy, got) -> policy.onStepSuccess())
            .build();
    var result = ran(orders().step(charge().policy(audit).build()));
    assertEquals(FailureCause.FAIL_WORKFLOW, result.failure().cause());
    assertEquals("fraud", result.failure().reason().code());
  }

  @Test
  void aFailureHookEndingGivesWhatItStandsForInJava() {
    var mock =
        StepPolicyScript.keepingDefault().onStepRetry(HookEnding.failWorkflow("declined")).build();
    assertEquals(
        List.of(OnFailure.keepDefault(), OnFailure.failWorkflow("declined")),
        List.of(mock.onStepFailure(), mock.onStepRetry()));
  }

  @Test
  void aWorkflowPolicyMockScriptedToFailThrowsItsMessageInJava() {
    var mock =
        WorkflowPolicyScript.returning().onWorkflowFailure(HookEnding.fails("no mail")).build();
    var thrown = assertThrows(RuntimeException.class, mock::onWorkflowFailure);
    assertEquals("no mail", thrown.getMessage());
  }

  @Test
  void aStepPolicyScriptFailingToBuildAbortsAJavaWorkflow() throws Refusal {
    var audit =
        StepPolicyDescriptor.builder(
                "audit", StepPolicyScript.keepingDefault().failingToBuild("no ledger"))
            .onStepSuccess((policy, got) -> policy.onStepSuccess())
            .build();
    var result = ran(orders().step(charge().policy(audit).build()));
    assertEquals(
        new PolicyCouldNotBeBuilt(StepName.of("charge"), PolicyName.of("audit"), "no ledger"),
        result.abort().details());
  }

  @Test
  void aWorkflowPolicyMockRunsInAJavaWorkflow() throws Refusal {
    var notify =
        WorkflowPolicyDescriptor.builder("notify", WorkflowPolicyScript.returning())
            .onWorkflowSuccess((policy, got) -> policy.onWorkflowSuccess())
            .build();
    assertTrue(ran(orders().step(charge().build()).policy(notify)).isSucceeded());
  }

  private static WorkflowBuilder<Shop, Sync> orders() {
    return WorkflowDescriptor.builder(Shop.class, "orders");
  }

  private static StepBuilder<Sync> charge() {
    return StepDescriptor.builder("charge", StepMock.playing(Ending.success()));
  }

  private static JourneyResult ran(WorkflowBuilder<Shop, Sync> orders) throws Refusal {
    return new LocalExecutor().run(InstanceBuilder.of(orders.build(), new Shop()).create());
  }
}
