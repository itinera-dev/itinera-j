package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.request.Answers;
import org.itineradev.itinera.request.AttemptProviderKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.service.AttemptProviderDescriptor;
import org.itineradev.itinera.service.JourneyProviderDescriptor;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.service.ProviderName;
import org.itineradev.itinera.service.ProviderReporter;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.service.SyncProvider;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaProviderMethodsTest {
  @FunctionalInterface
  public interface Payments {
    String take(long amount);
  }

  @Step
  public static final class Charge implements ItineraStep {
    public Outcome run(@Service("payments") Payments payments) {
      payments.take(40);
      return succeed();
    }
  }

  @Step
  public static final class Print implements ItineraStep {
    public Outcome run() {
      return succeed();
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class Shop implements ItineraWorkflow {
    private final String name;

    public Shop(String name) {
      this.name = name;
    }

    @Provider(
        perAttempt = true,
        steps = {"charge"},
        key = "payments",
        name = "gateway")
    public Provided<Payments> opened(
        @FromWorkflow("currency") String currency,
        Attempt attempt,
        StepName step,
        ProviderReporter reporter) {
      reporter.info("opening the gateway");
      Payments payments = amount -> name + "-" + attempt + "-" + amount + " " + currency;
      return name.isEmpty() ? noInstance() : provide(payments, name);
    }
  }

  @Test
  void aJavaProviderRequestsWhatItsParametersNameInOrder() {
    var gateway =
        (AttemptProviderDescriptor<?, ?>) WorkflowDescriptor.of(Shop.class).getProviders().get(0);
    assertEquals(
        List.of(ProviderName.of("gateway"), "payments", Payments.class),
        List.of(gateway.getName(), gateway.getKey(), gateway.getType()));
    assertEquals(List.of(StepName.of("charge")), gateway.getSteps());
    assertEquals(
        List.of(
            "data from the workflow currency: String?",
            "attempt",
            "step name",
            "provider reporter"),
        gateway.getRequests().stream().map(Request::toString).toList());
  }

  @ParameterizedTest
  @MethodSource("shops")
  void aJavaProviderIsCalledOnTheWorkflowObjectWithItsAnswers(
      String shop, String taken, String data) {
    var gateway =
        (AttemptProviderDescriptor<Shop, Sync>)
            WorkflowDescriptor.of(Shop.class).getProviders().get(0);
    var requests = gateway.getRequests();
    var told = new ArrayList<String>();
    var call = (SyncProvider<Shop, AttemptProviderKind<Sync>, ?>) gateway.getCall();
    var provided =
        call.provide(
            new Shop(shop),
            new Answers<>(
                Map.of(
                    requests.get(0),
                    "EUR",
                    requests.get(1),
                    Attempt.of(2),
                    requests.get(2),
                    StepName.of("charge"),
                    requests.get(3),
                    new Telling(told))));
    assertEquals(
        taken,
        provided instanceof Provided.Instance<?> instance
            ? ((Payments) instance.instance()).take(40)
            : null);
    assertEquals(data, provided instanceof Provided.Instance<?> instance ? instance.data() : null);
    assertEquals(List.of("opening the gateway"), told);
  }

  static Stream<Arguments> shops() {
    return Stream.of(
        Arguments.argumentSet("the shop's gateway", "corner", "corner-2-40 EUR", "corner"),
        Arguments.argumentSet("no instance for a shop with no name", "", null, null));
  }

  @Test
  void aJavaProviderOfJourneyScopeIsNamedAndKeyedAfterItsMethod() {
    var receipts =
        (JourneyProviderDescriptor<?, ?>)
            WorkflowDescriptor.of(Printing.class).getProviders().get(0);
    assertEquals(
        List.of(ProviderName.of("receipts"), "receipts", String.class),
        List.of(receipts.getName(), receipts.getKey(), receipts.getType()));
    assertEquals(
        List.of("journey ID"), receipts.getRequests().stream().map(Request::toString).toList());
  }

  @ParameterizedTest
  @MethodSource("misscoped")
  void aJavaProviderOfJourneyScopeRequestingWhatOnlyOneOfAttemptScopeReceivesIsRefused(
      Class<? extends ItineraWorkflow> workflow) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        List.of(new Violation.AttemptRequestInJourneyScope(ProviderName.of("receipts"))),
        refused.getViolations());
  }

  static Stream<Arguments> misscoped() {
    return Stream.of(
        Arguments.argumentSet("the attempt", Misscoped.class),
        Arguments.argumentSet("the step's name", Naming.class));
  }

  @ParameterizedTest
  @MethodSource("misdeclared")
  void everyMistakeInAJavaProviderIsRefusedWhenItsWorkflowIsDescribed(
      Class<? extends ItineraWorkflow> workflow, String problem) {
    var refused = assertThrows(Violations.class, () -> WorkflowDescriptor.of(workflow));
    assertEquals(
        List.of(problem),
        refused.getViolations().stream().map(JavaProviderMethodsTest::problem).toList());
  }

  private static String problem(Violation violation) {
    return ((Violation.Misdeclared) violation).problem();
  }

  static Stream<Arguments> misdeclared() {
    return Stream.of(
        Arguments.argumentSet(
            "a provider naming a step with an empty name",
            EmptyStep.class,
            "names a step with an empty name"),
        Arguments.argumentSet(
            "data from the step",
            FromStepping.class,
            "marks arg0 @FromStep, which a service provider never requests"),
        Arguments.argumentSet(
            "a role",
            RoleProvider.class,
            "marks arg0 @Role, which a service provider never requests"),
        Arguments.argumentSet(
            "an input",
            Inputting.class,
            "marks arg0 @Input, which a service provider never requests"),
        Arguments.argumentSet(
            "a service",
            Servicing.class,
            "marks arg0 @Service, which a service provider never requests"),
        Arguments.argumentSet(
            "a key", Keyed.class, "marks arg0 @Key, which a service provider never requests"),
        Arguments.argumentSet(
            "a raw Provided",
            Raw.class,
            "returns Provided of no class, which names no service "
                + "type: write the service's type in Provided"),
        Arguments.argumentSet(
            "a provider that returns what is not provided",
            Returning.class,
            "returns String, not Provided"),
        Arguments.argumentSet(
            "a contributor",
            Contributing.class,
            "requests a contributor, which a service provider never receives"),
        Arguments.argumentSet(
            "an Optional without itinera-java",
            Optionally.class,
            "requests an Optional, which itinera reads only with the artifact "
                + "org.itinera-dev:itinera-java: add it to the application's dependencies"),
        Arguments.argumentSet(
            "a parameter that requests nothing",
            Unasking.class,
            "has a parameter that requests nothing, arg0: mark data @FromWorkflow, and request "
                + "the journey ID, the step's name, the attempt and the provider's reporter by "
                + "their types"));
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Printing implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(JourneyId journey) {
      return provide("receipt " + journey);
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Misscoped implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(Attempt attempt) {
      return provide("receipt " + attempt);
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Naming implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(StepName step) {
      return provide("receipt of " + step);
    }
  }

  @Role
  public interface Customers {
    void tell(String message);
  }

  @WithStep(step = Print.class, name = "print")
  public static final class EmptyStep implements ItineraWorkflow {
    @Provider(steps = "")
    public Provided<String> receipts() {
      return noInstance();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class FromStepping implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@FromStep("printed") String printed) {
      return provide(printed);
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class RoleProvider implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@Role Customers customers) {
      customers.tell("printing");
      return noInstance();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Inputting implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@Input("printer") String printer) {
      return provide(printer);
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Servicing implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@Service("payments") Payments payments) {
      return provide(payments.take(1));
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Keyed implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@Key String key) {
      return provide(key);
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Raw implements ItineraWorkflow {
    @SuppressWarnings("rawtypes")
    @Provider
    public Provided receipts() {
      return noInstance();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Returning implements ItineraWorkflow {
    @Provider
    public String receipts() {
      return "receipt";
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Contributing implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(Contributor contributor) {
      contributor.contribute("printed", true);
      return noInstance();
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Optionally implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(@FromWorkflow("printer") Optional<String> printer) {
      return provide(printer.orElse("none"));
    }
  }

  @WithStep(step = Print.class, name = "print")
  public static final class Unasking implements ItineraWorkflow {
    @Provider
    public Provided<String> receipts(int copies) {
      return provide("copies " + copies);
    }
  }

  record Telling(List<String> told) implements ProviderReporter {
    @Override
    public void info(String message) {
      told.add(message);
    }

    @Override
    public void info(String message, Object data) {
      told.add(message);
    }

    @Override
    public void warning(String message) {
      told.add(message);
    }

    @Override
    public void warning(String message, Object data) {
      told.add(message);
    }

    @Override
    public void error(String message) {
      told.add(message);
    }

    @Override
    public void error(String message, Object data) {
      told.add(message);
    }

    @Override
    public void setGlobalMetadata(String key, Object value) {
      told.add(key);
    }
  }
}
