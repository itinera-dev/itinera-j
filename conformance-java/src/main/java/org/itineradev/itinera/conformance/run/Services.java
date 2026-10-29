package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.Implementation;
import org.itineradev.itinera.conformance.model.ProviderModel;
import org.itineradev.itinera.conformance.model.RequestedService;
import org.itineradev.itinera.conformance.model.ServiceType;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.ServiceRequest;
import org.itineradev.itinera.service.ProviderBuilder;
import org.itineradev.itinera.workflow.Mode;

/** The services of the cases, their requests and the steps their providers serve. */
public final class Services {
  private Services() {}

  /** The interface a service of {@code type} has in the runner. */
  static Class<?> classOf(ServiceType type) {
    return switch (type) {
      case LABELLER -> Labeller.class;
      case METER -> Meter.class;
    };
  }

  /** The request a step or rescue step makes for {@code service}. */
  static ServiceRequest<?> requestOf(RequestedService service) {
    return Requests.service(service.key(), classOf(service.type()));
  }

  /** The provider attached to the steps {@code provider} serves only, each as often as named. */
  static <W, M extends Mode, K> ProviderBuilder<W, M, K> servingOnly(
      ProviderBuilder<W, M, K> builder, ProviderModel provider) {
    provider.steps().forEach(builder::step);
    return builder;
  }

  /** The instance {@code provider} makes, labelled {@code label}. */
  static Object madeBy(ProviderModel provider, String label) {
    return switch (provider.type()) {
      case LABELLER -> labellerOf(provider.implementation(), label, provider.servicesFail());
      case METER -> (Meter) () -> 0;
    };
  }

  /** An instance of {@code implementation} labelled {@code label}, failing with {@code failure}. */
  static Labeller labellerOf(Implementation implementation, String label, String failure) {
    return switch (implementation) {
      case FIRST -> new MadeLabeller.First(label, failure);
      case SECOND -> new MadeLabeller.Second(label, failure);
    };
  }

  /** How a case writes the implementation {@code service_provided} names. */
  public static String writtenImplementation(String implementation) {
    if (implementation.equals(MadeLabeller.First.class.getName())) {
      return Implementation.FIRST.written();
    }
    if (implementation.equals(MadeLabeller.Second.class.getName())) {
      return Implementation.SECOND.written();
    }
    return implementation;
  }

  /** Uses {@code service} once, as a step does when it runs; its failure escapes. */
  static void use(Object service) {
    if (service instanceof Labeller labeller) {
      labeller.label();
    }
    if (service instanceof Meter meter) {
      meter.reading();
    }
  }
}
