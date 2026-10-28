package org.itineradev.itinera.conformance.run;

import org.itineradev.itinera.conformance.model.ProviderModel;
import org.itineradev.itinera.conformance.model.RequestedService;
import org.itineradev.itinera.conformance.model.ServiceType;
import org.itineradev.itinera.request.Requests;
import org.itineradev.itinera.request.ServiceRequest;
import org.itineradev.itinera.service.ProviderBuilder;
import org.itineradev.itinera.workflow.Mode;

/** The services of the cases, their requests and the steps their providers serve. */
final class Services {
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
}
