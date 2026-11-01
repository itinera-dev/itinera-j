package org.itineradev.itinera.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.itineradev.itinera.event.ReporterFailed;
import org.itineradev.itinera.value.TypeToken;
import org.junit.jupiter.api.Test;

final class JavaJourneyResultTest {
  record Receipt(String order, long total) {}

  @Test
  void javaReadsWhyAnAbortedJourneyHasNoOutput() {
    var aborted =
        new JourneyResult(
            JourneyId.of("order-42"),
            new JourneyStatus.Aborted(new Abort(new ReporterFailed(null, "full"), null)));
    var thrown =
        assertThrows(OutputException.class, () -> aborted.output(TypeToken.of(Receipt.class)));
    assertEquals(OutputProblem.Aborted.INSTANCE, thrown.getProblem());
    assertEquals("The journey was aborted, so it has no data bag to read.", thrown.getMessage());
  }
}
