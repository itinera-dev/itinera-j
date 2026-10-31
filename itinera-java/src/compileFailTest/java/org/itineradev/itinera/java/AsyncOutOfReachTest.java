package org.itineradev.itinera.java;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
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

  private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();

  @TempDir Path directory;

  @Test
  void javaCannotRunAnAsynchronousJourney() throws IOException {
    var refused = compile("new AsyncLocalExecutor().run(async)");
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(refused.messages().contains("cannot find symbol"), refused.messages());
    assertTrue(refused.messages().contains("method run"), refused.messages());
    var twin = compile("new LocalExecutor().run(sync)");
    assertTrue(twin.compiled(), twin.messages());
  }

  /** What javac made of a snippet. */
  private record Compiled(boolean compiled, String messages) {}

  private Compiled compile(String call) throws IOException {
    var source = Files.writeString(directory.resolve("Snippet.java"), SNIPPET.formatted(call));
    var messages = new StringWriter();
    try (var files = JAVAC.getStandardFileManager(null, null, null)) {
      var options =
          List.of("--release", "17", "-classpath", classPath(), "-d", directory.toString());
      var compiled =
          JAVAC
              .getTask(messages, files, null, options, null, files.getJavaFileObjects(source))
              .call();
      return new Compiled(compiled, messages.toString());
    }
  }

  private static String classPath() {
    return String.join(
        File.pathSeparator,
        Stream.of("java.class.path", "jdk.module.path")
            .map(System::getProperty)
            .filter(Objects::nonNull)
            .toList());
  }
}
