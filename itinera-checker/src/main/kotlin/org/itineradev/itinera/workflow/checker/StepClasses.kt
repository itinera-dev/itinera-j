package org.itineradev.itinera.workflow.checker

/** What a step class or a rescue step class declares, once read without a mistake. */
internal class ReadStep(
    /** What its constructor and its `run` request, in order. */
    val asks: List<Asked>
)

/**
 * Reads step classes and rescue step classes of a workflow class in [mode], recording each mistake
 * in [findings].
 */
internal class StepClasses(private val mode: Mode, private val findings: Findings) {
    private val requests = StepRequests(mode, findings)

    /** The step class [step] named [name], or `null` when the class has a mistake. */
    fun step(name: String, step: ClassSymbol): ReadStep? =
        read(step, Names.STEP, "a step", "step $name", Names.OUTCOME, requests::inputs)

    /** The rescue step class [rescue] of the step [step], or `null` when it has a mistake. */
    fun rescue(step: String, rescue: ClassSymbol): ReadStep? =
        read(
            rescue,
            Names.RESCUE_STEP,
            "a rescue step",
            "rescue step ${step}_rescue",
            Names.RESCUE_OUTCOME,
            requests::ofRescue,
        )

    private fun read(
        part: ClassSymbol,
        mark: String,
        what: String,
        named: String,
        returns: String,
        built: (FunctionSymbol) -> List<Asked>?,
    ): ReadStep? {
        val buildable = marked(part, mark) && buildable(part)
        val constructor = if (buildable) constructorOf(part, what) else null
        val run =
            if (buildable) runOf(part)?.takeIf { run -> checked(named, run, returns) } else null
        val asked = constructor?.let(built)
        val asks = run?.let(requests::ofRun)
        return if (asked != null && asks != null) ReadStep(asked + asks) else null
    }

    private fun marked(part: ClassSymbol, mark: String): Boolean {
        val marked = part.isMarked(mark)
        if (!marked) findings.misdeclared(part, "is not marked @${simpleNameOf(mark)}")
        return marked
    }

    private fun buildable(part: ClassSymbol): Boolean {
        if (part.isAbstract) findings.misdeclared(part, "cannot be built: it is abstract")
        return !part.isAbstract
    }

    private fun constructorOf(part: ClassSymbol, what: String): FunctionSymbol? {
        val constructors = part.constructors
        if (constructors.size != 1)
            findings.misdeclared(part, "has several constructors, and $what has one")
        return constructors.singleOrNull()
    }

    private fun runOf(part: ClassSymbol): FunctionSymbol? {
        val runs = part.publicMethods.filter(::isRun)
        if (runs.size != 1) {
            val count = if (runs.isEmpty()) "no public method" else "several public methods"
            findings.misdeclared(part, "has $count named run")
        }
        return runs.singleOrNull()
    }

    private fun checked(named: String, run: FunctionSymbol, returns: String): Boolean {
        val inMode = run.isSuspend == mode.suspends
        if (!inMode) findings.modeNotAccepted(named, mode, run.origin)
        val outcome = returns == Names.RESCUE_OUTCOME && returnsSubtype(run, Names.OUTCOME)
        if (outcome)
            findings.misdeclared(
                run,
                "returns Outcome, a step's outcome, though a rescue step never skips: return " +
                    "RescueOutcome",
            )
        return inMode && !outcome && returns(findings, run, returns)
    }
}

private fun isRun(method: FunctionSymbol): Boolean = method.jvmName == "run"
