package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaRescueTypesTest {
  @TempDir Path directory;

  @Test
  void aJavaRescueStepNeverSkips() throws IOException {
    var refused =
        new Javac(directory)
            .compile(
                """
                import org.itineradev.itinera.step.RescueOutcome;

                final class Skipped implements RescueOutcome {}
                """);
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("class is not allowed to extend sealed class: RescueOutcome"),
        refused.messages());
  }

  @Test
  void aJavaRescueStepsRunNeverReturnsAStepsOutcome() throws IOException {
    var refused =
        new Javac(directory)
            .compile(
                """
                import org.itineradev.itinera.step.Outcome;
                import org.itineradev.itinera.workflow.RescueStepDescriptor;

                final class Snippet {
                  Object rescue() {
                    return RescueStepDescriptor.builder(got -> () -> Outcome.success());
                  }
                }
                """);
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("Outcome cannot be converted to RescueOutcome"),
        refused.messages());
  }
}
