package org.itineradev.itinera.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaProviderTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.request.Got;
      import org.itineradev.itinera.request.JourneyProviderKind;
      import org.itineradev.itinera.request.Requests;
      import org.itineradev.itinera.workflow.Sync;

      final class Snippet {
        void provide(Got<JourneyProviderKind<Sync>> got) {
          %s
        }
      }
      """;

  @TempDir Path directory;

  @Test
  void aJavaProviderOfJourneyScopeCannotRequestTheAttempt() throws IOException {
    var refused = new Javac(directory).compile(SNIPPET.formatted("got.get(Requests.attempt());"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("cannot be converted to Request<? super JourneyProviderKind"),
        refused.messages());
  }

  @Test
  void aJavaProviderOfJourneyScopeCannotDeclareARequestOfTheAttempt() throws IOException {
    var refused =
        new Javac(directory)
            .compile(
                SNIPPET.formatted(
                    "org.itineradev.itinera.service.ProviderDescriptor.<Object, String>builder("
                        + "\"p\", \"k\", String.class, (workflow, answers) -> "
                        + "org.itineradev.itinera.service.Provided.noInstance())"
                        + ".request(Requests.attempt());"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("cannot be converted to Request<? super JourneyProviderKind"),
        refused.messages());
  }

  @Test
  void aJavaProviderOfJourneyScopeCannotRequestTheStepsName() throws IOException {
    var refused = new Javac(directory).compile(SNIPPET.formatted("got.get(Requests.stepName());"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("cannot be converted to Request<? super JourneyProviderKind"),
        refused.messages());
  }
}
