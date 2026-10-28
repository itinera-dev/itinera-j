package org.itineradev.itinera.engine

import org.itineradev.itinera.step.Reason

/**
 * What a step's failing attempt left, which a step hook or a rescue step requests by its type,
 * optional or not.
 */
internal class Failed(val what: String, val asked: (Boolean) -> Asked)

/**
 * What a failing attempt left that [form] requests by its type, or `null` when it requests none.
 */
internal fun failedBy(form: WrittenRequest): Failed? =
    (form.type() as? ClassType)?.raw?.let(failedByType::get)

private val failedByType: Map<Class<*>, Failed> =
    mapOf(
        Reason::class.java to Failed("the failure's reason", ::ReasonAsked),
        Throwable::class.java to Failed("the error", ::ErrorAsked),
    )
