package org.itineradev.itinera.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.Instant;
import java.util.Set;
import org.itineradev.itinera.journey.AbortReason;
import org.itineradev.itinera.journey.FailureCause;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.PolicyName;
import org.itineradev.itinera.policy.StepHook;
import org.itineradev.itinera.policy.WorkflowHook;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Reason;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.WorkflowName;
import org.junit.jupiter.api.Test;

class JavaEventsTest {
  private static final Instant NOW = Instant.parse("2026-10-27T18:00:00Z");
  private static final JourneyId JOURNEY = JourneyId.of("order-42");
  private static final WorkflowName ORDERS = WorkflowName.of("orders");
  private static final StepName CHARGE = StepName.of("charge");

  @Test
  void anEventIsARecordJavaReadsThroughItsAccessors() {
    Event event =
        new StepFailed(1L, NOW, JOURNEY, ORDERS, CHARGE, Attempt.of(2), new Reason("declined"));
    StepFailed failed = assertInstanceOf(StepFailed.class, event);
    assertEquals(EventKind.STEP_FAILED, failed.kind());
    assertEquals("charge", failed.step().toString());
    assertEquals(2, failed.attempt().getNumber());
    assertEquals("declined", failed.reason().code());
    assertNull(failed.reason().message());
  }

  @Test
  void aKindReadsBackFromItsName() {
    assertSame(EventKind.JOURNEY_STARTED, EventKind.parse("journey_started"));
  }

  @Test
  void aJourneyStartedEventListsTheKeysOfTheInitialData() {
    assertEquals(
        Set.of("amount"),
        new JourneyStarted(1L, NOW, JOURNEY, ORDERS, Set.of("amount")).initialKeys());
  }

  @Test
  void everyRecordReadsItsKindAndItsAbortReasonThroughItsOwnAccessors() {
    var called =
        new WorkflowHookCalled(
            1L,
            NOW,
            JOURNEY,
            ORDERS,
            new ByWorkflowHook(PolicyName.of("audit"), WorkflowHook.ON_WORKFLOW_SUCCESS));
    assertEquals(EventKind.HOOK_CALLED, called.kind());
    var refused = new NotAValue.UnderKey(null, ByStep.INSTANCE, "ticket", "no more tickets");
    assertEquals(AbortReason.NOT_A_VALUE, refused.reason());
    assertEquals("no more tickets", refused.errorMessage());
  }

  @Test
  void aDecisionByDefaultNamesTheDefault() {
    StepGivenUp givenUp =
        new StepGivenUp(
            3L, NOW, JOURNEY, ORDERS, CHARGE, Attempt.of(1), GiveUpCause.Failure.INSTANCE);
    assertSame(DecidedBy.Default.INSTANCE, givenUp.decidedBy());
    assertEquals(FailureCause.FAILURE, givenUp.cause().getCause());
  }

  @Test
  void anAbortNamesItsReasonAndWhoCausedIt() {
    JourneyAbort abort =
        new WrongType(
            CHARGE,
            new WrongData.Key(
                "amount", new ByStepHook(PolicyName.of("audit"), StepHook.ON_STEP_RETRY)),
            null);
    assertEquals(AbortReason.WRONG_TYPE, abort.getReason());
    assertEquals("wrong type", abort.getReason().toString());
    assertSame(ByStep.INSTANCE, new ReservedMetadataKey(CHARGE, ByStep.INSTANCE).source());
  }
}
