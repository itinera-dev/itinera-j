package org.itineradev.itinera.conformance.trace;

import java.util.List;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.run.Services;
import org.itineradev.itinera.event.AdapterSource;
import org.itineradev.itinera.event.ByWorkflowHook;
import org.itineradev.itinera.event.ContributionSource;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.OptionalInputAbsent;
import org.itineradev.itinera.event.ProviderSource;
import org.itineradev.itinera.event.RequestSource;
import org.itineradev.itinera.event.ServiceProvided;
import org.itineradev.itinera.event.StepHookSource;
import org.itineradev.itinera.event.StepSource;

/** The cells of the events whose columns depend on who they name. */
final class EventCells {
  private EventCells() {}

  /**
   * The cells of {@code event}: its code, its step, its error's message and what its abort names.
   */
  static List<Cell> cellsOf(JourneyAborted event) {
    var abort = event.abort();
    return Stream.concat(
            Stream.of(
                new Cell("code", abort.getReason()),
                new Cell("step", abort.getStep()),
                new Cell("error", abort.getErrorMessage())),
            Naming.cellsOf(abort).stream())
        .toList();
  }

  /** The cells of {@code event}, whose implementation is written as the cases name it. */
  static List<Cell> cellsOf(ServiceProvided event) {
    return List.of(
        new Cell("step", event.step()),
        new Cell("attempt", event.attempt()),
        new Cell("key", event.key()),
        new Cell("provider", event.provider()),
        new Cell("implementation", Services.writtenImplementation(event.implementation())));
  }

  /** The cells of {@code event}: the key and who requested it. */
  static List<Cell> cellsOf(OptionalInputAbsent event) {
    return Stream.concat(
            Stream.of(new Cell("key", event.key())), cellsOf(event.requester()).stream())
        .toList();
  }

  /** The cells of a contribution under {@code key}: the key, and who made it. */
  static List<Cell> contributedCells(String key, ContributionSource source) {
    return Stream.concat(Stream.of(new Cell("key", key)), cellsOf(source).stream()).toList();
  }

  private static List<Cell> cellsOf(ContributionSource source) {
    if (source instanceof StepSource step) {
      return List.of(
          new Cell("step", step.step()),
          new Cell("attempt", step.attempt()),
          new Cell("source", step.step()));
    }
    if (source instanceof StepHookSource hook) {
      return List.of(
          new Cell("step", hook.step()),
          new Cell("attempt", hook.attempt()),
          new Cell("source", hook.policy() + ", " + hook.hook()));
    }
    var hook = (ByWorkflowHook) source;
    return List.of(new Cell("source", hook.policy() + ", " + hook.hook()));
  }

  private static List<Cell> cellsOf(RequestSource source) {
    if (source instanceof StepSource step) {
      return List.of(new Cell("step", step.step()), new Cell("attempt", step.attempt()));
    }
    if (source instanceof AdapterSource adapter) {
      return List.of(
          new Cell("step", adapter.step()),
          new Cell("attempt", adapter.attempt()),
          new Cell("adapter", adapter.adapter()));
    }
    if (source instanceof ProviderSource provider) {
      return List.of(
          new Cell("step", provider.step()),
          new Cell("attempt", provider.attempt()),
          new Cell("provider", provider.provider()));
    }
    if (source instanceof StepHookSource hook) {
      return Stream.concat(
              Stream.of(new Cell("step", hook.step()), new Cell("attempt", hook.attempt())),
              Naming.hookCells(hook.policy(), hook.hook()).stream())
          .toList();
    }
    var hook = (ByWorkflowHook) source;
    return Naming.hookCells(hook.policy(), hook.hook());
  }
}
