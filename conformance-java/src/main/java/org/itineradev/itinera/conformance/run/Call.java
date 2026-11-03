package org.itineradev.itinera.conformance.run;

/**
 * A call a step or a hook run alone made on its reporter.
 *
 * @param method the method called
 * @param key its first argument: the message of an event or the key of an entry
 * @param interrupted whether it was interrupted
 */
public record Call(String method, String key, boolean interrupted) {
  /** The method that sets an entry of global metadata. */
  public static final String SETTING = "setGlobalMetadata";
}
