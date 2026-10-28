package org.itineradev.itinera.conformance.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.itineradev.itinera.conformance.model.ReporterFailure;
import org.itineradev.itinera.conformance.trace.Events;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.EventKind;
import org.junit.jupiter.api.Test;

final class RecordersTest {
  @Test
  void aReporterKeepsTheEventItFailsOnAndRemembersThatItFailed() {
    var recorders =
        new Recorders(
            Map.of("audit", new ReporterFailure.OnEvent(EventKind.ATTEMPT_STARTED, "disk full")));
    var audit = recorders.make("audit");
    var events = new Events();
    audit.report(events.started());
    var failure =
        assertThrows(ScriptedReporterFailure.class, () -> audit.report(events.attempt("charge")));
    assertEquals("disk full", failure.getMessage());
    assertEquals(
        List.of(EventKind.JOURNEY_STARTED, EventKind.ATTEMPT_STARTED),
        audit.events().stream().map(Event::getKind).toList());
    assertTrue(audit.failed());
  }

  @Test
  void aReporterThatFailsOnTheFirstEventFailsOnNoOther() {
    var audit = new Recorders(Map.of("audit", new ReporterFailure.OnFirstEvent())).make("audit");
    var events = new Events();
    assertThrows(ScriptedReporterFailure.class, () -> audit.report(events.started()));
    audit.report(events.attempt("charge"));
    assertEquals(2, audit.events().size());
  }

  @Test
  void anEntryOfGlobalMetadataRecordsTheKindOfTheLastEventBeforeIt() {
    var audit = new Recorders(Map.of()).make("audit");
    audit.setGlobalMetadata("journey_id", "order-1");
    audit.report(new Events().started());
    audit.setGlobalMetadata("tenant", "acme");
    assertEquals(
        List.of(
            new Entry("journey_id", "order-1", null),
            new Entry("tenant", "acme", EventKind.JOURNEY_STARTED)),
        audit.entries());
    assertFalse(audit.failed());
  }

  @Test
  void eachReporterMadeUnderANameIsKeptInTheOrderMade() {
    var recorders = new Recorders(Map.of());
    var first = recorders.make("audit");
    var second = recorders.make("audit");
    assertEquals(List.of(first, second), recorders.made("audit"));
    assertEquals(List.of(), recorders.made("metrics"));
  }
}
