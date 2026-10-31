package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.StepKind;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepReporter;
import org.itineradev.itinera.step.SyncStepFactory;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaStepRequestsTest {
  interface PaymentGateway {
    String take(long amount);
  }

  @Step
  public static final class Charge implements ItineraStep {
    private final long amount;
    private final @Nullable String note;

    Charge(@Input("amount") long amount, @Input("note") @Nullable String note) {
      this.amount = amount;
      this.note = note;
    }

    public Outcome run(Contributor contributor, StepReporter reporter) {
      reporter.info("charging", note);
      contributor.contribute("charged", amount);
      return succeed();
    }
  }

  @Step
  public static final class Note implements ItineraStep {
    @NullMarked
    Note(@Input("note") String note) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Workflow(name = "noting")
  @WithStep(step = Note.class, name = "note")
  public static final class Noting implements ItineraWorkflow {}

  @Workflow(name = "charging")
  @WithStep(step = Charge.class, name = "charge")
  public static final class Charging implements ItineraWorkflow {}

  @Test
  void aJavaStepRequestsItsConstructorsInputsThenWhatItsRunTakes() {
    var charge = WorkflowDescriptor.of(Charging.class).getSteps().get(0);
    assertEquals(
        List.of("input amount: Long", "input note: String?", "contributor", "step reporter"),
        charge.getRequests().stream().map(Request::toString).toList());
  }

  @Test
  void aJavaStepIsBuiltWithItsInputsAndRunWithItsHandles() {
    var charge = WorkflowDescriptor.of(Charging.class).getSteps().get(0);
    var requests = charge.getRequests();
    var contributed = new ArrayList<String>();
    var reporter = new RecordingReporter();
    Contributor contributor = (key, value) -> contributed.add(key + " " + value);
    var answers =
        new Answers<StepKind<Sync>>(
            Map.of(
                requests.get(0),
                40L,
                requests.get(1),
                "gift",
                requests.get(2),
                contributor,
                requests.get(3),
                reporter));
    ((SyncStepFactory) charge.getFactory()).build(answers).run();
    assertEquals(List.of("charged 40"), contributed);
    assertEquals(List.of("charging gift"), reporter.infos);
  }

  @Test
  void aNullMarkedConstructorsInputsExcludeNull() {
    var note = WorkflowDescriptor.of(Noting.class).getSteps().get(0);
    assertEquals(
        List.of("input note: String"), note.getRequests().stream().map(Request::toString).toList());
  }

  private static final class RecordingReporter implements StepReporter {
    final List<String> infos = new ArrayList<>();

    @Override
    public void info(String message) {
      infos.add(message);
    }

    @Override
    public void info(String message, @Nullable Object data) {
      infos.add(message + " " + data);
    }

    @Override
    public void warning(String message) {}

    @Override
    public void warning(String message, @Nullable Object data) {}

    @Override
    public void error(String message) {}

    @Override
    public void error(String message, @Nullable Object data) {}

    @Override
    public void setGlobalMetadata(String key, @Nullable Object value) {}
  }

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInWhatAJavaStepRequestsIsRefusedWhenItIsDescribed(
      Class<? extends ItineraWorkflow> workflow, List<String> problems) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        problems, refused.getViolations().stream().map(JavaStepRequestsTest::problem).toList());
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a step class not marked @Step", Unmarked.class, List.of("is not marked @Step")),
        Arguments.argumentSet(
            "a step class with two constructors",
            TwoConstructors.class,
            List.of("has several constructors, and a step has one")),
        Arguments.argumentSet(
            "an inner step class",
            Inner.class,
            List.of(
                "captures what its enclosing code holds, which itinera cannot give: declare the "
                    + "step at the top level, or as a static nested class")),
        Arguments.argumentSet(
            "an input whose name was not kept",
            Unkeyed.class,
            List.of(
                "names no key for arg0, whose name was not kept: write the key, or compile with "
                    + "-parameters")),
        Arguments.argumentSet(
            "an Optional input without itinera-java",
            Optionally.class,
            List.of(
                "requests an Optional, which itinera reads only with the artifact "
                    + "org.itinera-dev:itinera-java: add it to the application's dependencies")),
        Arguments.argumentSet(
            "an input no value can be",
            Linked.class,
            List.of(
                "requests lines: No value is a java.util.LinkedList: a value's lists, sets and "
                    + "maps are read as List, Set and Map, or as ArrayList, LinkedHashSet and "
                    + "LinkedHashMap.")),
        Arguments.argumentSet(
            "an input requested in run",
            InputInRun.class,
            List.of("requests an input in run, though a step's inputs are its constructor's")),
        Arguments.argumentSet(
            "a service no provider serves", Paying.class, List.of("service not provided")),
        Arguments.argumentSet(
            "one key requested twice among a step's services",
            PayingTwice.class,
            List.of("service requested twice", "service not provided")));
  }

  private static String problem(Violation violation) {
    return violation instanceof Violation.Misdeclared misdeclared
        ? misdeclared.problem()
        : violation.getNamed();
  }

  public static final class UnmarkedStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class TwoConstructorsStep implements ItineraStep {
    TwoConstructorsStep() {}

    TwoConstructorsStep(@Input("amount") long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public final class InnerStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class UnkeyedStep implements ItineraStep {
    UnkeyedStep(@Input long amount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class OptionalStep implements ItineraStep {
    OptionalStep(@Input("discount") Optional<Long> discount) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class LinkedStep implements ItineraStep {
    LinkedStep(@Input("lines") LinkedList<String> lines) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class InputInRunStep implements ItineraStep {
    public Outcome run(@Input("amount") long amount) {
      return succeed();
    }
  }

  @Step
  public static final class PayingStep implements ItineraStep {
    public Outcome run(@Service("payments") PaymentGateway payments) {
      return succeed();
    }
  }

  @Step
  public static final class PayingTwiceStep implements ItineraStep {
    public Outcome run(
        @Service("payments") PaymentGateway payments, @Service("payments") PaymentGateway again) {
      return succeed();
    }
  }

  @WithStep(step = UnmarkedStep.class, name = "charge")
  public static final class Unmarked implements ItineraWorkflow {}

  @WithStep(step = TwoConstructorsStep.class, name = "charge")
  public static final class TwoConstructors implements ItineraWorkflow {}

  @WithStep(step = InnerStep.class, name = "charge")
  public static final class Inner implements ItineraWorkflow {}

  @WithStep(step = UnkeyedStep.class, name = "charge")
  public static final class Unkeyed implements ItineraWorkflow {}

  @WithStep(step = OptionalStep.class, name = "charge")
  public static final class Optionally implements ItineraWorkflow {}

  @WithStep(step = LinkedStep.class, name = "charge")
  public static final class Linked implements ItineraWorkflow {}

  @WithStep(step = InputInRunStep.class, name = "charge")
  public static final class InputInRun implements ItineraWorkflow {}

  @WithStep(step = PayingStep.class, name = "charge")
  public static final class Paying implements ItineraWorkflow {}

  @WithStep(step = PayingTwiceStep.class, name = "charge")
  public static final class PayingTwice implements ItineraWorkflow {}
}
