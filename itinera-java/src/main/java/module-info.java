/**
 * Itinera for Java: the core, and what makes its API read naturally from Java, for synchronous
 * workflows.
 */
module org.itineradev.itinera.java {
  requires transitive org.itineradev.itinera;

  exports org.itineradev.itinera.request.java;

  provides org.itineradev.itinera.engine.JavaForms with
      org.itineradev.itinera.request.java.OptionalForms;
}
