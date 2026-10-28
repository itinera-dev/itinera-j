/**
 * Itinera's core: everything a synchronous workflow needs, written in Kotlin and usable from Java.
 */
@SuppressWarnings("module")
module org.itineradev.itinera {
  requires transitive kotlin.stdlib;

  exports org.itineradev.itinera;
  exports org.itineradev.itinera.event;
  exports org.itineradev.itinera.executor;
  exports org.itineradev.itinera.instance;
  exports org.itineradev.itinera.journey;
  exports org.itineradev.itinera.policy;
  exports org.itineradev.itinera.report;
  exports org.itineradev.itinera.service;
  exports org.itineradev.itinera.step;
  exports org.itineradev.itinera.value;
  exports org.itineradev.itinera.workflow;
  exports org.itineradev.itinera.engine to
      org.itineradev.itinera.kotlin;

  uses org.itineradev.itinera.engine.KotlinClasses;
}
