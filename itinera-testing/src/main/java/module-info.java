/** Itinera's test support, which an application adds to its tests only. */
module org.itineradev.itinera.testing {
  requires transitive org.itineradev.itinera;
  requires static org.itineradev.itinera.kotlin;

  exports org.itineradev.itinera.report.testing;
}
