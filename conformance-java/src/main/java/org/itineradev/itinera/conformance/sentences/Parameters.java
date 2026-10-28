package org.itineradev.itinera.conformance.sentences;

import io.cucumber.java.ParameterType;
import org.itineradev.itinera.conformance.model.Implementation;
import org.itineradev.itinera.conformance.trace.Status;

/** The parameter types the sentences share. */
public final class Parameters {
  @ParameterType("succeeded|failed|skipped|not executed|rescuing|aborted")
  public Status status(String written) {
    return Status.named(written);
  }

  @ParameterType("first labeller|second labeller")
  public Implementation implementation(String written) {
    return Implementation.named(written);
  }
}
