package org.itineradev.itinera.policy.testing;

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

final class JavaHookEndingTypesTest {
  private static final String SNIPPET =
      """
      import org.itineradev.itinera.policy.testing.HookEnding;
      import org.itineradev.itinera.policy.testing.StepPolicyScript;
      import org.itineradev.itinera.policy.testing.WorkflowPolicyScript;

      final class Snippet {
        void script() {
          var audit = StepPolicyScript.keepingDefault();
          var notify = WorkflowPolicyScript.returning();
          %s
        }
      }
      """;

  @TempDir Path directory;

  @ParameterizedTest
  @MethodSource("refusals")
  void aJavaHookOfAPolicyMockEndsOnlyAsTheHookItselfMay(String refused, String message)
      throws IOException {
    var failing = new Javac(directory).compile(SNIPPET.formatted(refused));
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(failing.messages().contains(message), failing.messages());
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        Arguments.argumentSet(
            "a failure hook that finishes the workflow",
            "audit.onStepFailure(HookEnding.finishWorkflow());",
            "HookEnding cannot be converted to FailureHookEnding"),
        Arguments.argumentSet(
            "a workflow hook that fails the workflow",
            "notify.onWorkflowSuccess(HookEnding.failWorkflow(\"late\"));",
            "FailureHookEnding cannot be converted to WorkflowHookEnding"));
  }
}
