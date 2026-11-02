package org.itineradev.itinera.conformance.model

/** One thing a scripted step does on every attempt before it ends, as a case writes it. */
internal sealed interface StepAction {
    /** It contributes [value] under [key]. */
    data class Contributes(val key: String, val value: Any?) : StepAction

    /** It emits [kind] with [message] and [data], `null` when the case gives none. */
    data class Emits(val kind: Emitted, val message: String, val data: Any?) : StepAction

    /** It sets the global metadata [key] to [value]. */
    data class SetsGlobalMetadata(val key: String, val value: Any?) : StepAction
}

/** The events a step emits through its step reporter, as a case names them. */
internal enum class Emitted(val written: String) {
    STEP_INFO("step_info"),
    STEP_WARNING("step_warning"),
    STEP_ERROR("step_error");

    companion object {
        private val byName = entries.associateBy(Emitted::written)

        /** The event a case names. */
        fun named(written: String): Emitted =
            byName[written] ?: throw ModelException("a step cannot emit $written")
    }
}
