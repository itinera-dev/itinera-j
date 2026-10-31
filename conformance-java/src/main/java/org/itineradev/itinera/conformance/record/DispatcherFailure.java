package org.itineradev.itinera.conformance.record;

/** What a scripted dispatcher or factory throws. */
public final class DispatcherFailure extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** A failure saying {@code message}. */
  public DispatcherFailure(String message) {
    super(message);
  }
}
