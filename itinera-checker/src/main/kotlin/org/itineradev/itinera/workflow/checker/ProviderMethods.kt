package org.itineradev.itinera.workflow.checker

/**
 * A service provider of a workflow class, once read without a mistake: its name, the key it serves,
 * its steps and the class of the service it supplies.
 */
internal class ReadProvider(
    val name: String,
    val key: String,
    val steps: List<String>,
    val service: String,
)

/**
 * Reads the service providers of a workflow class in [mode], its [markedMethods] marked
 * `@Provider`, recording each mistake in [findings].
 */
internal class ProviderMethods(private val mode: Mode, private val findings: Findings) {
    private val requests = ProviderRequests(mode, findings)

    /** The providers [type] declares, leaving out those with a mistake. */
    fun of(type: ClassSymbol): List<ReadProvider> =
        markedMethods(type, Names.PROVIDER).mapNotNull(::provider)

    private fun provider(method: FunctionSymbol): ReadProvider? {
        val marked = method.annotation(Names.PROVIDER) ?: return null
        val name = marked.string("name").ifEmpty { method.name }
        val inMode = method.isSuspend == mode.suspends
        if (!inMode) findings.modeNotAccepted("service provider $name", mode, method.origin)
        val steps = marked.strings("steps")
        val stepsNamed = stepsNamed(findings, method, steps)
        val asks = requests.of(method)
        val returns = returns(findings, method, Names.PROVIDED)
        val service = serviceOf(method)
        val perAttempt = marked.boolean("perAttempt")
        val scoped = asks == null || perAttempt || inJourneyScope(name, asks, method)
        val usable = inMode && stepsNamed && returns && scoped
        return if (usable && asks != null && service != null)
            ReadProvider(name, marked.string("key").ifEmpty { method.name }, steps, service)
        else null
    }

    /** The class of the service [method] provides, the argument of its `Provided`. */
    private fun serviceOf(method: FunctionSymbol): String? {
        val service = method.returnArguments.singleOrNull()
        if (service == null && returnsSubtype(method, Names.PROVIDED))
            findings.misdeclared(
                method,
                "returns Provided of no class, which names no service type: " +
                    "write the service's type in Provided",
            )
        return service
    }

    /** Whether [asks] holds only what a provider of journey scope named [name] may request. */
    private fun inJourneyScope(name: String, asks: List<Asked>, method: FunctionSymbol): Boolean {
        val attempted = OfTheAttempt in asks
        if (attempted)
            findings +=
                Named(
                    "attempt request in journey scope",
                    "service provider $name requests the step's name or the attempt, which only " +
                        "a provider of attempt scope receives",
                    method.origin,
                )
        return !attempted
    }
}

/**
 * Reads what a service provider's method of [mode] requests from its parameters, as one of attempt
 * scope may, recording each mistake in [findings]: data marked `@FromWorkflow`, and the rest by its
 * type, the reporter of its mode among them.
 */
internal class ProviderRequests(private val mode: Mode, private val findings: Findings) {
    private val data = DataRequests(findings)

    /** What [method] requests, or `null` when one of its parameters has a mistake. */
    fun of(method: FunctionSymbol): List<Asked>? =
        method.parameters.map { parameter -> of(method, parameter) }.let(::all)

    private fun of(method: FunctionSymbol, parameter: ParameterSymbol): Asked? {
        val fromWorkflow = parameter.annotation(Names.FROM_WORKFLOW)
        val refused = providerRefusedMarks.find { mark -> parameter.isMarked(mark) }
        return when {
            fromWorkflow != null ->
                data.data(
                    method,
                    parameter,
                    fromWorkflow.string("value"),
                    fromWorkflow.boolean("required"),
                )
            refused != null ->
                data.refused(
                    method,
                    "marks ${parameter.name} @${simpleNameOf(refused)}, which a service " +
                        "provider never requests",
                )
            else -> byType(method, parameter)
        }
    }

    private fun byType(method: FunctionSymbol, parameter: ParameterSymbol): Asked? =
        when (parameter.jvmClass) {
            Names.JOURNEY_ID,
            mode.providerReporter -> Other
            Names.STEP_NAME,
            Names.ATTEMPT -> OfTheAttempt
            mode.other.providerReporter ->
                data.refused(method, mode.otherMode(mode.other.providerReporter, "provider"))
            Names.CONTRIBUTOR ->
                data.refused(
                    method,
                    "requests a contributor, which a service provider never receives",
                )
            else ->
                data.refused(
                    method,
                    "has a parameter that requests nothing, ${parameter.name}: mark data " +
                        "@FromWorkflow, and request the journey ID, the step's name, the attempt " +
                        "and the provider's reporter by their types",
                )
        }
}

private val providerRefusedMarks =
    listOf(Names.FROM_STEP, Names.ROLE, Names.INPUT, Names.SERVICE, Names.KEY)
