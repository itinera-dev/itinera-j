/** Itinera's test support, which an application adds to its tests only. */
module org.itineradev.itinera.testing {
  requires transitive org.itineradev.itinera;
  requires static org.itineradev.itinera.kotlin;
  requires static kotlinx.coroutines.core;

  exports org.itineradev.itinera.instance.testing;
  exports org.itineradev.itinera.journey.testing;
  exports org.itineradev.itinera.policy.testing;
  exports org.itineradev.itinera.report.testing;
  exports org.itineradev.itinera.step.testing;
  exports org.itineradev.itinera.testing;
}
