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

final class JavaAnnotationTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.policy.ItineraStepPolicy;
      import org.itineradev.itinera.policy.ItineraWorkflowPolicy;
      import org.itineradev.itinera.policy.StepPolicy;
      import org.itineradev.itinera.policy.WorkflowPolicy;
      import org.itineradev.itinera.step.ItineraStep;
      import org.itineradev.itinera.step.Outcome;
      import org.itineradev.itinera.step.Step;
      import org.itineradev.itinera.workflow.ItineraWorkflow;
      import org.itineradev.itinera.workflow.WithStep;
      import org.itineradev.itinera.workflow.Workflow;

      final class Snippet {
        @Step
        public static final class Charge implements ItineraStep {
          public Outcome run() {
            return succeed();
          }
        }

        @StepPolicy(name = "audit")
        public static final class Audit implements ItineraStepPolicy {}

        @WorkflowPolicy(name = "tell")
        public static final class Tell implements ItineraWorkflowPolicy {}

        %s
        public static final class Shop implements ItineraWorkflow {}
      }
      """;

  @TempDir Path directory;

  @ParameterizedTest
  @MethodSource("misattached")
  void javaAttachesOnlyStepPoliciesToAStepAndWorkflowPoliciesToAWorkflow(
      String annotation, String message) throws IOException {
    var failing = new Javac(directory).compile(SNIPPET.formatted(annotation));
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(failing.messages().contains(message), failing.messages());
  }

  static Stream<Arguments> misattached() {
    return Stream.of(
        Arguments.argumentSet(
            "a workflow policy attached to a step",
            "@Workflow(name = \"shop\") "
                + "@WithStep(step = Charge.class, name = \"charge\", policies = Tell.class)",
            "Class<Tell> cannot be converted to Class<? extends ItineraStepPolicy>"),
        Arguments.argumentSet(
            "a step policy attached to a workflow",
            "@Workflow(name = \"shop\", policies = Audit.class) "
                + "@WithStep(step = Charge.class, name = \"charge\")",
            "Class<Audit> cannot be converted to Class<? extends ItineraWorkflowPolicy>"));
  }
}
