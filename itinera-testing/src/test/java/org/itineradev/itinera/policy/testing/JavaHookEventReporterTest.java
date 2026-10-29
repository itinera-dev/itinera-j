package org.itineradev.itinera.policy.testing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class JavaHookEventReporterTest {
  @Test
  void aJavaHookEventReporterKeepsTheGlobalMetadataSet() {
    var recorder = new HookEventReporter();
    recorder.getReporter().setGlobalMetadata("audited", true);
    assertEquals(Map.of("audited", true), recorder.getGlobalMetadata());
  }

  @Test
  void aFailingJavaHookEventReporterInterruptsEveryCall() {
    var recorder = HookEventReporter.failing();
    assertThrows(RuntimeException.class, () -> recorder.getReporter().info("auditing"));
    assertTrue(recorder.getGlobalMetadata().isEmpty());
  }
}
