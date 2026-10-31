package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.itineradev.itinera.engine.DataBags;
import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.InputAdapterKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaAdapterMethodsTest {
  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Shop implements ItineraWorkflow {
    @InputAdapter(
        steps = {"charge"},
        name = "pricing")
    public Supplied priced(
        @Key String key,
        @FromWorkflow("price") long price,
        DataBagAccess access,
        StepName step,
        JourneyId journey) {
      Long discount = access.read("discount", TypeToken.of(Long.class)).orNull();
      return key.equals("amount")
          ? supply(price - (discount == null ? 0L : discount))
          : fromDataBag();
    }
  }

  @Test
  void aJavaInputAdapterRequestsWhatItsParametersNameInOrder() {
    var pricing = WorkflowDescriptor.of(Shop.class).getInputAdapters().get(0);
    assertEquals(AdapterName.of("pricing"), pricing.getName());
    assertEquals(List.of(StepName.of("charge")), pricing.getSteps());
    assertEquals(
        List.of(
            "key",
            "data from the workflow price: Long",
            "data bag access",
            "step name",
            "journey ID"),
        pricing.getRequests().stream().map(Request::toString).toList());
  }

  @Test
  void aJavaInputAdapterIsCalledOnTheWorkflowObjectWithItsAnswers() {
    var pricing = WorkflowDescriptor.of(Shop.class).getInputAdapters().get(0);
    var call = (SyncInputAdapter<Shop>) pricing.getCall();
    assertEquals(
        new Supplied.Value(35L),
        call.adapt(
            new Shop(),
            answered(
                pricing.getRequests(),
                "amount",
                40L,
                DataBags.access(org.itineradev.itinera.journey.DataBags.of("discount", 5L)),
                StepName.of("charge"),
                JourneyId.of("1"))));
  }

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInAJavaInputAdapterIsRefusedWhenItsWorkflowIsDescribed(
      Class<? extends ItineraWorkflow> workflow, String problem) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        List.of(problem),
        refused.getViolations().stream().map(JavaAdapterMethodsTest::problem).toList());
  }

  private static Answers<InputAdapterKind> answered(
      List<? extends Request<?, ?>> requests, Object... answers) {
    return new Answers<>(
        IntStream.range(0, requests.size())
            .boxed()
            .collect(Collectors.toMap(requests::get, at -> answers[at])));
  }

  private static String problem(Violation violation) {
    return ((Violation.Misdeclared) violation).problem();
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a key that is not a String",
            ObjectKey.class,
            "marks arg0 @Key, though the key is a String"),
        Arguments.argumentSet(
            "data whose key is not written and whose name was not kept",
            Keyless.class,
            "names no key for arg0, whose name was not kept: write the key, or compile with "
                + "-parameters"),
        Arguments.argumentSet(
            "a role", RoleAdapter.class, "marks arg0 @Role, which an input adapter never requests"),
        Arguments.argumentSet(
            "an input",
            Inputting.class,
            "marks arg0 @Input, which an input adapter never requests"),
        Arguments.argumentSet(
            "an Optional without itinera-java",
            Optionally.class,
            "requests an Optional, which itinera reads only with the artifact "
                + "org.itinera-dev:itinera-java: add it to the application's dependencies"),
        Arguments.argumentSet(
            "a parameter that requests nothing",
            Unasking.class,
            "has a parameter that requests nothing, arg0: mark data @FromWorkflow and the key "
                + "@Key, and request the step's name, the journey ID and the data bag's access "
                + "by their types"),
        Arguments.argumentSet(
            "an adapter that returns what is not supplied",
            Returning.class,
            "returns String, not Supplied"));
  }

  @Role
  public interface Customers {
    void tell(String message);
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class ObjectKey implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@Key Object key) {
      return supply(key);
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Keyless implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@FromWorkflow long price) {
      return supply(price);
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class RoleAdapter implements ItineraWorkflow, Customers {
    @InputAdapter(steps = "charge")
    public Supplied priced(@Role Customers customers) {
      customers.tell("priced");
      return fromDataBag();
    }

    @Override
    public void tell(String message) {}
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Inputting implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@Input("price") long price) {
      return supply(price);
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Optionally implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@FromWorkflow("price") Optional<Long> price) {
      return supply(price.orElse(0L));
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Unasking implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(int retries) {
      return supply(retries);
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Returning implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public String priced() {
      return "40";
    }
  }
}
