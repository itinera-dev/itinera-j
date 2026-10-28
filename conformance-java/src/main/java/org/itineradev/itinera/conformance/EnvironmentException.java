package org.itineradev.itinera.conformance;

/** The environment does not say what to run. */
final class EnvironmentException extends Exception {
  private static final long serialVersionUID = 1L;

  /** An environment that does not say what to run, for the reason {@code message} gives. */
  EnvironmentException(String message, Throwable cause) {
    super(message, cause);
  }
}
