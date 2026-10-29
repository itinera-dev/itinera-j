package org.itineradev.itinera.workflow.checker

/** A step a workflow class declares, once read without a mistake. */
internal class DeclaredStep(
    val name: String,
    val retryBudget: Int,
    val asks: List<Asked>,
    val policies: List<ReadPolicy>,
    /** What its rescue step requests, when it has one. */
    val rescue: List<Asked>?,
)

/** A workflow class, once read without a mistake. */
internal class ReadWorkflow(
    val steps: List<DeclaredStep>,
    val policies: List<ReadPolicy>,
    val roles: List<ClassSymbol>,
    val adapters: List<ReadAdapter>,
    val providers: List<ReadProvider>,
    val origin: Any,
)

/**
 * Reads the workflow the class [type] declares in [mode], finding every mistake the run-time reader
 * finds: those of its declaration first, and only when there are none, those of how it is put
 * together.
 */
internal class Reading(private val type: ClassSymbol, private val mode: Mode) {
    private val findings = Findings()
    private val steps = StepClasses(mode, findings)
    private val policies = PolicyClasses(mode, findings)

    /** Every mistake the class has. */
    fun mistakes(): List<Finding> {
        val workflow = type.annotation(Names.WORKFLOW)
        val name = workflow?.string("name") ?: type.simpleName
        if (name.isEmpty()) findings.misdeclared(type, "names its workflow with an empty name")
        val declared = type.annotations(Names.WITH_STEP).map(::step)
        val attached = workflow?.types("policies").orEmpty().map(policies::workflowPolicy)
        workflow?.types("reporters").orEmpty().forEach(::reporter)
        val adapted = AdapterMethods(mode, findings).of(type)
        val provided = ProviderMethods(mode, findings).of(type)
        IdGenerators(findings).read(type)
        findings += duplicateStepNames(declared.mapNotNull(Declared::name), type.origin)
        if (findings.found) return findings.all
        val read =
            ReadWorkflow(
                declared.mapNotNull(Declared::step),
                attached.filterNotNull(),
                rolesOf(type),
                adapted,
                provided,
                type.origin,
            )
        return assemblyMistakes(read)
    }

    private fun reporter(listed: ClassSymbol) {
        if (listed.isSubtypeOf(mode.reporter)) builtWithNothing(findings, listed, "reporter")
        else findings.modeNotAccepted("reporter ${listed.name}", mode, type.origin)
    }

    private fun step(declared: AnnotationSymbol): Declared {
        val written = declared.string("name")
        val name = written.takeUnless(String::isEmpty)
        if (name == null) findings.misdeclared(type, "names a step with an empty name")
        val budget = declared.int("retryBudget")
        val budgeted = budget >= 0
        if (!budgeted) findings.misdeclared(type, "gives step $written a retry budget below 0")
        val step = name?.let { stepName -> steps.step(stepName, declared.type("step")) }
        val attached = all(declared.types("policies").map(policies::stepPolicy))
        val rescue = name?.let { stepName -> rescue(stepName, declared.type("rescue")) }
        val read = step != null && attached != null && rescue != null
        val described =
            if (name != null && budgeted && read)
                DeclaredStep(name, budget, step.asks, attached, rescue.asks)
            else null
        return Declared(name, described)
    }

    /** The rescue step [rescue] gives the step [name]: none, or `null` when it has a mistake. */
    private fun rescue(name: String, rescue: ClassSymbol): DeclaredRescue? =
        if (rescue.name == Names.ITINERA_RESCUE_STEP) DeclaredRescue(null)
        else steps.rescue(name, rescue)?.let { read -> DeclaredRescue(read.asks) }
}

/** The rescue step a workflow class's `@WithStep` gives its step: what it requests, if any. */
private class DeclaredRescue(val asks: List<Asked>?)

/** A step a workflow class declares: its name and what was read of it, when each could be. */
private class Declared(val name: String?, val step: DeclaredStep?)

/** The roles [type] provides: every interface marked `@Role` that it or a superclass implements. */
private fun rolesOf(type: ClassSymbol): List<ClassSymbol> =
    generateSequence(type, ClassSymbol::superclass)
        .toList()
        .flatMap(::implemented)
        .filter(::isRole)
        .distinctBy(ClassSymbol::name)

private fun implemented(type: ClassSymbol): List<ClassSymbol> =
    type.supertypes
        .map(ClassUse::raw)
        .filter(ClassSymbol::isInterface)
        .flatMap(::withSuperinterfaces)

private fun withSuperinterfaces(type: ClassSymbol): List<ClassSymbol> =
    listOf(type) + implemented(type)
