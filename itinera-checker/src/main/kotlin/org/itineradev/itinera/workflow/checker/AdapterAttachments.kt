package org.itineradev.itinera.workflow.checker

/**
 * The mistakes in how the input adapters of [workflow] are attached, to [steps]: each name several
 * share, each adapter attached to no step, each step several are attached to, and each adapter
 * attached to a step the workflow does not have, once.
 */
internal fun adapterMistakes(workflow: ReadWorkflow, steps: Set<String>): List<Finding> {
    val origin = workflow.origin
    val (known, unknown) =
        workflow.adapters.flatMap(::adapted).partition { adapted -> isKnown(adapted, steps) }
    return shared(workflow.adapters.map(ReadAdapter::name)).map { name ->
        duplicateAdapterName(name, origin)
    } +
        workflow.adapters.filter(::isAttachedToNoStep).map { adapter ->
            attachedToNoStep(adapter, origin)
        } +
        known.groupBy(Adapted::step, Adapted::adapter).filterValues(::isSharedList).map {
            (step, adapters) ->
            adaptedTwice(step, adapters, origin)
        } +
        unknown.distinct().map { adapted -> adapterForUnknownStep(adapted, origin) }
}

/** One step an adapter is attached to, once for each time it is. */
private data class Adapted(val step: String, val adapter: String)

private fun adapted(adapter: ReadAdapter): List<Adapted> =
    adapter.steps.map { step -> adaptedTo(step, adapter) }

private fun adaptedTo(step: String, adapter: ReadAdapter): Adapted = Adapted(step, adapter.name)

private fun isKnown(adapted: Adapted, steps: Set<String>): Boolean = adapted.step in steps

private fun isAttachedToNoStep(adapter: ReadAdapter): Boolean = adapter.steps.isEmpty()

private fun duplicateAdapterName(name: String, origin: Any): Finding =
    Named("duplicate adapter name", "two input adapters are named $name", origin)

private fun attachedToNoStep(adapter: ReadAdapter, origin: Any): Finding =
    Misdeclared("input adapter ${adapter.name}", "is attached to no step", origin)

private fun adaptedTwice(step: String, adapters: List<String>, origin: Any): Finding =
    Named(
        "step adapted twice",
        "step $step has several input adapters, ${adapters.joinToString(", ")}",
        origin,
    )

private fun adapterForUnknownStep(adapted: Adapted, origin: Any): Finding =
    Named(
        "input adapter for unknown step",
        "input adapter ${adapted.adapter} names step ${adapted.step}, which the workflow does not " +
            "have",
        origin,
    )
