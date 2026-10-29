/**
 * Itinera for Kotlin: asynchronous workflows, Kotlin classes and data classes, and the reified
 * forms.
 */
module org.itineradev.itinera.kotlin {
  requires transitive org.itineradev.itinera;
  requires kotlin.reflect;
  requires kotlinx.coroutines.core;

  exports org.itineradev.itinera.executor.async;
  exports org.itineradev.itinera.instance.async;
  exports org.itineradev.itinera.journey.kotlin;
  exports org.itineradev.itinera.policy.async;
  exports org.itineradev.itinera.report.async;
  exports org.itineradev.itinera.request.async;
  exports org.itineradev.itinera.request.kotlin;
  exports org.itineradev.itinera.service.async;
  exports org.itineradev.itinera.step.async;
  exports org.itineradev.itinera.value.kotlin;
  exports org.itineradev.itinera.workflow.async;
  exports org.itineradev.itinera.workflow.kotlin;

  provides org.itineradev.itinera.engine.KotlinClasses with
      org.itineradev.itinera.value.kotlin.KotlinDataClasses;
}
