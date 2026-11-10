package org.itineradev.itinera.conformance.model

/**
 * The checks of a case that need the whole model, such as one comparing a building block with the
 * workflow it is bound to, made once every Given sentence is written.
 */
internal class CaseChecks {
    private val checks = mutableListOf<(Model) -> Unit>()

    /** Adds a check, made after those added before. */
    fun add(check: (Model) -> Unit) {
        checks += check
    }

    /** Makes every check on [model], the whole model of the case. */
    fun make(model: Model) {
        for (check in checks) check(model)
    }
}
