package org.itineradev.itinera.java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AsyncOutOfReachTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.executor.LocalExecutor;
      import org.itineradev.itinera.executor.async.AsyncLocalExecutor;
      import org.itineradev.itinera.instance.WorkflowInstance;
      import org.itineradev.itinera.workflow.Async;
      import org.itineradev.itinera.workflow.Sync;

      final class Snippet {
        void run(WorkflowInstance<Sync> sync, WorkflowInstance<Async> async) throws Exception {
          %s;
        }
      }
      """;

  @TempDir Path directory;

  @Test
  void javaCannotRunAnAsynchronousJourney() throws IOException {
    var javac = new Javac(directory);
    var refused = javac.compile(SNIPPET.formatted("new AsyncLocalExecutor().run(async)"));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(refused.messages().contains("cannot find symbol"), refused.messages());
    assertTrue(refused.messages().contains("method run"), refused.messages());
    var twin = javac.compile(SNIPPET.formatted("new LocalExecutor().run(sync)"));
    assertTrue(twin.compiled(), twin.messages());
  }
}
