package org.itineradev.itinera.conformance.run;

/**
 * What a case scripts and the runner cannot run yet, because the stage of itinera that makes it run
 * has not landed.
 */
public final class NotRunYet extends UnsupportedOperationException {
  private static final long serialVersionUID = 1L;

  /** Says the runner cannot run {@code what} yet. */
  public NotRunYet(String what) {
    super("the runner cannot run " + what + " yet");
  }
}
