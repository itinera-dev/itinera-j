package org.itineradev.itinera.conformance.run;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.itineradev.itinera.conformance.model.ModelException;
import org.itineradev.itinera.conformance.model.ProviderAction;
import org.itineradev.itinera.conformance.model.ProviderFailure;
import org.itineradev.itinera.conformance.model.ProviderModel;
import org.itineradev.itinera.conformance.model.ProviderRequest;
import org.itineradev.itinera.conformance.model.Supplies;
import org.itineradev.itinera.journey.JourneyId;
import org.itineradev.itinera.request.AttemptProviderKind;
import org.itineradev.itinera.request.Got;
import org.itineradev.itinera.request.JourneyProviderKind;
import org.itineradev.itinera.request.Request;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.java.JavaRequests;
import org.itineradev.itinera.service.Provided;
import org.itineradev.itinera.service.ProviderBuilder;
import org.itineradev.itinera.service.ProviderReporter;
import org.itineradev.itinera.value.TypeToken;
import org.itineradev.itinera.workflow.Sync;

/**
 * What a scripted provider does when called, through requests of kind {@code K}: the witness
 * records what it received, it does what its actions say through its reporter, and it fails,
 * returns no instance or makes one, as its script says.
 */
final class Providing<K extends JourneyProviderKind<Sync>> {
  private final ProviderModel provider;
  private final Witness witness;
  private final Request<? super K, JourneyId> journeyId = Requests.journeyId();
  private final Request<? super K, ProviderReporter> reporter = Requests.providerReporter();
  private final Map<ProviderRequest, Request<? super K, ?>> requests;
  private final Map<JourneyId, Integer> made = new HashMap<>();

  private Providing(
      ProviderModel provider,
      Witness witness,
      Map<ProviderRequest, Request<? super K, ?>> requests) {
    this.provider = provider;
    this.witness = witness;
    this.requests = requests;
  }

  /** Calls of a provider of journey scope scripted as {@code provider}. */
  static Providing<JourneyProviderKind<Sync>> ofJourney(ProviderModel provider, Witness witness) {
    return new Providing<>(provider, witness, requestsOf(provider, Providing::journeyRequestOf));
  }

  /** Calls of a provider of attempt scope scripted as {@code provider}. */
  static Providing<AttemptProviderKind<Sync>> ofAttempt(ProviderModel provider, Witness witness) {
    return new Providing<>(provider, witness, requestsOf(provider, Providing::attemptRequestOf));
  }

  private static <K> Map<ProviderRequest, Request<? super K, ?>> requestsOf(
      ProviderModel provider, Function<ProviderRequest, Request<? super K, ?>> requestOf) {
    var requests = new LinkedHashMap<ProviderRequest, Request<? super K, ?>>();
    for (var request : provider.requests()) {
      requests.put(request, requestOf.apply(request));
    }
    return requests;
  }

  /** Declares what the provider requests on {@code builder}, in the order written. */
  <W> ProviderBuilder<W, Sync, K> declaredOn(ProviderBuilder<W, Sync, K> builder) {
    builder.request(journeyId);
    builder.request(reporter);
    requests.values().forEach(builder::request);
    return builder;
  }

  /** What the provider gives for one call, given {@code got}, making instances of {@code type}. */
  <T> Provided<T> provided(Got<K> got, Class<T> type) {
    var received = new LinkedHashMap<ProviderRequest, Object>();
    for (var request : requests.entrySet()) {
      received.put(request.getKey(), answerOf(got.get(request.getValue())));
    }
    witness.called(provider.name(), received);
    perform(got.get(reporter));
    var failure = provider.failure();
    if (failure != null && failsOn(failure, received)) {
      throw new IllegalStateException(failure.message());
    }
    if (provider.returnsNothing()) {
      return Provided.noInstance();
    }
    if (provider.returnsNull()) {
      return null;
    }
    int count = made.merge(got.get(journeyId), 1, Integer::sum);
    return new Provided.Instance<>(
        type.cast(Services.madeBy(provider, labelOf(received, count))), dataOf(provider));
  }

  private void perform(ProviderReporter reporter) {
    for (var action : provider.actions()) {
      if (action instanceof ProviderAction.Emits emits) {
        emit(reporter, emits);
      } else {
        var sets = (ProviderAction.SetsGlobalMetadata) action;
        reporter.setGlobalMetadata(sets.key(), sets.value());
      }
    }
  }

  private static void emit(ProviderReporter reporter, ProviderAction.Emits emits) {
    switch (emits.kind()) {
      case JOURNEY_INFO -> reporter.info(emits.message(), emits.data());
      case JOURNEY_WARNING -> reporter.warning(emits.message(), emits.data());
      case JOURNEY_ERROR -> reporter.error(emits.message(), emits.data());
    }
  }

  private String labelOf(Map<ProviderRequest, Object> received, int count) {
    var key = provider.labelledBy();
    return key == null ? provider.name() + " " + count : String.valueOf(receivedFor(received, key));
  }

  /** What the provider received for its request of data from the workflow under {@code key}. */
  static Object receivedFor(Map<ProviderRequest, Object> received, String key) {
    return received.entrySet().stream()
        .filter(entry -> isFromWorkflow(entry.getKey(), key))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  /** What a request was answered, with an empty {@code Optional} as absent. */
  static Object answerOf(Object answer) {
    return answer instanceof Optional<?> optional ? optional.orElse(null) : answer;
  }

  private static boolean isFromWorkflow(ProviderRequest request, String key) {
    return request instanceof ProviderRequest.FromWorkflow fromWorkflow
        && fromWorkflow.key().equals(key);
  }

  private static boolean failsOn(ProviderFailure failure, Map<ProviderRequest, Object> received) {
    return failure.key() == null || failure.received().equals(receivedFor(received, failure.key()));
  }

  private static Object dataOf(ProviderModel provider) {
    var supplies = provider.supplies();
    if (supplies instanceof Supplies.Data data) {
      return data.value();
    }
    return supplies instanceof Supplies.NonValue ? NotAValue.INSTANCE : null;
  }

  private static Request<? super JourneyProviderKind<Sync>, ?> journeyRequestOf(
      ProviderRequest request) {
    if (request instanceof ProviderRequest.JourneyId) {
      return Requests.journeyId();
    }
    if (request instanceof ProviderRequest.FromWorkflow fromWorkflow) {
      return fromWorkflowRequestOf(fromWorkflow);
    }
    throw new ModelException(
        "a provider of journey scope requesting the "
            + request.getClass().getSimpleName()
            + " does not compile: Java's types refuse it");
  }

  private static Request<? super AttemptProviderKind<Sync>, ?> attemptRequestOf(
      ProviderRequest request) {
    if (request instanceof ProviderRequest.StepName) {
      return Requests.stepName();
    }
    if (request instanceof ProviderRequest.Attempt) {
      return Requests.attempt();
    }
    return journeyRequestOf(request);
  }

  private static Request<? super JourneyProviderKind<Sync>, ?> fromWorkflowRequestOf(
      ProviderRequest.FromWorkflow request) {
    var type = TypeToken.of(request.type().type());
    return request.optional()
        ? JavaRequests.optionalFromWorkflow(request.key(), type)
        : Requests.fromWorkflow(request.key(), type);
  }
}
