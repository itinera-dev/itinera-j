/**
 * Itinera's core: everything a synchronous workflow needs, written in Kotlin and usable from Java.
 */
@SuppressWarnings("module")
module org.itineradev.itinera {
  requires transitive kotlin.stdlib;

  exports org.itineradev.itinera;
  exports org.itineradev.itinera.journey;
  exports org.itineradev.itinera.value;
  exports org.itineradev.itinera.engine to
      org.itineradev.itinera.kotlin;

  uses org.itineradev.itinera.engine.KotlinClasses;
}
