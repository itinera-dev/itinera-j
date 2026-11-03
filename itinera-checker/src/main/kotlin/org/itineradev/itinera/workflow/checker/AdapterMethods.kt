package org.itineradev.itinera.workflow.checker

/** An input adapter of a workflow class, once read without a mistake: its name and its steps. */
internal class ReadAdapter(val name: String, val steps: List<String>)

/**
 * Reads the input adapters of a workflow class in [mode], its [markedMethods] marked
 * `@InputAdapter`, recording each mistake in [findings].
 */
internal class AdapterMethods(private val mode: Mode, private val findings: Findings) {
    private val requests = AdapterRequests(findings)

    /** The adapters [type] declares, leaving out those with a mistake. */
    fun of(type: ClassSymbol): List<ReadAdapter> =
        markedMethods(type, Names.INPUT_ADAPTER).mapNotNull(::adapter)

    private fun adapter(method: FunctionSymbol): ReadAdapter? {
        val marked = method.annotation(Names.INPUT_ADAPTER) ?: return null
        val name = marked.string("name").ifEmpty { method.name }
        val inMode = method.isSuspend == mode.suspends
        if (!inMode) findings.modeNotAccepted("input adapter $name", mode, method.origin)
        val steps = marked.strings("steps")
        val stepsNamed = stepsNamed(findings, method, steps)
        val asks = requests.of(method)
        val returns = returns(findings, method, Names.SUPPLIED)
        val checked = inMode && returns && stepsNamed
        return if (checked && asks != null) ReadAdapter(name, steps) else null
    }
}

/** Whether none of [steps], which [method] names, is empty, recording in [findings] when one is. */
internal fun stepsNamed(findings: Findings, method: FunctionSymbol, steps: List<String>): Boolean {
    val empty = steps.any(String::isEmpty)
    if (empty) findings.misdeclared(method, "names a step with an empty name")
    return !empty
}

/**
 * Reads what an input adapter's method requests from its parameters, recording each mistake in
 * [findings]: data marked `@FromWorkflow`, the key marked `@Key`, and the rest by its type.
 */
internal class AdapterRequests(private val findings: Findings) {
    private val data = DataRequests(findings)

    /** What [method] requests, or `null` when one of its parameters has a mistake. */
    fun of(method: FunctionSymbol): List<Asked>? =
        method.parameters.map { parameter -> of(method, parameter) }.let(::all)

    private fun of(method: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val fromWorkflow = parameter.annotation(Names.FROM_WORKFLOW)
        val refused = adapterRefusedMarks.find { mark -> parameter.isMarked(mark) }
        return when {
            fromWorkflow != null ->
                data.data(
                    method,
                    parameter,
                    fromWorkflow.string("value"),
                    fromWorkflow.boolean("required"),
                )
            parameter.isMarked(Names.KEY) -> key(method, parameter)
            refused != null ->
                data.refused(
                    method,
                    "marks ${parameter.name} @${simpleNameOf(refused)}, which an input adapter " +
                        "never requests",
                )
            else -> byType(method, parameter)
        }
    }

    private fun key(method: FunctionSymbol, parameter: ParameterSymbol): Asked? =
        when {
            parameter.jvmClass != Names.STRING ->
                data.refused(method, "marks ${parameter.name} @Key, though the key is a String")
            method.owner.isKotlin && admitsNull(parameter.type) ->
                data.refused(method, "requests the key as String?, though the key is always given")
            else -> Other
        }

    private fun byType(method: FunctionSymbol, parameter: ParameterSymbol): Asked? =
        when (parameter.jvmClass) {
            Names.STEP_NAME,
            Names.JOURNEY_ID,
            Names.DATA_BAG_ACCESS -> Other
            Names.CONTRIBUTOR ->
                data.refused(
                    method,
                    "requests a contributor, which an input adapter never receives",
                )
            else ->
                data.refused(
                    method,
                    "has a parameter that requests nothing, ${parameter.name}: mark data " +
                        "@FromWorkflow and the key @Key, and request the step's name, the " +
                        "journey ID and the data bag's access by their types",
                )
        }
}

private val adapterRefusedMarks = listOf(Names.FROM_STEP, Names.ROLE, Names.INPUT, Names.SERVICE)
