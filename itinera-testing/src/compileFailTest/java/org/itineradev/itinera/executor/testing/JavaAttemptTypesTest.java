package org.itineradev.itinera.executor.testing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaAttemptTypesTest {
  @TempDir Path directory;

  @Test
  void aJavaScriptChoosesAnAttemptByAWholeNumber() throws IOException {
    var failing =
        new Javac(directory)
            .compile(
                """
                import org.itineradev.itinera.executor.testing.Script;

                final class Snippet {
                  Object refused(Script script) {
                    return script.step("charge").onAttempt(1.5);
                  }
                }
                """);
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(
        failing.messages().contains("possible lossy conversion from double to int"),
        failing.messages());
  }

  @Test
  void aJavaRecordReadsAnAttemptByAWholeNumber() throws IOException {
    var failing =
        new Javac(directory)
            .compile(
                """
                import org.itineradev.itinera.executor.testing.StepRecord;

                final class Snippet {
                  Object refused(StepRecord record) {
                    return record.attempt(1.5);
                  }
                }
                """);
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(
        failing.messages().contains("possible lossy conversion from double to int"),
        failing.messages());
  }
}
