package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.itineradev.itinera.executor.LocalExecutor;
import org.itineradev.itinera.executor.Refusal;
import org.itineradev.itinera.journey.DataBag;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaAnnotatedWorkflowsTest {
  @Step
  public static final class Pack implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class Ship implements ItineraStep {
    public Outcome run() {
      return Outcome.success();
    }
  }

  @Workflow(name = "shipping")
  @WithStep(step = Pack.class, name = "pack")
  @WithStep(step = Ship.class, name = "ship", retryBudget = 2, noRetryOnAbnormalTermination = true)
  public static final class Shipping implements ItineraWorkflow {
    private final String warehouse;

    Shipping(String warehouse) {
      this.warehouse = warehouse;
    }

    @IdGenerator
    public String parcelId(DataBag data) {
      return warehouse + "-" + data.read("parcel", TypeToken.of(String.class)).orNull();
    }
  }

  @WithStep(step = Pack.class, name = "pack")
  public static final class Unnamed implements ItineraWorkflow {}

  @Test
  void anAnnotatedJavaWorkflowRunsItsStepsInTheOrderWritten() throws Refusal {
    var instance = new Shipping("north").instance().data("parcel", "7").create();
    var result = new LocalExecutor().run(instance);
    assertTrue(result.isSucceeded());
    assertEquals(JourneyId.of("north-7"), result.journeyId());
    var descriptor = WorkflowDescriptor.of(Shipping.class);
    assertEquals(WorkflowName.of("shipping"), descriptor.getName());
    assertEquals(
        List.of(StepName.of("pack"), StepName.of("ship")),
        descriptor.getSteps().stream().map(StepDescriptor::getName).toList());
  }

  @Test
  void eachStepStatesWhatItsAnnotationSays() {
    var ship = WorkflowDescriptor.of(Shipping.class).getSteps().get(1);
    assertEquals(2, ship.getRetryBudget());
    assertFalse(ship.getRetryOnAbnormalTermination());
  }

  @Test
  void aClassIsDescribedOnceAndItsDescriptorShared() {
    assertSame(WorkflowDescriptor.of(Shipping.class), WorkflowDescriptor.of(Shipping.class));
  }

  @Test
  void aWorkflowWithoutANameIsNamedAfterItsClass() {
    assertEquals(WorkflowName.of("Unnamed"), WorkflowDescriptor.of(Unnamed.class).getName());
  }

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInAnAnnotatedClassIsRefusedWhenItIsDescribed(
      Class<? extends ItineraWorkflow> workflow, List<String> problems) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        problems,
        refused.getViolations().stream().map(JavaAnnotatedWorkflowsTest::problem).toList());
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a step class with no run", NoRun.class, List.of("has no public method named run")),
        Arguments.argumentSet(
            "a step class with two runs",
            TwoRuns.class,
            List.of("has several public methods named run")),
        Arguments.argumentSet(
            "a run that returns what is not an outcome",
            Returning.class,
            List.of("returns String, not Outcome")),
        Arguments.argumentSet(
            "a run with a parameter",
            Requesting.class,
            List.of(
                "has a parameter that requests nothing, arg0: mark a service @Service, and "
                    + "request a handle by its type")),
        Arguments.argumentSet(
            "a step class whose constructor takes what is not an input",
            Constructed.class,
            List.of(
                "has a parameter that is not an input, arg0: a step's constructor takes only "
                    + "inputs, marked @Input")),
        Arguments.argumentSet(
            "an abstract step class",
            AbstractStep.class,
            List.of("cannot be built: it is abstract")),
        Arguments.argumentSet(
            "an empty step name and a retry budget below 0",
            BadSteps.class,
            List.of("names a step with an empty name", "gives step ship a retry budget below 0")),
        Arguments.argumentSet(
            "an empty workflow name",
            EmptyName.class,
            List.of("names its workflow with an empty name")),
        Arguments.argumentSet(
            "two steps with one name", Twice.class, List.of("duplicate step name")),
        Arguments.argumentSet(
            "two ID generators", TwoGenerators.class, List.of("has several ID generators")),
        Arguments.argumentSet(
            "an ID generator returning what is not a string",
            ObjectIds.class,
            List.of("returns Object, not String")),
        Arguments.argumentSet(
            "an ID generator taking what it cannot",
            NumberedIds.class,
            List.of("takes what an ID generator cannot: it takes nothing or a DataBag")),
        Arguments.argumentSet(
            "a Kotlin step without itinera-kotlin",
            KotlinStep.class,
            List.of("is written in Kotlin, which itinera reads only with itinera-kotlin")));
  }

  private static String problem(Violation violation) {
    return violation instanceof Violation.Misdeclared misdeclared
        ? misdeclared.problem()
        : violation.getNamed();
  }

  @Step
  public static final class TwoRunsStep implements ItineraStep {
    public Outcome run() {
      return succeed();
    }

    public Outcome run(String parcel) {
      return succeed();
    }
  }

  @Step
  public static final class StringStep implements ItineraStep {
    public String run() {
      return "done";
    }
  }

  @Step
  public static final class RequestingStep implements ItineraStep {
    public Outcome run(String parcel) {
      return succeed();
    }
  }

  @Step
  public static final class ConstructedStep implements ItineraStep {
    ConstructedStep(String parcel) {}

    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public abstract static class Abstract implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @Step
  public static final class Nothing implements ItineraStep {}

  @WithStep(step = Nothing.class, name = "pack")
  public static final class NoRun implements ItineraWorkflow {}

  @WithStep(step = TwoRunsStep.class, name = "pack")
  public static final class TwoRuns implements ItineraWorkflow {}

  @WithStep(step = StringStep.class, name = "pack")
  public static final class Returning implements ItineraWorkflow {}

  @WithStep(step = RequestingStep.class, name = "pack")
  public static final class Requesting implements ItineraWorkflow {}

  @WithStep(step = ConstructedStep.class, name = "pack")
  public static final class Constructed implements ItineraWorkflow {}

  @WithStep(step = Abstract.class, name = "pack")
  public static final class AbstractStep implements ItineraWorkflow {}

  @WithStep(step = Pack.class, name = "")
  @WithStep(step = Ship.class, name = "ship", retryBudget = -1)
  public static final class BadSteps implements ItineraWorkflow {}

  @Workflow(name = "")
  public static final class EmptyName implements ItineraWorkflow {}

  @WithStep(step = Pack.class, name = "pack")
  @WithStep(step = Ship.class, name = "pack")
  public static final class Twice implements ItineraWorkflow {}

  public static final class TwoGenerators implements ItineraWorkflow {
    @IdGenerator
    public String first() {
      return "a";
    }

    @IdGenerator
    public String second() {
      return "b";
    }
  }

  public static final class ObjectIds implements ItineraWorkflow {
    @IdGenerator
    public Object id() {
      return "a";
    }
  }

  public static final class NumberedIds implements ItineraWorkflow {
    @IdGenerator
    public String id(long number) {
      return "a" + number;
    }
  }

  @WithStep(step = org.itineradev.itinera.step.samples.Ship.class, name = "ship")
  public static final class KotlinStep implements ItineraWorkflow {}
}
