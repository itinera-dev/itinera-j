package org.itineradev.itinera.conformance.run;

/**
 * What a Then sentence expected and itinera did not do: a failure of the implementation, never an
 * error in the case.
 */
public final class Unmet extends AssertionError {
  private static final long serialVersionUID = 1L;

  /** A failure saying what was expected and found. */
  public Unmet(String message) {
    super(message);
  }
}
