package org.itineradev.itinera.policy;

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

final class JavaPolicyTypesTest {
  private static final String SNIPPET =
      """
      import java.util.List;
      import org.itineradev.itinera.policy.OnSuccess;
      import org.itineradev.itinera.policy.Role;
      import org.itineradev.itinera.policy.StepPolicyDescriptor;
      import org.itineradev.itinera.request.Requests;
      import org.itineradev.itinera.workflow.WorkflowDescriptor;

      final class Snippet {
        @Role
        interface Messages {
          void tell(String message);
        }

        static final class Shop {}

        static final class Audit {}

        void declare() {
          var audit = StepPolicyDescriptor.builder("audit", Audit::new);
          %s
        }
      }
      """;

  @TempDir Path directory;

  @ParameterizedTest
  @MethodSource("refusals")
  void javaDoesNotCompileWhatThePolicyTypesRefuse(String refused, String message)
      throws IOException {
    var javac = new Javac(directory);
    var failing = javac.compile(SNIPPET.formatted(refused));
    assertFalse(failing.compiled(), failing.messages());
    assertTrue(failing.messages().contains(message), failing.messages());
  }

  static Stream<Arguments> refusals() {
    return Stream.of(
        Arguments.argumentSet(
            "a failure hook that finishes the workflow",
            "audit.onStepFailure((policy, got) -> OnSuccess.finishWorkflow());",
            "OnSuccess cannot be converted to OnFailure"),
        Arguments.argumentSet(
            "the failure's cause in on step success",
            "audit.onStepSuccess(List.of(Requests.failureCause()), (policy, got) -> OnSuccess.keepDefault());",
            "lower bounds: Request<FailureCauseKind,FailureCause>"),
        Arguments.argumentSet(
            "a role the workflow's objects do not implement",
            "WorkflowDescriptor.builder(Shop.class, \"shop\").role(Messages.class);",
            "Class<Messages> cannot be converted to Class<? super Shop>"));
  }
}
