package org.itineradev.itinera.conformance.model

/** How an attempt of a scripted step ends, as a case writes it. */
internal enum class Ending(val written: String) {
    SUCCESS("success"),
    FAILURE("failure"),
    SKIPPED("skipped"),
    ERROR("error");

    companion object {
        private val byName = entries.associateBy(Ending::written)

        /** The ending a case names. */
        fun named(written: String): Ending =
            byName[written] ?: throw ModelException("$written is not an outcome of the cases")
    }
}

/**
 * One row of a step's attempts: how the attempt ends, the reason's code, message and details or the
 * error's message, and what it contributes first, in the order written.
 */
internal data class ScriptedAttempt(
    val number: Int,
    val ending: Ending,
    val code: String? = null,
    val message: String? = null,
    val details: Any? = null,
    val contributes: Map<String, Any?> = emptyMap(),
)

/** A required or optional input a step requests, of a type of the cases. */
internal data class Input(val key: String, val type: ValueType, val optional: Boolean)

/** What a scenario scripts for one step, a rescue step included, filled in as it is written. */
internal class StepScript {
    val inputs = mutableListOf<Input>()
    val contributions = mutableListOf<Pair<String, Any?>>()

    /** Each attempt in order; attempts after the last repeat it. `null` until stated. */
    var attempts: List<ScriptedAttempt>? = null
        private set

    var retries: Int? = null
        private set

    var retriesAbnormalTerminations = true
        private set

    /** The message the step's build fails with, once stated. */
    var buildFailure: String? = null
        private set

    var hasRescueStep = false
    var isRescueStep = false
    var isAsynchronous = false

    /** Says how each attempt ends, once. */
    fun attempts(name: String, scripted: List<ScriptedAttempt>) {
        if (attempts != null)
            throw ModelException("how the attempts of \"$name\" end is stated twice")
        if (scripted.map(ScriptedAttempt::number) != (1..scripted.size).toList()) {
            throw ModelException("the attempts of \"$name\" are not numbered 1, 2, 3 and so on")
        }
        attempts = scripted
    }

    /** Gives the step its retry budget, once. */
    fun allows(name: String, budget: Int) {
        if (retries != null) throw ModelException("the retry budget of \"$name\" is stated twice")
        if (budget < 0) throw ModelException("\"$name\" cannot allow $budget retries")
        retries = budget
    }

    /** Says that building the step fails with [message], once. */
    fun cannotBeBuilt(name: String, message: String) {
        if (buildFailure != null)
            throw ModelException("how \"$name\" fails to build is stated twice")
        buildFailure = message
    }

    /** Turns `retry on abnormal termination` off. */
    fun doesNotRetryAbnormalTerminations() {
        retriesAbnormalTerminations = false
    }
}

/**
 * The workflow a scenario declares: its name, its steps in declaration order, and their scripts.
 */
internal class WorkflowModel(val name: String, val steps: List<String>) {
    /** The script of each step and rescue step, by name; two steps of one name share one. */
    val scripts: MutableMap<String, StepScript> =
        steps.associateWithTo(linkedMapOf()) { StepScript() }

    /** The reporters the workflow lists, in order, once stated. */
    var reporters: List<String>? = null

    /** Where journey IDs come from, once stated. */
    var idGenerator: IdGenerator? = null
}

/** Where a workflow instance's journey ID comes from. */
internal sealed interface IdGenerator {
    /** The default: a UUID v4 for each instance. */
    data object None : IdGenerator

    /** A generator that returns this ID. */
    data class Returns(val id: String) : IdGenerator
}
