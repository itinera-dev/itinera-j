package org.itineradev.itinera.conformance.model;

/** How building a scripted policy goes. */
public sealed interface PolicyBuild {
  /** It is built. */
  record Built() implements PolicyBuild {}

  /**
   * Building it fails.
   *
   * @param message its message, {@code null} when the case writes none
   */
  record Fails(String message) implements PolicyBuild {}
}
