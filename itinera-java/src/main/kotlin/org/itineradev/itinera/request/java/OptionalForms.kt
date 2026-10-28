package org.itineradev.itinera.request.java

import java.util.Optional
import org.itineradev.itinera.engine.Answer
import org.itineradev.itinera.engine.JavaForms

/** Gives an optional request written as `Optional<T>` an `Optional` of what was resolved. */
internal class OptionalForms : JavaForms {
    override val optional: Answer = presentOrEmpty
}

/** An `Optional` of what was resolved, empty when it was answered absent. */
internal val presentOrEmpty: Answer = Answer(Optional<Any>::ofNullable)
