package org.itineradev.itinera.conformance.run;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.itineradev.itinera.conformance.model.Input;
import org.itineradev.itinera.conformance.model.StepScript;
import org.itineradev.itinera.journey.Contributor;
import org.itineradev.itinera.request.BuildKind;
import org.itineradev.itinera.request.ContributorKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.ServiceRequest;
import org.itineradev.itinera.request.StepKind;
import org.itineradev.itinera.request.java.JavaRequests;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.StepBuilder;
import org.itineradev.itinera.workflow.Sync;

/**
 * What a step's script requests, and what the step does with what it receives before the rest of
 * its script: it uses each service once, changes the inputs the script says, and contributes what
 * the script says, first what every attempt contributes, then what this attempt does.
 */
final class Scripted {
  private final StepScript script;
  private final Map<String, Request<BuildKind, ?>> inputs = new LinkedHashMap<>();
  private final List<ServiceRequest<?>> services;
  private final Request<ContributorKind, Contributor> contributor = Requests.contributor();
  private final AtomicInteger built = new AtomicInteger();

  Scripted(StepScript script) {
    this.script = script;
    for (var input : script.inputs()) {
      inputs.put(input.key(), requested(input));
    }
    services = script.services().stream().<ServiceRequest<?>>map(Services::requestOf).toList();
  }

  /**
   * Declares the step's inputs on {@code builder}, then its services, each in the order written,
   * then its contributor.
   */
  StepBuilder<Sync> declaredOn(StepBuilder<Sync> builder) {
    inputs.values().forEach(builder::request);
    services.forEach(builder::request);
    builder.request(contributor);
    return builder;
  }

  /** What {@code got} gives the step for one attempt. */
  Received receivedFrom(Got<StepKind<Sync>> got) {
    var read = new LinkedHashMap<String, Object>();
    for (var input : inputs.entrySet()) {
      read.put(input.getKey(), Providing.answerOf(got.get(input.getValue())));
    }
    var lent = new LinkedHashMap<String, Object>();
    for (var service : services) {
      lent.put(service.getKey(), got.get(service));
    }
    return new Received(read, lent, got.get(contributor), built.getAndIncrement());
  }

  /**
   * Uses each service the step received, whose failure escapes, changes its inputs in place, then
   * contributes.
   */
  void act(Received received) {
    received.services().values().forEach(Services::use);
    script.inputChanges().forEach((key, to) -> changeInPlace(received.inputs().get(key), to));
    var contributing = received.contributor();
    for (var contribution : script.contributions()) {
      contributing.contribute(contribution.getKey(), contribution.getValue());
    }
    contributionsOf(received.builtBefore()).forEach(contributing::contribute);
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

  @SuppressWarnings("unchecked")
  private static void changeInPlace(Object received, Object to) {
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
