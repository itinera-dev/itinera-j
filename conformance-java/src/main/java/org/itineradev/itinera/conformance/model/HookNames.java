package org.itineradev.itinera.conformance.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.itineradev.itinera.policy.StepHook;
import org.itineradev.itinera.policy.WorkflowHook;

/** Every hook, by the name the cases write. */
final class HookNames {
  static final Map<String, Hook> BY_NAME =
      Stream.concat(
              Arrays.stream(StepHook.values()).map(Hook.OfStep::new),
              Arrays.stream(WorkflowHook.values()).map(Hook.OfWorkflow::new))
          .collect(Collectors.toMap(Hook::toString, Function.identity()));

  private HookNames() {}
}
