package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.FakeModel;
import org.itineradev.itinera.service.Provided;

/**
 * A fake put in place of a service provider, doing as its model says and counting its calls; the
 * {@code labeller} it makes on call n is labelled {@code fake n}.
 */
public final class CountedFake {
  private final FakeModel fake;
  private int calls;

  CountedFake(FakeModel fake) {
    this.fake = fake;
  }

  /** How many times it was called, whether it made an instance or failed. */
  public int calls() {
    return calls;
  }

  /** The service it makes. */
  Class<?> service() {
    return fake instanceof FakeModel.MakesMeters ? Meter.class : Labeller.class;
  }

  /** What it provides on its next call, of {@code type}, the service it makes. */
  <T> Provided<T> provided(Class<T> type) {
    calls += 1;
    if (fake instanceof FakeModel.Makes makes) {
      var made = Services.labellerOf(makes.implementation(), "fake " + calls, null);
      return new Provided.Instance<>(type.cast(made), makes.data());
    }
    if (fake instanceof FakeModel.Fails fails) {
      throw new IllegalStateException(fails.message());
    }
    return new Provided.Instance<>(type.cast((Meter) () -> 0));
  }
}
