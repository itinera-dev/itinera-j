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

/**
 * What a rescue step requests of the step it rescues, written after [inputsBefore] of its inputs.
 */
internal data class RescuedStepRequest(val request: HookRequest, val inputsBefore: Int)

/** What a scenario scripts for one step, a rescue step included, filled in as it is written. */
internal class StepScript {
    val inputs = mutableListOf<Input>()
    val services = mutableListOf<RequestedService>()

    /**
     * What a rescue step requests of the step it rescues: its data, its failure reason or its
     * error, in the order written.
     */
    val ofRescuedStep = mutableListOf<RescuedStepRequest>()

    /** What the step does on every attempt before it ends, in the order written. */
    val actions = mutableListOf<StepAction>()

    /** What the step changes each input it received to, in place, by key. */
    val inputChanges = linkedMapOf<String, Any?>()

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

    /** Whether the step catches what its emit calls throw and carries on. */
    var ignoresEmitFailures = false

    /** The step policies attached to the step, in order, once stated. */
    var policies: List<String>? = null
        private set

    /** Whether the step keeps its contributor and step reporter after its attempt ends. */
    var keepsHandles = false

    /** Whether the step declares neither a contributor nor a step reporter. */
    var declaresNoHandles = false

    /** Attaches step policies, once. */
    fun attach(name: String, attached: List<String>) {
        if (policies != null) throw ModelException("the policies of \"$name\" are stated twice")
        policies = attached
    }

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
