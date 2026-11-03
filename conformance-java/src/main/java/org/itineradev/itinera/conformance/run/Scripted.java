package org.itineradev.itinera.conformance.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.itineradev.itinera.conformance.model.HookRequest;
import org.itineradev.itinera.conformance.model.Input;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.RescuedStepRequest;
import org.itineradev.itinera.conformance.model.StepScript;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.BuildKind;
import org.itineradev.itinera.request.ContributorKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.RescueKind;
import org.itineradev.itinera.request.ServiceRequest;
import org.itineradev.itinera.request.StepKind;
import org.itineradev.itinera.request.StepReporterKind;
import org.itineradev.itinera.request.java.JavaRequests;
import org.itineradev.itinera.step.StepReporter;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.RescueStepBuilder;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.Sync;

/**
 * What a step's or a rescue step's script requests, and what the step does with what it receives
 * before the rest of its script: it uses each service once, changes the inputs the script says,
 * then acts as the script says, first what every attempt does, in the order written, then what this
 * attempt contributes.
 */
final class Scripted {
  private final StepScript script;
  private final Map<String, Request<BuildKind, ?>> inputs = new LinkedHashMap<>();
  private final List<ServiceRequest<?>> services;
  private final Map<HookRequest, Request<? super RescueKind<Sync>, ?>> ofRescuedStep;
  private final Request<ContributorKind, Contributor> contributor = Requests.contributor();
  private final Request<StepReporterKind<Sync>, StepReporter> reporter = Requests.stepReporter();
  private final AtomicInteger built = new AtomicInteger();

  Scripted(StepScript script) {
    this.script = script;
    for (var input : script.inputs()) {
      inputs.put(input.key(), requested(input));
    }
    services = script.services().stream().<ServiceRequest<?>>map(Services::requestOf).toList();
    ofRescuedStep =
        script.ofRescuedStep().stream()
            .collect(
                Collectors.toMap(
                    RescuedStepRequest::request,
                    Scripted::rescuedRequested,
                    (first, again) -> first,
                    LinkedHashMap::new));
  }

  /**
   * Declares the step's inputs on {@code builder}, then its services, each in the order written,
   * then its contributor and its step reporter.
   */
  StepBuilder<Sync> declaredOn(StepBuilder<Sync> builder) {
    inputs.values().forEach(builder::request);
    afterInputs().forEach(builder::request);
    return builder;
  }

  /**
   * Declares the rescue step's inputs and its requests of the step it rescues on {@code builder},
   * together in the order written, then its services, its contributor and its step reporter.
   */
  RescueStepBuilder<Sync> declaredOn(RescueStepBuilder<Sync> builder) {
    writtenForRescue().forEach(builder::request);
    afterInputs().forEach(builder::request);
    return builder;
  }

  private List<Request<? super StepKind<Sync>, ?>> afterInputs() {
    var declared = new ArrayList<Request<? super StepKind<Sync>, ?>>(services);
    if (!script.declaresNoHandles()) {
      declared.add(contributor);
      declared.add(reporter);
    }
    return declared;
  }

  private List<Request<? super RescueKind<Sync>, ?>> writtenForRescue() {
    var written = List.copyOf(inputs.values());
    return Stream.concat(
            IntStream.range(0, written.size()).mapToObj(index -> placedInput(written, index)),
            script.ofRescuedStep().stream().map(this::placedRescued))
        .sorted(Comparator.comparingInt(Placed::place))
        .<Request<? super RescueKind<Sync>, ?>>map(Placed::request)
        .toList();
  }

  private static Placed placedInput(List<Request<BuildKind, ?>> written, int index) {
    return new Placed(2 * index + 1, written.get(index));
  }

  private Placed placedRescued(RescuedStepRequest written) {
    return new Placed(2 * written.inputsBefore(), ofRescuedStep.get(written.request()));
  }

  private static Request<? super RescueKind<Sync>, ?> rescuedRequested(RescuedStepRequest written) {
    return ScriptedHook.ofKind(ScriptedHook.requestOf(written.request()));
  }

  /**
   * A rescue step's {@code request} at its {@code place} in the order written: a request of the
   * step it rescues written before input n is at 2n, and input n at 2n + 1.
   */
  private record Placed(int place, Request<? super RescueKind<Sync>, ?> request) {}

  /** What {@code got} gives the step for one attempt. */
  Received receivedFrom(Got<StepKind<Sync>> got) {
    return received(got, Map.of());
  }

  /** What {@code got} gives the rescue step for its attempt. */
  Received rescueReceivedFrom(Got<RescueKind<Sync>> got) {
    var rescued = new LinkedHashMap<HookRequest, Object>();
    for (var request : ofRescuedStep.entrySet()) {
      rescued.put(request.getKey(), Providing.answerOf(got.get(request.getValue())));
    }
    return received(got, Collections.unmodifiableMap(rescued));
  }

  private <K extends StepKind<Sync>> Received received(
      Got<K> got, Map<HookRequest, Object> ofRescuedStep) {
    if (script.declaresNoHandles()) {
      throw new ModelException("the cases never build a step that declares no handles");
    }
    var read = new LinkedHashMap<String, Object>();
    for (var input : inputs.entrySet()) {
      read.put(input.getKey(), Providing.answerOf(got.get(input.getValue())));
    }
    var lent = new LinkedHashMap<String, Object>();
    for (var service : services) {
      lent.put(service.getKey(), got.get(service));
    }
    return new Received(
        read,
        lent,
        got.get(contributor),
        got.get(reporter),
        built.getAndIncrement(),
        ofRescuedStep);
  }

  /** Whether the step's factory returns {@code null}, not a step. */
  boolean builtAsNull() {
    return script.builtAsNull();
  }

  /** Whether the step's run returns {@code null}, not an outcome. */
  boolean returnsNoOutcome() {
    return script.returnsNoOutcome();
  }

  /**
   * Uses each service the step received, whose failure escapes, changes its inputs in place, then
   * acts.
   */
  void act(Received received) {
    received.services().values().forEach(Services::use);
    script.inputChanges().forEach((key, to) -> changeInPlace(received.inputs().get(key), to));
    for (var action : script.actions()) {
      Acting.perform(action, received, script.ignoresEmitFailures());
    }
    contributionsOf(received.builtBefore()).forEach(received.contributor()::contribute);
  }

  /**
   * What the step built after {@code builtBefore} others contributes, as its row says, or as the
   * last row says for a step built past them.
   */
  private Map<String, Object> contributionsOf(int builtBefore) {
    var rows = script.attempts();
    if (rows == null) {
      return Map.of();
    }
    return rows.get(Math.min(builtBefore, rows.size() - 1)).contributes();
  }

  private static Request<BuildKind, ?> requested(Input input) {
    var type = TypeToken.of(input.type().type());
    return input.optional()
        ? JavaRequests.optionalInput(input.key(), type)
        : Requests.input(input.key(), type);
  }

  /** Changes {@code received}, a list or a map, to the content of {@code to}, in place. */
  @SuppressWarnings("unchecked")
  static void changeInPlace(Object received, Object to) {
    if (received instanceof List<?> list && to instanceof List<?> changed) {
      var inPlace = (List<Object>) list;
      inPlace.clear();
      inPlace.addAll(changed);
    }
    if (received instanceof Map<?, ?> map && to instanceof Map<?, ?> changed) {
      var inPlace = (Map<Object, Object>) map;
      inPlace.clear();
      inPlace.putAll(changed);
    }
  }
}
