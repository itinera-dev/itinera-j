package org.itineradev.itinera.conformance.trace;

import static org.itineradev.itinera.conformance.trace.Fields.apartFromIdAndTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.itineradev.itinera.event.Event;
import org.itineradev.itinera.event.JourneyStarted;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.workflow.WorkflowName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

final class FieldsTest {
  private static List<Event> started(String id, String at, String workflow, Set<String> keys) {
    return List.of(
        new JourneyStarted(
            1, Instant.parse(at), JourneyId.of(id), WorkflowName.of(workflow), keys));
  }

  @Test
  void eventsApartFromTheirJourneyIdAndTimestampCompareEqual() {
    assertEquals(
        apartFromIdAndTime(started("a", "2026-10-28T02:00:00Z", "orders", Set.of("x"))),
        apartFromIdAndTime(started("b", "2026-10-28T03:00:00Z", "orders", Set.of("x"))));
  }

  static final List<Arguments> OTHERS =
      List.of(
          argumentSet("another workflow", "refunds", "x"),
          argumentSet("other initial keys", "orders", "y"));

  @ParameterizedTest
  @FieldSource("OTHERS")
  void anyOtherFieldTellsEventsApart(String workflow, String key) {
    assertNotEquals(
        apartFromIdAndTime(started("a", "2026-10-28T02:00:00Z", "orders", Set.of("x"))),
        apartFromIdAndTime(started("a", "2026-10-28T02:00:00Z", workflow, Set.of(key))));
  }
}
