package org.itineradev.itinera.executor.async;

import org.itineradev.itinera.policy.OnSuccess;
import org.itineradev.itinera.policy.async.AsyncHook;
import org.itineradev.itinera.report.async.AsyncDispatcherFactory;
import org.itineradev.itinera.report.async.AsyncReporter;
import org.itineradev.itinera.request.StepHookKind;
import org.itineradev.itinera.workflow.Async;
import org.itineradev.itinera.workflow.async.AsyncInputAdapter;

/**
 * Asynchronous parts written in Java, which javac accepts though they write none of their {@code
 * suspend} operations, since it cannot see them.
 */
final class WrittenInJava {
  private WrittenInJava() {}

  static final class Reporter implements AsyncReporter {}

  static final class Hook implements AsyncHook<Object, StepHookKind<Async>, OnSuccess> {}

  static final class DispatcherFactory implements AsyncDispatcherFactory {}

  static final class Adapter implements AsyncInputAdapter<Object> {}
}
