package org.itineradev.itinera.workflow

import org.itineradev.itinera.ExperimentalItineraApi

/**
 * A workflow refused before any journey: thrown when it is described or built, with every
 * [Violation] found, not only the first. No instance of it exists, so no journey ID.
 *
 * @sample org.itineradev.itinera.workflow.samples.violations
 */
@ExperimentalItineraApi
public class Violations(
    /** Every violation found, in the order found. */
    public val violations: List<Violation>
) : IllegalArgumentException(violations.joinToString("; ", "The workflow is refused: "))
