package org.itineradev.itinera.conformance.record;

import org.itineradev.itinera.conformance.model.DispatcherScript;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.report.DefaultDispatcher;
import org.itineradev.itinera.report.Dispatcher;
import org.itineradev.itinera.report.DispatcherFactory;
import org.itineradev.itinera.report.Reporter;

/** The runner's dispatcher factories. */
public final class Dispatchers {
  private Dispatchers() {}

  /**
   * The runner's dispatcher factory: every dispatcher it creates holds {@code held}, the same
   * reporter each time, and delivers as a {@link DefaultDispatcher} does to it and to the reporters
   * the executor adds, except where {@code script} says otherwise.
   */
  public static DispatcherFactory dispatchers(Reporter held, DispatcherScript script) {
    return () -> {
      var delivering = new DefaultDispatcher();
      delivering.add(held);
      return new Scripted(delivering, script);
    };
  }

  /** A dispatcher factory that fails whenever the executor asks it for a dispatcher. */
  public static DispatcherFactory failingDispatchers() {
    return () -> {
      throw new DispatcherFailure("the factory fails");
    };
  }

  private record Scripted(DefaultDispatcher delivering, DispatcherScript script)
      implements Dispatcher {
    @Override
    public void add(Reporter reporter) {
      if (script.throwsWhenAdding()) {
        throw new DispatcherFailure("the dispatcher throws when a reporter is added");
      }
      if (script.addsReporters()) {
        delivering.add(reporter);
      }
    }

    @Override
    public void dispatch(Event event) {
      if (script.throwsWhenDispatching(event.getKind())) {
        throw new DispatcherFailure("the dispatcher throws when dispatching " + event.getKind());
      }
      delivering.dispatch(event);
    }

    @Override
    public void setGlobalMetadata(String key, Object value) {
      delivering.setGlobalMetadata(key, value);
    }
  }
}
