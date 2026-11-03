package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyInfo;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.instance.InstanceBuilder;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.HookReporter;
import org.itineradev.itinera.policy.ItineraStepPolicy;
import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
import org.itineradev.itinera.policy.OnStepSuccess;
import org.itineradev.itinera.policy.OnSuccess;
import org.itineradev.itinera.policy.OnWorkflowSuccess;
import org.itineradev.itinera.policy.PolicyName;
import org.itineradev.itinera.policy.StepPolicy;
import org.itineradev.itinera.policy.StepPolicyDescriptor;
import org.itineradev.itinera.policy.WorkflowPolicy;
import org.itineradev.itinera.policy.WorkflowPolicyDescriptor;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraRescueStep;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.RescueOutcome;
import org.itineradev.itinera.step.RescueStep;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaPartClassesTest {
  @Step
  public static final class Charge implements ItineraStep {
    private final String card;
    private final long amount;

    public Charge(@Input("card") String card, @Input("amount") long amount) {
      this.card = card;
      this.amount = amount;
    }

    public Outcome run(Contributor contributor) {
      if (!card.endsWith("renewed")) return fail("declined");
      contributor.contribute("charged", amount);
      return succeed();
    }
  }

  @RescueStep
  public static final class Renew implements ItineraRescueStep {
    private final String card;

    public Renew(@Input("card") String card) {
      this.card = card;
    }

    public RescueOutcome run(Contributor contributor) {
      contributor.contribute("card", card + " renewed");
      return succeed();
    }
  }

  @StepPolicy(name = "audit")
  public static final class Audit implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged(@FromStep("charged") long charged, Contributor contributor) {
      contributor.contribute("audit", "charged " + charged);
      return keepDefault();
    }
  }

  @WorkflowPolicy(name = "ledger")
  public static final class Ledger implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid(@FromWorkflow("charged") long charged, HookReporter reporter) {
      reporter.info("paid " + charged);
    }
  }

  public static final class Shop {}

  public record Receipt(String card, long charged, String audit) {}

  public static final class UnmarkedStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  public static final class UnmarkedRescue implements ItineraRescueStep {
    public RescueOutcome run() {
      return succeed();
    }
  }

  public static final class UnmarkedStepPolicy implements ItineraStepPolicy {
    @OnStepSuccess
    public OnSuccess charged() {
      return keepDefault();
    }
  }

  public static final class UnmarkedWorkflowPolicy implements ItineraWorkflowPolicy {
    @OnWorkflowSuccess
    public void paid() {}
  }

  @Test
  void aStepDescribedAloneTakesItsNameAndRetryBudgetFromTheBuilder() {
    var charge = StepDescriptor.builder("charge", Charge.class).retryBudget(2).build();
    assertEquals(StepName.of("charge"), charge.getName());
    assertEquals(2, charge.getRetryBudget());
    assertEquals(3, charge.getRequests().size());
  }

  @Test
  void aPolicyDescribedAloneIsNamedAsItsAnnotationSays() {
    assertEquals(PolicyName.of("audit"), StepPolicyDescriptor.of(Audit.class).getName());
    assertEquals(PolicyName.of("ledger"), WorkflowPolicyDescriptor.of(Ledger.class).getName());
  }

  @Test
  void aWorkflowDeclaredByHandRunsThePartsDescribedAlone() throws Refusal {
    var charge =
        StepDescriptor.builder("charge", Charge.class)
            .rescue(RescueStepDescriptor.of(Renew.class))
            .policy(StepPolicyDescriptor.of(Audit.class))
            .build();
    var shop =
        WorkflowDescriptor.builder(Shop.class, "shop")
            .step(charge)
            .policy(WorkflowPolicyDescriptor.of(Ledger.class))
            .build();
    var events = new ArrayList<Event>();
    var instance =
        InstanceBuilder.of(shop, new Shop())
            .data("card", "4242")
            .data("amount", 40L)
            .reporter(events::add)
            .create();
    var result = new LocalExecutor().run(instance);
    assertEquals(
        new Receipt("4242 renewed", 40L, "charged 40"), result.output(TypeToken.of(Receipt.class)));
    assertEquals(
        List.of("paid 40"),
        events.stream()
            .filter(JourneyInfo.class::isInstance)
            .map(JourneyInfo.class::cast)
            .map(JourneyInfo::message)
            .toList());
  }

  @ParameterizedTest
  @MethodSource("unmarked")
  void aPartDescribedAloneIsRefusedWhenItsClassIsNotMarked(Executable describe, String problem) {
    var refused = assertThrows(Violations.class, describe);
    assertEquals(
        List.of(problem),
        refused.getViolations().stream().map(JavaPartClassesTest::problem).toList());
  }

  static Stream<Arguments> unmarked() {
    return Stream.of(
        Arguments.argumentSet(
            "a step",
            (Executable) () -> StepDescriptor.builder("charge", UnmarkedStep.class),
            "is not marked @Step"),
        Arguments.argumentSet(
            "a rescue step",
            (Executable) () -> RescueStepDescriptor.of(UnmarkedRescue.class),
            "is not marked @RescueStep"),
        Arguments.argumentSet(
            "a step policy",
            (Executable) () -> StepPolicyDescriptor.of(UnmarkedStepPolicy.class),
            "is not marked @StepPolicy"),
        Arguments.argumentSet(
            "a workflow policy",
            (Executable) () -> WorkflowPolicyDescriptor.of(UnmarkedWorkflowPolicy.class),
            "is not marked @WorkflowPolicy"));
  }

  private static String problem(Violation violation) {
    return ((Violation.Misdeclared) violation).problem();
  }
}
