package org.itineradev.itinera.workflow.checker

/**
 * Reads what a step class or a rescue step class of [mode] requests from its parameters, recording
 * each mistake in [findings]: inputs from its constructor's, with what a rescue step requests from
 * the step it rescues, and services and handles from its run's.
 */
internal class StepRequests(private val mode: Mode, private val findings: Findings) {
    private val data = DataRequests(findings)

    /** The inputs [constructor] requests, or `null` when one of them has a mistake. */
    fun inputs(constructor: FunctionSymbol): List<Asked>? =
        parametersOf(constructor)?.map { parameter -> input(constructor, parameter) }?.let(::all)

    /**
     * The inputs and the data from the step it rescues that a rescue step's [constructor] requests,
     * or `null` when one of them has a mistake.
     */
    fun ofRescue(constructor: FunctionSymbol): List<Asked>? =
        parametersOf(constructor)?.map { parameter -> ofRescue(constructor, parameter) }?.let(::all)

    /** The services and handles [run] requests, or `null` when one of them has a mistake. */
    fun ofRun(run: FunctionSymbol): List<Asked>? =
        parametersOf(run)?.map { parameter -> ofRun(run, parameter) }?.let(::all)

    private fun parametersOf(declaration: FunctionSymbol): List<ParameterSymbol>? {
        if (declaration.capturesEnclosing)
            findings.misdeclared(
                declaration,
                "captures what its enclosing code holds, which itinera cannot give: declare the " +
                    "step at the top level, or as a static nested class",
            )
        return declaration.parameters.takeUnless { declaration.capturesEnclosing }
    }

    private fun input(constructor: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val input =
            parameter.annotation(Names.INPUT)
                ?: return data.refused(
                    constructor,
                    "has a parameter that is not an input, ${parameter.name}: a step's " +
                        "constructor takes only inputs, marked @Input",
                )
        return data.data(constructor, parameter, input.string("value"), input.boolean("required"))
    }

    private fun ofRescue(constructor: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val input = parameter.annotation(Names.INPUT)
        val rescued = parameter.annotation(Names.FROM_RESCUED_STEP)
        return when {
            input != null ->
                data.data(constructor, parameter, input.string("value"), input.boolean("required"))
            rescued != null ->
                data.data(
                    constructor,
                    parameter,
                    rescued.string("value"),
                    rescued.boolean("required"),
                )
            failedBy(parameter.type, constructor.owner.isKotlin) != null -> Other
            else ->
                data.refused(
                    constructor,
                    "has a parameter that requests nothing, ${parameter.name}: a rescue " +
                        "step's constructor takes inputs, marked @Input, data from the step it " +
                        "rescues, marked @FromRescuedStep, and the failure's Reason and error by " +
                        "their types",
                )
        }
    }

    private fun ofRun(run: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val type = parameter.jvmClass
        val service = parameter.annotation(Names.SERVICE)
        return when {
            service != null -> service(run, parameter, service.string("value"))
            parameter.isMarked(Names.INPUT) ->
                data.refused(
                    run,
                    "requests an input in run, though a step's inputs are its constructor's",
                )
            type == Names.CONTRIBUTOR || type == mode.stepReporter -> Other
            type == mode.other.stepReporter ->
                data.refused(run, mode.otherMode(mode.other.stepReporter, "step"))
            else ->
                data.refused(
                    run,
                    "has a parameter that requests nothing, ${parameter.name}: mark a " +
                        "service @Service, and request a handle by its type",
                )
        }
    }

    private fun service(run: FunctionSymbol, parameter: ParameterSymbol, written: String): Asked? {
        val key = written.ifEmpty { parameter.name }
        return if (run.owner.isKotlin && admitsNull(parameter.type))
            data.refused(
                run,
                "requests service $key as a type that admits null, though a service is always " +
                    "required",
            )
        else ServiceAsked(key, boxed(parameter.jvmClass))
    }
}
