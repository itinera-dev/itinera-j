package org.itineradev.itinera.conformance.record;

import static org.itineradev.itinera.conformance.record.Dispatchers.dispatchers;
import static org.itineradev.itinera.conformance.record.Dispatchers.failingDispatchers;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.itineradev.itinera.conformance.model.DispatcherScript;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.EventKind;
import org.itineradev.itinera.event.JourneyAborted;
import org.itineradev.itinera.event.JourneyStarted;
import org.itineradev.itinera.event.ReporterFailed;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.report.Dispatcher;
import org.itineradev.itinera.report.testing.RecordingReporter;
import org.itineradev.itinera.workflow.WorkflowName;
import org.junit.jupiter.api.Test;

final class DispatchersTest {
  private static final Instant TIMESTAMP = Instant.parse("2026-10-28T02:00:00Z");
  private static final JourneyId JOURNEY = JourneyId.of("order-42");
  private static final WorkflowName WORKFLOW = WorkflowName.of("orders");
  private static final Event STARTED =
      new JourneyStarted(1, TIMESTAMP, JOURNEY, WORKFLOW, Set.of("amount"));
  private static final Event ABORTED =
      new JourneyAborted(2, TIMESTAMP, JOURNEY, WORKFLOW, new ReporterFailed(null, "full"));

  private final RecordingReporter held = new RecordingReporter();
  private final RecordingReporter added = new RecordingReporter();

  private Dispatcher scripted(DispatcherScript script) {
    return dispatchers(held, script).create();
  }

  @Test
  void aScriptedDispatcherDeliversToTheReporterItHoldsAndToThoseAdded() {
    var dispatcher = scripted(new DispatcherScript.Delivers());
    dispatcher.add(added);
    dispatcher.setGlobalMetadata("tenant", "acme");
    dispatcher.dispatch(STARTED);
    assertEquals(List.of(STARTED), held.getEvents());
    assertEquals(Map.of("tenant", "acme"), held.getGlobalMetadata());
    assertEquals(List.of(STARTED), added.getEvents());
  }

  @Test
  void aDispatcherThatIgnoresAddedReportersDeliversOnlyToTheOneItHolds() {
    var dispatcher = scripted(new DispatcherScript.IgnoresAddedReporters());
    dispatcher.add(added);
    dispatcher.dispatch(STARTED);
    assertEquals(List.of(STARTED), held.getEvents());
    assertEquals(List.of(), added.getEvents());
  }

  @Test
  void aDispatcherThatThrowsWhenAReporterIsAddedAddsNone() {
    var dispatcher = scripted(new DispatcherScript.ThrowsWhenAdding());
    assertThrows(DispatcherFailure.class, () -> dispatcher.add(added));
    dispatcher.dispatch(STARTED);
    assertEquals(List.of(), added.getEvents());
  }

  @Test
  void aDispatcherThatThrowsOnOneKindOfEventDeliversEveryOther() {
    var dispatcher =
        scripted(new DispatcherScript.ThrowsWhenDispatching(EventKind.JOURNEY_ABORTED));
    dispatcher.dispatch(STARTED);
    assertThrows(DispatcherFailure.class, () -> dispatcher.dispatch(ABORTED));
    assertEquals(List.of(STARTED), held.getEvents());
  }

  @Test
  void aFailingFactoryThrowsWhenAskedForADispatcher() {
    var factory = failingDispatchers();
    assertThrows(DispatcherFailure.class, factory::create);
  }
}
