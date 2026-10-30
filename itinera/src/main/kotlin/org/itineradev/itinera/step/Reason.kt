package org.itineradev.itinera.step

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * Why a step failed or was skipped, or why a hook failed the journey: a short code a program can
 * compare, a message for people, and details. The details are a value, which itinera captures when
 * the reason is given, so changing the original afterwards changes nothing.
 *
 * @sample org.itineradev.itinera.step.samples.reasons
 */
@ExperimentalItineraApi
@JvmRecord
public data class Reason
@JvmOverloads
constructor(
    /** A short code a program can compare, such as `declined`. */
    val code: String,
    /** A message for people, if any. */
    val message: String? = null,
    /** Anything else the reason says, a value; `null` when it says nothing more. */
    val details: Any? = null,
)
