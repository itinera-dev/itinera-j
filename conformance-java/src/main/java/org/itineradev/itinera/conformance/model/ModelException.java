package org.itineradev.itinera.conformance.model;

/**
 * A scenario's sentences do not make a model: an error in the case, such as a step that is not in
 * the workflow, reported as such rather than as a failure of itinera.
 */
public final class ModelException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** An error in the case, which {@code message} describes. */
  public ModelException(String message) {
    this(message, null);
  }

  /** An error in the case, which {@code message} describes, found through {@code cause}. */
  public ModelException(String message, Throwable cause) {
    super("The case is wrong: " + message, cause);
  }
}
