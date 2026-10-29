package org.itineradev.itinera.workflow.checker

/**
 * Reads what a hook method of a policy class of [mode] requests from its parameters, recording each
 * mistake in [findings]: data marked `@FromStep` or `@FromWorkflow`, roles marked `@Role`, and the
 * rest by its type, each only where its hook's kind may request it.
 */
internal class HookRequests(private val mode: Mode, private val findings: Findings) {
    private val data = DataRequests(findings)

    /**
     * What [method], marked [hook], requests, or `null` when one of its parameters has a mistake.
     */
    fun of(hook: Hook, method: FunctionSymbol): List<Asked>? =
        method.parameters.map { parameter -> of(hook, method, parameter) }.let(::all)

    private fun of(hook: Hook, method: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val fromStep = parameter.annotation(Names.FROM_STEP)
        val fromWorkflow = parameter.annotation(Names.FROM_WORKFLOW)
        return when {
            fromStep != null ->
                if (hook.ofStep)
                    data.data(
                        method,
                        parameter,
                        fromStep.string("value"),
                        fromStep.boolean("required"),
                    )
                else data.refused(method, "requests data from the step, which only a step hook can")
            fromWorkflow != null ->
                data.data(
                    method,
                    parameter,
                    fromWorkflow.string("value"),
                    fromWorkflow.boolean("required"),
                )
            parameter.isMarked(Names.ROLE) -> role(method, parameter)
            parameter.isMarked(Names.INPUT) ->
                data.refused(
                    method,
                    "marks ${parameter.name} @Input, though a hook requests data with " +
                        "@FromStep or @FromWorkflow",
                )
            parameter.isMarked(Names.SERVICE) ->
                data.refused(
                    method,
                    "marks ${parameter.name} @Service, though a hook never requests a service",
                )
            else -> byType(hook, method, parameter)
        }
    }

    private fun role(method: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val type = parameter.erasedClass
        return when {
            type == null || !isRole(type) ->
                data.refused(
                    method,
                    "requests a role of ${parameter.jvmClass}, which is not an interface marked " +
                        "@Role",
                )
            method.owner.isKotlin && admitsNull(parameter.type) ->
                data.refused(
                    method,
                    "requests role ${type.simpleName} as a type that admits null, though a role " +
                        "is always given",
                )
            else -> RoleAsked(type)
        }
    }

    private fun byType(hook: Hook, method: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val type = parameter.jvmClass
        val given = givenByType[type]
        val failed = failedBy(parameter.type, method.owner.isKotlin)
        return when {
            given != null ->
                if (hook in given.hooks) Other
                else data.refused(method, "requests ${given.what}, which only ${given.who} can")
            failed != null ->
                if (hook.ofStep) Other
                else data.refused(method, "requests $failed, which only a step hook can")
            type == mode.hookReporter -> Other
            type == mode.other.hookReporter ->
                data.refused(method, mode.otherMode(mode.other.hookReporter, "hook"))
            else ->
                data.refused(
                    method,
                    "has a parameter that requests nothing, ${parameter.name}: mark data " +
                        "@FromStep or @FromWorkflow and a role @Role, and request the rest by " +
                        "its type",
                )
        }
    }
}

/** Whether [type] is a role: an interface marked `@Role`. */
internal fun isRole(type: ClassSymbol): Boolean = type.isInterface && type.isMarked(Names.ROLE)

/** A request a hook makes by its parameter's type, [what] it is, and the [hooks] that may. */
private class Given(val what: String, val hooks: Set<Hook>) {
    /** Which hooks may, when not every hook may: one hook, or every step hook. */
    val who: String
        get() = hooks.singleOrNull()?.hook ?: "a step hook"
}

private val stepHooks = Hook.entries.filter(Hook::ofStep).toSet()

private val givenByType: Map<String?, Given> =
    mapOf(
        Names.CONTRIBUTOR to Given("a contributor", Hook.entries.toSet()),
        Names.JOURNEY_ID to Given("the journey ID", Hook.entries.toSet()),
        Names.STEP_NAME to Given("the step's name", stepHooks),
        Names.ATTEMPT to Given("the attempt", stepHooks),
        Names.FAILURE_CAUSE to Given("why the step failed", setOf(Hook.ON_STEP_FAILURE)),
        Names.RETRY_CAUSE to Given("why the step is retried", setOf(Hook.ON_STEP_RETRY)),
    )
