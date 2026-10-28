package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaAdapterTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.journey.DataBagAccess;
      import org.itineradev.itinera.request.Got;
      import org.itineradev.itinera.request.InputAdapterKind;
      import org.itineradev.itinera.request.Requests;

      final class Snippet {
        void adapt(Got<InputAdapterKind> got, DataBagAccess access) {
          %s
        }
      }
      """;

  @TempDir Path directory;

  @Test
  void aJavaAdaptersAccessToTheDataBagHasNoMethodThatWrites() throws IOException {
    var refused = new Javac(directory).compile(SNIPPET.formatted("access.put(\"amount\", 7L);"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(refused.messages().contains("cannot find symbol"), refused.messages());
  }

  @Test
  void aJavaInputAdapterRequestsOnlyWhatItsKindMay() throws IOException {
    var refused =
        new Javac(directory).compile(SNIPPET.formatted("got.get(Requests.contributor());"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(
        refused.messages().contains("cannot be converted to Request<? super InputAdapterKind"),
        refused.messages());
  }
}
