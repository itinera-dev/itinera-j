package org.itineradev.itinera.workflow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.itineradev.itinera.compiling.Javac;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JavaModeTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.service.ProviderDescriptor;
      import org.itineradev.itinera.workflow.Async;
      import org.itineradev.itinera.workflow.InputAdapterDescriptor;
      import org.itineradev.itinera.workflow.StepDescriptor;
      import org.itineradev.itinera.workflow.Sync;
      import org.itineradev.itinera.workflow.WorkflowBuilder;

      final class Snippet {
        void add(
            WorkflowBuilder<Snippet, Sync> shop,
            StepDescriptor<Async> charge,
            InputAdapterDescriptor<Snippet, Async> pricing,
            ProviderDescriptor<Snippet, Async> payments) {
          shop.%s;
        }
      }
      """;

  @TempDir Path directory;

  @ParameterizedTest
  @MethodSource("asynchronousParts")
  void aSynchronousJavaWorkflowTakesNoAsynchronousPart(String added, String message)
      throws IOException {
    var refused = new Javac(directory).compile(SNIPPET.formatted(added));
    assertFalse(refused.compiled(), refused.messages());
    assertTrue(refused.messages().contains(message), refused.messages());
  }

  static Stream<Arguments> asynchronousParts() {
    return Stream.of(
        Arguments.argumentSet(
            "an asynchronous step",
            "step(charge)",
            "StepDescriptor<Async> cannot be converted to StepDescriptor<Sync>"),
        Arguments.argumentSet(
            "an asynchronous input adapter",
            "inputAdapter(pricing)",
            "InputAdapterDescriptor<Snippet,Async> cannot be converted to"
                + " InputAdapterDescriptor<? super Snippet,Sync>"),
        Arguments.argumentSet(
            "an asynchronous service provider",
            "provider(payments)",
            "ProviderDescriptor<Snippet,Async> cannot be converted to"
                + " ProviderDescriptor<? super Snippet,Sync>"));
  }
}
