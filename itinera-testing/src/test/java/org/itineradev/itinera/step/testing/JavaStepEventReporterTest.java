package org.itineradev.itinera.step.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.itineradev.itinera.report.testing.Emitted;
import org.itineradev.itinera.report.testing.Level;
import org.junit.jupiter.api.Test;

final class JavaStepEventReporterTest {
  @Test
  void aJavaStepEventReporterKeepsWhatIsEmitted() {
    var recorder = new StepEventReporter();
    recorder.getReporter().warning("the gateway is slow");
    assertEquals(
        List.of(new Emitted(Level.WARNING, "the gateway is slow", null)), recorder.getEmitted());
  }

  @Test
  void aFailingJavaStepEventReporterInterruptsEveryCall() {
    var recorder = StepEventReporter.failing();
    assertThrows(RuntimeException.class, () -> recorder.getReporter().info("charging"));
    assertTrue(recorder.getEmitted().isEmpty());
  }
}
