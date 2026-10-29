package org.itineradev.itinera.workflow.checker.fixtures;

import java.util.Optional;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.journey.DataBagAccess;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.policy.FromWorkflow;
import org.itineradev.itinera.policy.Role;
import org.itineradev.itinera.step.Input;
import org.itineradev.itinera.step.ItineraStep;
import org.itineradev.itinera.step.Outcome;
import org.itineradev.itinera.step.Step;
import org.itineradev.itinera.step.StepName;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.InputAdapter;
import org.itineradev.itinera.workflow.ItineraWorkflow;
import org.itineradev.itinera.workflow.Key;
import org.itineradev.itinera.workflow.Supplied;
import org.itineradev.itinera.workflow.WithStep;

/** Mistakes in input adapters, each in a workflow of its own. */
public final class Adapters {
  private Adapters() {}

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

  @WithStep(step = Charge.class, name = "charge")
  public static final class Contributing implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(Contributor contributor) {
      return fromDataBag();
    }
  }

  public abstract static class Asking<T> implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public Supplied priced(@FromWorkflow("asked") T asked) {
      return fromDataBag();
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class NarrowedOverride extends Asking<String> {
    @Override
    public Supplied priced(String asked) {
      return fromDataBag();
    }
  }

  @WithStep(step = Charge.class, name = "charge")
  public static final class BoundKey implements ItineraWorkflow {
    @InputAdapter(steps = "charge")
    public <K extends String> Supplied priced(@Key K key, StepName step) {
      return fromDataBag();
    }
  }
}
