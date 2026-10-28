package org.itineradev.itinera.conformance.run;

/** What a scripted reporter throws. */
final class ScriptedReporterFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;

  ScriptedReporterFailure(String message) {
    super(message);
  }
}
