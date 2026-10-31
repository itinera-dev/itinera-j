package org.itineradev.itinera.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaRequestTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.request.Got;
      import org.itineradev.itinera.request.Requests;
      import org.itineradev.itinera.request.StepKind;
      import org.itineradev.itinera.value.TypeToken;
      import org.itineradev.itinera.workflow.Sync;

      final class Snippet {
        void read(Got<StepKind<Sync>> got) {
          var amount = Requests.input("amount", TypeToken.of(Long.class));
          %s
        }
      }
      """;

  @TempDir Path directory;

  @Test
  void aJavaReadGivesTheTypeItsTokenDeclares() throws IOException {
    var javac = new Javac(directory);
    var refused = javac.compile(SNIPPET.formatted("String read = got.get(amount);"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(refused.messages().contains("incompatible types"), refused.messages());
    var twin = javac.compile(SNIPPET.formatted("Long read = got.get(amount);"));
    assertTrue(twin.compiled(), twin.messages());
  }
}
