/**
 * Itinera for Kotlin: asynchronous workflows, Kotlin classes and data classes, and the reified
 * forms.
 */
module org.itineradev.itinera.kotlin {
  requires transitive org.itineradev.itinera;
  requires kotlin.reflect;

  exports org.itineradev.itinera.journey.kotlin;
  exports org.itineradev.itinera.report.async;
  exports org.itineradev.itinera.value.kotlin;

  provides org.itineradev.itinera.engine.KotlinClasses with
      org.itineradev.itinera.value.kotlin.KotlinDataClasses;
}
