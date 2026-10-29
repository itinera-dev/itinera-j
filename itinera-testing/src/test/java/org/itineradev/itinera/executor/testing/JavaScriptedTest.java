package org.itineradev.itinera.executor.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.report.testing.Level;
import org.itineradev.itinera.report.testing.RecordingReporter;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.SyncStepRun;
import org.itineradev.itinera.testing.Mistake;
import org.itineradev.itinera.testing.TestMistake;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.Sync;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaScriptedTest {
  static final class Shop {}

  private static final TypeToken<Long> LONG = TypeToken.of(Long.class);

  private static WorkflowDescriptor<Shop, Sync> orders() {
    return WorkflowDescriptor.builder(Shop.class, "orders")
        .step(
            StepDescriptor.builder("charge", got -> (SyncStepRun) Outcome::success)
                .request(Requests.contributor())
                .request(Requests.stepReporter())
                .retryBudget(1)
                .build())
        .step(
            StepDescriptor.builder("ship", got -> (SyncStepRun) Outcome::success)
                .request(Requests.input("charged", LONG))
                .build())
        .build();
  }

  @Test
  void aJavaScriptedRunPlaysItsScriptAndRecordsItsSteps() throws Refusal {
    var reporter = new RecordingReporter();
    var run =
        Scripted.scripted(InstanceBuilder.of(orders(), new Shop()).reporter(reporter).create())
            .step("charge")
            .willFail("declined")
            .onAttempt(2)
            .willContribute("charged", 35L)
            .willEmit(Level.INFO, "charged")
            .willSucceed()
            .andStep("ship")
            .willRunForReal()
            .ensureAllScripted()
            .run();
    assertEquals(
        List.of(
            "charge attempt 1 failed: declined",
            "charge retrying",
            "charge attempt 2 succeeded",
            "ship attempt 1 succeeded",
            "journey succeeded"),
        reporter.getPath());
    assertEquals(35L, run.step("ship").attempt(1).input("charged", LONG).orNull());
    assertNull(run.step("ship").attempt(2));
  }

  @Test
  void aJavaTestReadsEachProblemOfAMistake() {
    var script = Scripted.scripted(InstanceBuilder.of(orders(), new Shop()).create());
    var mistake =
        assertThrows(TestMistake.class, () -> script.step("charge").willFail("declined").run());
    var problem = assertInstanceOf(Mistake.AttemptLeftOut.class, mistake.getProblems().get(0));
    assertEquals("charge", problem.step());
    assertEquals(2, problem.attempt());
  }
}
