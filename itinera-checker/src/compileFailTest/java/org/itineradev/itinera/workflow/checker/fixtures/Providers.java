package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.List;
import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromStep;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.Provider;
import org.itineradev.itinera.service.ProviderReporter;
import org.itineradev.itinera.service.Service;
import org.itineradev.itinera.step.Attempt;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.Key;
import org.itineradev.itinera.workflow.WithStep;

/** Mistakes in service providers, each in a workflow of its own. */
public final class Providers {
  private Providers() {}

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

  @Step
  public static final class Pay implements ItineraStep {
    public Outcome run(@Service("payments") Payments payments) {
      return succeed();
    }
  }

  @WithStep(step = Pay.class, name = "pay")
  public static final class ExtendingWildcard implements ItineraWorkflow {
    @Provider(key = "payments")
    public Provided<? extends Payments> payments() {
      return noInstance();
    }
  }

  public abstract static class Supplying<T> implements ItineraWorkflow {
    @Provider(key = "payments")
    public Provided<Payments> payments(@FromWorkflow("asked") T asked) {
      return noInstance();
    }
  }

  @WithStep(step = Pay.class, name = "pay")
  public static final class NarrowedProvider extends Supplying<String> {
    @Override
    public Provided<Payments> payments(String asked) {
      return noInstance();
    }

    @Provider(key = "payments", name = "gateway")
    public Provided<Payments> gateway() {
      return noInstance();
    }
  }

  @Step
  public static final class BoundPay implements ItineraStep {
    public <P extends Payments> Outcome run(@Service("payments") P payments) {
      return succeed();
    }
  }

  @WithStep(step = BoundPay.class, name = "pay")
  public static final class BoundService implements ItineraWorkflow {
    @Provider(key = "payments")
    public Provided<Payments> payments() {
      return noInstance();
    }
  }

  @Step
  public static final class Labelling implements ItineraStep {
    public Outcome run(@Service("labels") String[] labels) {
      return succeed();
    }
  }

  @WithStep(step = Labelling.class, name = "label")
  public static final class ArrayService implements ItineraWorkflow {
    @Provider(key = "labels")
    public Provided<String[]> labels() {
      return noInstance();
    }
  }
}
