package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.ParameterType;
import org.itineradev.itinera.conformance.trace.Status;

/** The parameter types the sentences share. */
public final class Parameters {
  @ParameterType("succeeded|failed|skipped|not executed|rescuing|aborted")
  public Status status(String written) {
    return Status.named(written);
  }
}
