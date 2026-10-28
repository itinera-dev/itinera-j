package org.itineradev.itinera.conformance.sentences

import io.cucumber.java.ParameterType
import org.itineradev.itinera.conformance.trace.Status

/** The parameter types the sentences share. */
internal class Parameters {
    @ParameterType("succeeded|failed|skipped|not executed|rescuing|aborted")
    fun status(written: String): Status = Status.named(written)
}
