package org.itineradev.itinera.step.testing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaRescueEndingTypesTest {
  @TempDir Path directory;

  @Test
  void aJavaRescueStepMockNeverPlaysASkip() throws IOException {
    var failing =
        new Javac(directory)
            .compile(
                """
                import org.itineradev.itinera.step.testing.Ending;
                import org.itineradev.itinera.step.testing.RescueStepMock;

                final class Snippet {
                  RescueStepMock refused = RescueStepMock.playing(Ending.skipped());
                }
                """);
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(
        failing.messages().contains("Ending cannot be converted to RescueEnding"),
        failing.messages());
  }
}
