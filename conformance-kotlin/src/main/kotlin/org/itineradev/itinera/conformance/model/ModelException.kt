package org.itineradev.itinera.conformance.model

/**
 * A scenario's sentences do not make a model: an error in the case, such as a step that is not in
 * the workflow, reported as such rather than as a failure of itinera.
 */
internal class ModelException(message: String, cause: Throwable? = null) :
    RuntimeException("The case is wrong: $message", cause)
