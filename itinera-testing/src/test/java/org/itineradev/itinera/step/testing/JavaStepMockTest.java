package org.itineradev.itinera.step.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.AbortReason;
import org.itineradev.itinera.report.testing.RecordingReporter;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.workflow.StepDescriptor;
import org.itineradev.itinera.workflow.WorkflowDescriptor;
import org.junit.jupiter.api.Test;

final class JavaStepMockTest {
  static final class Shop {}

  @Test
  void aStepMockPlaysItsScriptInAJavaWorkflow() throws Refusal {
    var charge = StepMock.playing(Ending.abnormalTermination("timeout"), Ending.success());
    var orders =
        WorkflowDescriptor.builder(Shop.class, "orders")
            .step(StepDescriptor.builder("charge", charge).retryBudget(1).build())
            .build();
    var reporter = new RecordingReporter();
    var result =
        new LocalExecutor().run(InstanceBuilder.of(orders, new Shop()).reporter(reporter).create());
    assertTrue(result.isSucceeded());
    assertEquals(2, charge.getBuilds());
    assertEquals(
        List.of(
            "charge attempt 1 terminated abnormally: timeout",
            "charge retrying",
            "charge attempt 2 succeeded",
            "journey succeeded"),
        reporter.getPath());
  }

  @Test
  void anEndingGivesTheOutcomeItStandsForInJava() {
    assertEquals(Outcome.success(), Ending.success().outcome());
    assertEquals(Outcome.failure("declined"), Ending.failure("declined").outcome());
    assertEquals(Outcome.skipped(), Ending.skipped().outcome());
  }

  @Test
  void aStepMockFailingToBuildAbortsAJavaWorkflow() throws Refusal {
    var orders =
        WorkflowDescriptor.builder(Shop.class, "orders")
            .step(StepDescriptor.builder("charge", StepMock.failingToBuild("no gateway")).build())
            .build();
    var result = new LocalExecutor().run(InstanceBuilder.of(orders, new Shop()).create());
    assertEquals(AbortReason.STEP_COULD_NOT_BE_BUILT, result.abort().reason());
    assertEquals("no gateway", result.abort().errorMessage());
  }
}
