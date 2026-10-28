package org.itineradev.itinera.conformance.model;

import org.itineradev.itinera.event.EventKind;

/** How the dispatchers of the runner's factory behave. */
public sealed interface DispatcherScript {
  /** Whether it throws when the executor adds a reporter. */
  boolean throwsWhenAdding();

  /** Whether it adds the reporters the executor adds. */
  boolean addsReporters();

  /** Whether it throws instead of delivering an event of {@code kind}. */
  boolean throwsWhenDispatching(EventKind kind);

  /** Delivers to the reporter it holds and to those the executor adds, as the default does. */
  record Delivers() implements DispatcherScript {
    @Override
    public boolean throwsWhenAdding() {
      return false;
    }

    @Override
    public boolean addsReporters() {
      return true;
    }

    @Override
    public boolean throwsWhenDispatching(EventKind kind) {
      return false;
    }
  }

  /** Delivers only to the reporter it holds, ignoring those the executor adds. */
  record IgnoresAddedReporters() implements DispatcherScript {
    @Override
    public boolean throwsWhenAdding() {
      return false;
    }

    @Override
    public boolean addsReporters() {
      return false;
    }

    @Override
    public boolean throwsWhenDispatching(EventKind kind) {
      return false;
    }
  }

  /** Throws when the executor adds a reporter, and adds none. */
  record ThrowsWhenAdding() implements DispatcherScript {
    @Override
    public boolean throwsWhenAdding() {
      return true;
    }

    @Override
    public boolean addsReporters() {
      return false;
    }

    @Override
    public boolean throwsWhenDispatching(EventKind kind) {
      return false;
    }
  }

  /**
   * Throws instead of delivering an event of {@code kind}, and delivers every other.
   *
   * @param kind the kind of event it throws on
   */
  record ThrowsWhenDispatching(EventKind kind) implements DispatcherScript {
    @Override
    public boolean throwsWhenAdding() {
      return false;
    }

    @Override
    public boolean addsReporters() {
      return true;
    }

    @Override
    public boolean throwsWhenDispatching(EventKind dispatched) {
      return dispatched == kind;
    }
  }
}
